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
        if (maxLength < suffix.length()) {
            return text.substring(0, maxLength)
        }
        int budget = maxLength - suffix.length()
        int newline = text.lastIndexOf('\n', budget)
        if (newline > 0) {
            return text.substring(0, newline) + suffix
        }
        String prefix = text.substring(0, budget)
        int cut = cutBeforeUnclosedLink(prefix)
        String kept = rtrim(prefix.substring(0, cut))
        if (kept.isEmpty()) {
            return '\u2026'
        }
        return kept + suffix
    }

    @NonCPS
    private static int cutBeforeUnclosedLink(String prefix) {
        int separator = lastUnescapedLinkSeparator(prefix)
        if (separator < 0 || hasUnescapedCloseParen(prefix, separator + 2)) {
            return codePointSafeEnd(prefix)
        }
        int bracket = unescapedOpenBracket(prefix, separator)
        return bracket >= 0 ? bracket : separator
    }

    @NonCPS
    private static int lastUnescapedLinkSeparator(String prefix) {
        int from = prefix.length()
        while (from > 0) {
            int separator = prefix.lastIndexOf('](', from - 1)
            if (separator < 0) {
                return -1
            }
            if (!isEscaped(prefix, separator)) {
                return separator
            }
            from = separator
        }
        return -1
    }

    @NonCPS
    private static boolean hasUnescapedCloseParen(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            if (charEquals(text, i, ')') && !isEscaped(text, i)) {
                return true
            }
        }
        return false
    }

    @NonCPS
    private static int unescapedOpenBracket(String text, int before) {
        for (int i = before - 1; i >= 0; i--) {
            if (charEquals(text, i, '[') && !isEscaped(text, i)) {
                return i
            }
        }
        return -1
    }

    @NonCPS
    private static boolean charEquals(String text, int index, String symbol) {
        return text.substring(index, index + 1) == symbol
    }

    @NonCPS
    private static boolean isEscaped(String text, int index) {
        int slashes = 0
        for (int i = index - 1; i >= 0 && charEquals(text, i, '\\'); i--) {
            slashes++
        }
        return slashes % 2 == 1
    }

    @NonCPS
    private static int codePointSafeEnd(String text) {
        if (text.isEmpty()) {
            return 0
        }
        int end = text.length()
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            return end - 1
        }
        return end
    }

    @NonCPS
    private static String rtrim(String text) {
        int end = text.length()
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--
        }
        return text.substring(0, end)
    }
}
