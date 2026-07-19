package ru.pulsar.jenkins.library.steps

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.databind.ObjectMapper
import hudson.model.Result
import hudson.scm.ChangeLogSet
import io.jenkins.blueocean.rest.impl.pipeline.FlowNodeWrapper
import io.jenkins.blueocean.rest.impl.pipeline.PipelineNodeGraphVisitor
import io.jenkins.blueocean.rest.model.BlueRun
import org.apache.commons.lang3.time.DurationFormatUtils
import jenkins.plugins.http_request.HttpMode
import jenkins.plugins.http_request.MimeType
import org.jenkinsci.plugins.workflow.actions.TimingAction
import org.jenkinsci.plugins.workflow.graph.BlockStartNode
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.Secrets
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.RepoUtils
import ru.pulsar.jenkins.library.utils.StringJoiner

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import static ru.pulsar.jenkins.library.configuration.Secrets.UNKNOWN_ID

class MaxNotification implements Serializable {

    private static final String MAX_API_URL = "https://platform-api2.max.ru/messages"

    private final JobConfiguration config;

    MaxNotification(JobConfiguration config) {
        this.config = config
    }

    def run() {

        Logger.printLocation()

        if (!config.stageFlags.max) {
            Logger.println("MAX notifications are disabled")
            return
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def options = config.notificationsOptions.maxNotificationOptions

        def currentBuild = steps.currentBuild()
        def currentResult = Result.fromString(currentBuild.getCurrentResult())

        String message = getMessage(currentBuild)

        if (options.onAlways) {
            sendMessage(message)
        } else if (options.onFailure && (currentResult == Result.FAILURE || currentResult == Result.ABORTED)) {
            sendMessage(message)
        } else if (options.onUnstable && currentResult == Result.UNSTABLE) {
            sendMessage(message)
        } else if (options.onSuccess && currentResult == Result.SUCCESS) {
            sendMessage(message)
        } else {
            Logger.println("Unknown build result! Can't send a message to MAX")
        }

    }

    private void sendMessage(message) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env();

        String repoSlug = RepoUtils.getRepoSlug()

        Secrets secrets = config.secrets

        String maxChatIdCredentials = secrets.maxChatId == UNKNOWN_ID ? repoSlug + "_MAX_CHAT_ID" : secrets.maxChatId
        String maxBotTokenCredentials = secrets.maxBotToken == UNKNOWN_ID ? "MAX_BOT_TOKEN" : secrets.maxBotToken

        steps.withCredentials([
            steps.string(maxBotTokenCredentials, 'TOKEN'),
            steps.string(maxChatIdCredentials, 'CHAT_ID')
        ]) {

            def mapper = new ObjectMapper()

            def body = [
                text                : message,
                format              : 'markdown',
                disable_link_preview: true
            ]

            def bodyString = mapper.writeValueAsString(body)
            String chatId = URLEncoder.encode(env.CHAT_ID as String, StandardCharsets.UTF_8.name())
            String url = "${MAX_API_URL}?chat_id=${chatId}"

            steps.echo(message)
            steps.echo(bodyString)

            steps.httpRequest(
                url,
                HttpMode.POST,
                MimeType.APPLICATION_JSON_UTF8,
                bodyString,
                '200',
                true,
                [[name: 'Authorization', value: env.TOKEN as String]]
            )
        }
    }

    private static String getMessage(RunWrapper currentBuild) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env();

        def currentResult = Result.fromString(currentBuild.getCurrentResult())

        def messageJoiner = new StringJoiner('\n\n')

        def displayName = escapeStringForMarkdown(currentBuild.fullDisplayName)
        String header = "[$displayName]($env.BUILD_URL)"
        messageJoiner.add(header)

        String result = ""
        if (currentResult == Result.SUCCESS) {
            result = "✅ Сборка прошла успешно!"
        } else if (currentResult == Result.FAILURE) {
            result = "❌ Сборка завершилась с ошибкой!"
        } else if (currentResult == Result.ABORTED) {
            result = "🛑 Сборка прервана!"
        } else if (currentResult == Result.UNSTABLE) {
            result = "💩 Есть упавшие тесты!"
        }

        result = escapeStringForMarkdown(result)
        messageJoiner.add(result)

        String stageResults = getStageResultsMessage(currentBuild)
        if (stageResults.length() > 0) {
            stageResults = escapeStringForMarkdown(stageResults)
            messageJoiner.add(stageResults)
        }

        def duration = "Длительность сборки: ${currentBuild.getDurationString()}".replace(" and counting", "")
        duration = escapeStringForMarkdown(duration)
        messageJoiner.add(duration)

        def changeSet = getChangeSet(currentBuild)
        steps.echo(changeSet)
        if (changeSet.length() > 0) {
            changeSet = 'Изменения с последней сборки:\n\n' + changeSet
            messageJoiner.add(changeSet)
        }

        String buildUrl = "[Лог сборки](${env.BUILD_URL}console)"
        messageJoiner.add(buildUrl)

        steps.echo(messageJoiner.toString())

        return messageJoiner.toString()
    }

    @NonCPS
    private static String getChangeSet(RunWrapper currentBuild) {
        String changeSetText = ''

        int counter = 0
        currentBuild.changeSets.each { changeSet ->
            changeSetText += "Набор изменений #${++counter}:\n"
            changeSet.items.each { ChangeLogSet.Entry entry ->
                String commit = ''
                def commitId = entry.commitId;
                if (commitId != null) {
                    if (isValidSHA1(commitId)) {
                        commitId = commitId.substring(0, 7)
                    }

                    def link = changeSet.browser?.getChangeSetLink(entry)
                    if (link != null) {
                        commit = "[$commitId]($link)"
                    } else {
                        commit = commitId
                    }
                }

                def author = escapeStringForMarkdown(entry.author.displayName)
                def authorLink = entry.author.absoluteUrl

                def message = escapeStringForMarkdown(entry.getMsgAnnotated())
                changeSetText += "* $commit $message ([$author]($authorLink))\n"
            }
            changeSetText += '\n'
        }
        return changeSetText.trim()
    }

    @NonCPS
    private static String getStageResultsMessage(RunWrapper currentBuild) {
        def visitor = new PipelineNodeGraphVisitor(currentBuild.rawBuild as WorkflowRun)
        def stages = visitor.pipelineNodes.findAll { it.type != FlowNodeWrapper.NodeType.STEP }

        def stageResultMessage = ""
        for (FlowNodeWrapper stage in stages) {
            if (stage.status.result == BlueRun.BlueRunResult.SUCCESS || stage.status.result == BlueRun.BlueRunResult.NOT_BUILT) {
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
    private static String escapeStringForMarkdown(String incoming) {
        return incoming.replace('\\', '\\\\')
            .replace('_', '\\_')
            .replace('*', '\\*')
            .replace('`', '\\`')
            .replace('[', '\\[')
            .replace(']', '\\]')
            .replace('(', '\\(')
            .replace(')', '\\)')
    }

    @NonCPS
    private static boolean isValidSHA1(String s) {
        return s.matches('^[a-fA-F0-9]{40}$');
    }
}
