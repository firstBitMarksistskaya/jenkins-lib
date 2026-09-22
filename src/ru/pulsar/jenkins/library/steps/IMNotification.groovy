package ru.pulsar.jenkins.library.steps

import hudson.model.Result
import jenkins.plugins.http_request.HttpMode
import jenkins.plugins.http_request.MimeType
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.notification.IMNotificationOptions
import ru.pulsar.jenkins.library.configuration.notification.im.Messenger
import ru.pulsar.jenkins.library.configuration.notification.im.NotificationMessageBuilder
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.RepoUtils

class IMNotification implements Serializable {

    private final JobConfiguration config
    private final Messenger messenger

    IMNotification(JobConfiguration config, Messenger messenger) {
        this.config = config
        this.messenger = messenger
    }

    def run() {

        Logger.printLocation()

        if (!messenger.isEnabled(config)) {
            Logger.println("${messenger.name()} notifications are disabled")
            return
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def options = messenger.getOptions(config)

        def currentBuild = steps.currentBuild()
        def currentResult = Result.fromString(currentBuild.getCurrentResult())

        String message = NotificationMessageBuilder.getMessage(currentBuild, messenger.getFlavor())

        if (options.onAlways) {
            sendMessage(message)
        } else if (options.onFailure && (currentResult == Result.FAILURE || currentResult == Result.ABORTED)) {
            sendMessage(message)
        } else if (options.onUnstable && currentResult == Result.UNSTABLE) {
            sendMessage(message)
        } else if (options.onSuccess && currentResult == Result.SUCCESS) {
            sendMessage(message)
        } else {
            Logger.println("Unknown build result! Can't send a message to ${messenger.name()}")
        }
    }

    private void sendMessage(String fullMessage) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env()

        String repoSlug = RepoUtils.getRepoSlug()

        String botTokenCred = messenger.getBotTokenCredentialId(config, repoSlug)
        String chatIdCred = messenger.getChatIdCredentialId(config, repoSlug)

        def bindings = []
        if (botTokenCred != null) {
            bindings << steps.string(botTokenCred, 'TOKEN')
        }
        if (chatIdCred != null) {
            bindings << steps.string(chatIdCred, 'CHAT_ID')
        }

        steps.withCredentials(bindings) {

            String token = botTokenCred != null ? (env.TOKEN as String) : null
            String chatId = chatIdCred != null ? (env.CHAT_ID as String) : null

            String messageToSend = truncate(fullMessage, messenger.getMaxMessageLength())
            String url = messenger.buildUrl(token, chatId)
            String bodyString = messenger.buildBody(chatId, messageToSend)
            List<Map<String, String>> customHeaders = messenger.buildHeaders(token)

            steps.echo("Sending ${messenger.name()} notification:")
            steps.echo(fullMessage)
            steps.echo(bodyString)

            if (customHeaders == null || customHeaders.isEmpty()) {
                steps.httpRequest(
                    url,
                    HttpMode.POST,
                    MimeType.APPLICATION_JSON_UTF8,
                    bodyString,
                    '200:299',
                    true
                )
            } else {
                steps.httpRequest(
                    url,
                    HttpMode.POST,
                    MimeType.APPLICATION_JSON_UTF8,
                    bodyString,
                    '200:299',
                    true,
                    customHeaders
                )
            }
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text
        }
        return text.substring(0, maxLength - 3) + '...'
    }
}
