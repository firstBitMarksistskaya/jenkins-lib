package ru.pulsar.jenkins.library.steps

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.databind.ObjectMapper
import hudson.model.Result
import hudson.scm.ChangeLogSet
import io.jenkins.blueocean.rest.impl.pipeline.FlowNodeWrapper
import io.jenkins.blueocean.rest.impl.pipeline.PipelineNodeGraphVisitor
import io.jenkins.blueocean.rest.model.BlueRun
import io.jenkins.cli.shaded.org.apache.commons.lang.time.DurationFormatUtils
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

import static ru.pulsar.jenkins.library.configuration.Secrets.UNKNOWN_ID

class TelegramNotification implements Serializable {

    private final JobConfiguration config;

    TelegramNotification(JobConfiguration config) {
        this.config = config
    }

    def run() {

        Logger.printLocation()

        if (!config.stageFlags.telegram) {
            Logger.println("Telegram notifications are disabled")
            return
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def options = config.notificationsOptions.telegramNotificationOptions

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
            Logger.println("Unknown build result! Can't send a message to telegram")
        }

    }

    private void sendMessage(message) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env();

        String repoSlug = RepoUtils.getRepoSlug()

        Secrets secrets = config.secrets

        String telegramChatIdCredentials = secrets.telegramChatId == UNKNOWN_ID ? repoSlug + "_TELEGRAM_CHAT_ID" : secrets.telegramChatId
        String telegramBotTokenCredentials = secrets.telegramBotToken == UNKNOWN_ID ? "TELEGRAM_BOT_TOKEN" : secrets.telegramBotToken

        steps.withCredentials([
            steps.string(telegramBotTokenCredentials, 'TOKEN'),
            steps.string(telegramChatIdCredentials, 'CHAT_ID')
        ]) {

            def mapper = new ObjectMapper()

            def body = [
                chat_id                 : env.CHAT_ID,
                text                    : message,
                disable_web_page_preview: true,
                parse_mode              : 'MarkdownV2'
            ]

            def bodyString = mapper.writeValueAsString(body)
            String url = "https://api.telegram.org/bot${env.TOKEN}/sendMessage"

            steps.echo(message)
            steps.echo(bodyString)

            steps.httpRequest(
                url,
                HttpMode.POST,
                MimeType.APPLICATION_JSON_UTF8,
                bodyString,
                '200',
                true
            )
        }
    }

    private static String getMessage(RunWrapper currentBuild) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env();

        def currentResult = Result.fromString(currentBuild.getCurrentResult())

        def messageJoiner = new StringJoiner('\n\n')

        def displayName = escapeStringForMarkdownV2(currentBuild.fullDisplayName)
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

        result = escapeStringForMarkdownV2(result)
        messageJoiner.add(result)

        String stageResults = getStageResultsMessage(currentBuild)
        if (stageResults.length() > 0) {
            stageResults = escapeStringForMarkdownV2(stageResults)
            messageJoiner.add(stageResults)
        }

        def duration = "Длительность сборки: ${currentBuild.getDurationString()}".replace(" and counting", "")
        duration = escapeStringForMarkdownV2(duration)
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
            changeSetText += "Набор изменений \\#${++counter}:\n"
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

                def author = escapeStringForMarkdownV2(entry.author.displayName)
                def authorLink = entry.author.absoluteUrl

                def message = escapeStringForMarkdownV2(entry.getMsgAnnotated())
                changeSetText += "\\* $commit $message \\([$author]($authorLink)\\)\n"
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
    private static String escapeStringForMarkdownV2(String incoming) {
        return incoming.replace('_', '\\_')
            .replace('*', '\\*')
            .replace('[', '\\[')
            .replace(']', '\\]')
            .replace('(', '\\(')
            .replace(')', '\\)')
            .replace('~', '\\~')
            .replace('`', '\\`')
            .replace('>', '\\>')
            .replace('#', '\\#')
            .replace('+', '\\+')
            .replace('-', '\\-')
            .replace('=', '\\=')
            .replace('|', '\\|')
            .replace('{', '\\{')
            .replace('}', '\\}')
            .replace('.', '\\.')
            .replace('!', '\\!')
    }

    @NonCPS
    private static boolean isValidSHA1(String s) {
        return s.matches('^[a-fA-F0-9]{40}$');
    }
}
