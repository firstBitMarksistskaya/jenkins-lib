package ru.pulsar.jenkins.library.configuration.notification.im

import com.cloudbees.groovy.cps.NonCPS
import hudson.model.Result
import hudson.scm.ChangeLogSet
import io.jenkins.blueocean.rest.impl.pipeline.FlowNodeWrapper
import io.jenkins.blueocean.rest.impl.pipeline.PipelineNodeGraphVisitor
import org.apache.commons.lang3.time.DurationFormatUtils
import org.jenkinsci.plugins.workflow.actions.TimingAction
import org.jenkinsci.plugins.workflow.graph.BlockStartNode
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.StringJoiner

class NotificationMessageBuilder implements Serializable {

    static String getMessage(RunWrapper currentBuild, MarkdownFlavor flavor) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env()

        def currentResult = Result.fromString(currentBuild.getCurrentResult())

        def messageJoiner = new StringJoiner('\n\n')

        String header = flavor.link(currentBuild.fullDisplayName, env.BUILD_URL as String)
        messageJoiner.add(header)

        String result = ''
        if (currentResult == Result.SUCCESS) {
            result = "✅ Сборка прошла успешно!"
        } else if (currentResult == Result.FAILURE) {
            result = "❌ Сборка завершилась с ошибкой!"
        } else if (currentResult == Result.ABORTED) {
            result = "🛑 Сборка прервана!"
        } else if (currentResult == Result.UNSTABLE) {
            result = "💩 Есть упавшие тесты!"
        }
        result = flavor.escape(result)
        messageJoiner.add(result)

        String stageResults = getStageResultsMessage(currentBuild)
        if (stageResults.length() > 0) {
            stageResults = flavor.escape(stageResults)
            messageJoiner.add(stageResults)
        }

        def duration = "Длительность сборки: ${currentBuild.getDurationString()}".replace(" and counting", "")
        duration = flavor.escape(duration)
        messageJoiner.add(duration)

        def changeSet = getChangeSet(currentBuild, flavor)
        if (changeSet.length() > 0) {
            changeSet = 'Изменения с последней сборки:\n\n' + changeSet
            messageJoiner.add(changeSet)
        }

        String buildUrl = flavor.link('Лог сборки', "${env.BUILD_URL}console")
        messageJoiner.add(buildUrl)

        return messageJoiner.toString()
    }

    @NonCPS
    static String getChangeSet(RunWrapper currentBuild, MarkdownFlavor flavor) {
        String changeSetText = ''

        int counter = 0
        currentBuild.changeSets.each { changeSet ->
            changeSetText += "Набор изменений ${flavor.hash()}${++counter}:\n"
            changeSet.items.each { ChangeLogSet.Entry entry ->
                String commit = ''
                def commitId = entry.commitId
                if (commitId != null) {
                    if (isValidSHA1(commitId)) {
                        commitId = commitId.substring(0, 7)
                    }

                    def link = changeSet.browser?.getChangeSetLink(entry)
                    if (link != null) {
                        commit = flavor.link(commitId as String, link as String)
                    } else {
                        commit = commitId
                    }
                }

                def authorRef = flavor.link(entry.author.displayName, entry.author.absoluteUrl)
                def message = flavor.escape(entry.getMsg())
                changeSetText += "${flavor.bullet()} $commit $message ${flavor.openParen()}${authorRef}${flavor.closeParen()}\n"
            }
            changeSetText += '\n'
        }
        return changeSetText.trim()
    }

    @NonCPS
    static String getStageResultsMessage(RunWrapper currentBuild) {
        def visitor = new PipelineNodeGraphVisitor(currentBuild.rawBuild as WorkflowRun)
        def stages = visitor.pipelineNodes.findAll { it.type != FlowNodeWrapper.NodeType.STEP }

        def parallelParentIds = [] as Set
        for (FlowNodeWrapper node in stages) {
            if (node.type == FlowNodeWrapper.NodeType.PARALLEL && node.firstParent != null) {
                parallelParentIds.add(node.firstParent.id)
            }
        }

        def stageResultMessage = ""
        for (FlowNodeWrapper stage in stages) {
            def parent = stage.firstParent
            if (!shouldReportStage(
                stage.type?.name(),
                stage.status?.result?.name(),
                isContainerStage(stage, parallelParentIds),
                parent?.type?.name(),
                parent?.status?.result?.name(),
                parent != null && isContainerStage(parent, parallelParentIds)
            )) {
                continue
            }

            long duration
            def endNode = stage.node.getExecution().getEndNode(stage.node as BlockStartNode)
            if (endNode != null) {
                def startTime = TimingAction.getStartTime(stage.node)
                def endTime = TimingAction.getStartTime(endNode)
                duration = endTime - startTime
            } else {
                duration = stage.timing.totalDurationMillis
            }

            def time = DurationFormatUtils.formatDuration(duration, "H:mm:ss")
            stageResultMessage += "$stage.displayName: $stage.status.result, затрачено времени $time  \n"
        }

        return stageResultMessage.trim()
    }

    @NonCPS
    public static boolean shouldReportStage(
        String type,
        String result,
        boolean isContainer,
        String parentType,
        String parentResult,
        boolean parentIsContainer
    ) {
        if (type == null || type == 'STEP' || type == 'PARALLEL') {
            return false
        }
        if (result == null || result == 'SUCCESS' || result == 'NOT_BUILT') {
            return false
        }
        if (isContainer) {
            return false
        }
        if (parentType == null || parentType == 'STEP') {
            return true
        }
        if (parentIsContainer) {
            return true
        }
        return parentResult == null || parentResult == 'SUCCESS' || parentResult == 'NOT_BUILT'
    }

    @NonCPS
    private static boolean isContainerStage(FlowNodeWrapper node, Set parallelParentIds) {
        if (node == null) {
            return false
        }
        return node.type == FlowNodeWrapper.NodeType.PARALLEL || parallelParentIds.contains(node.id)
    }

    @NonCPS
    static boolean isValidSHA1(String s) {
        return s.matches('^[a-fA-F0-9]{40}$')
    }
}
