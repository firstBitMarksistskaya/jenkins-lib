package ru.pulsar.jenkins.library.steps

import com.cloudbees.groovy.cps.NonCPS
import hudson.model.Result
import jenkins.plugins.http_request.HttpMode
import jenkins.plugins.http_request.MimeType
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
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
        def options = messenger.getOptions(config)

        String botTokenCred = messenger.getBotTokenCredentialId(config, repoSlug)
        String chatIdCred = messenger.getChatIdCredentialId(config, repoSlug, env.BRANCH_NAME as String)
        String proxyCred = messenger.getHttpProxyCredentialId(options)

        def bindings = []
        if (botTokenCred != null) {
            bindings << steps.string(botTokenCred, 'TOKEN')
        }
        if (chatIdCred != null) {
            bindings << steps.string(chatIdCred, 'CHAT_ID')
        }
        if (proxyCred != null) {
            bindings << steps.string(proxyCred, 'HTTP_PROXY')
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

            String httpProxy = proxyCred == null ? null : env.HTTP_PROXY as String
            String proxyAuthentication = messenger.getProxyAuthenticationCredentialId(options)

            if (customHeaders == null || customHeaders.isEmpty()) {
                steps.httpRequest(
                    url,
                    HttpMode.POST,
                    MimeType.APPLICATION_JSON_UTF8,
                    bodyString,
                    '200:299',
                    true,
                    httpProxy,
                    proxyAuthentication
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

    @NonCPS
    static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text
        }
        String suffix = '\n\u2026'
        int budget = maxLength - suffix.length()
        if (budget < 0) {
            return text.substring(0, safeEnd(text, maxLength))
        }
        int newline = text.lastIndexOf('\n', budget)
        while (newline > 0 && text.startsWith('\n', newline - 1)) {
            newline--
        }
        if (newline > 0) {
            return text.substring(0, newline) + suffix
        }
        String prefix = text.substring(0, budget)
        int cut = safeEnd(prefix, cutBeforeUnclosedLink(prefix))
        String kept = prefix.substring(0, cut).stripTrailing()
        return kept.isEmpty() ? '\u2026' : kept + suffix
    }

    @NonCPS
    private static int cutBeforeUnclosedLink(String prefix) {
        int open = lastUnescaped(prefix, '[')
        boolean unclosed = open >= 0 && lastUnescaped(prefix, ')') < open
        return unclosed ? open : prefix.length()
    }

    @NonCPS
    private static int lastUnescaped(String text, String symbol) {
        int index = text.lastIndexOf(symbol)
        while (index >= 0 && isEscaped(text, index)) {
            index = text.lastIndexOf(symbol, index - 1)
        }
        return index
    }

    @NonCPS
    private static boolean isEscaped(String text, int index) {
        int start = index
        while (start > 0 && text.startsWith('\\', start - 1)) {
            start--
        }
        return (index - start) % 2 == 1
    }

    @NonCPS
    private static int safeEnd(String text, int end) {
        return end > 0 && Character.isHighSurrogate(text.charAt(end - 1)) ? end - 1 : end
    }
}
