package ru.pulsar.jenkins.library.configuration.notification.im

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.databind.ObjectMapper
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.Secrets
import ru.pulsar.jenkins.library.configuration.notification.IMNotificationOptions
import ru.pulsar.jenkins.library.configuration.notification.TelegramNotificationOptions

import static ru.pulsar.jenkins.library.configuration.Secrets.UNKNOWN_ID

class TelegramMessenger implements Messenger {

    public static final String TELEGRAM_HTTP_PROXY_CREDENTIAL_ID = "TELEGRAM_HTTP_PROXY"
    public static final String TELEGRAM_HTTP_PROXY_AUTH_CREDENTIAL_ID = "TELEGRAM_HTTP_PROXY_AUTH"
    public static final String TELEGRAM_CHAT_ID = "TELEGRAM_CHAT_ID"
    public static final String TELEGRAM_CHAT_ID_OTHER_BRANCHES = "TELEGRAM_CHAT_ID_OTHER_BRANCHES"

    private static final MarkdownV2Flavor FLAVOR = new MarkdownV2Flavor()

    @Override
    @NonCPS
    String name() {
        return "Telegram"
    }

    @Override
    boolean isEnabled(JobConfiguration config) {
        return config.stageFlags.telegram
    }

    @Override
    IMNotificationOptions getOptions(JobConfiguration config) {
        return config.notificationsOptions.imNotificationOptions[TelegramNotificationOptions.OPTIONS_KEY]
    }

    @Override
    String getBotTokenCredentialId(JobConfiguration config, String repoSlug) {
        Secrets secrets = config.secrets
        return secrets.telegramBotToken == UNKNOWN_ID ? "TELEGRAM_BOT_TOKEN" : secrets.telegramBotToken
    }

    @Override
    String getChatIdCredentialId(JobConfiguration config, String repoSlug, String branchName) {
        def options = getOptions(config)
        boolean useOtherBranchesChat = options instanceof TelegramNotificationOptions && options.useOtherBranchesChat == true
        return resolveChatIdCredentialId(
            config.secrets,
            branchName,
            config.defaultBranch,
            repoSlug,
            useOtherBranchesChat
        )
    }

    @NonCPS
    static String resolveChatIdCredentialId(
        Secrets secrets,
        String branch,
        String defaultBranch,
        String slug,
        boolean useOtherBranchesChat
    ) {
        if (useOtherBranchesChat && isOtherBranch(branch, defaultBranch)) {
            return configuredOrSlug(
                secrets == null ? null : secrets.telegramChatIdOtherBranches,
                slug,
                TELEGRAM_CHAT_ID_OTHER_BRANCHES
            )
        }
        return configuredOrSlug(
            secrets == null ? null : secrets.telegramChatId,
            slug,
            TELEGRAM_CHAT_ID
        )
    }

    @NonCPS
    private static String configuredOrSlug(String configuredId, String slug, String key) {
        if (configuredId == null || configuredId == UNKNOWN_ID || configuredId.trim().isEmpty()) {
            return slug + "_" + key
        }
        return configuredId
    }

    @NonCPS
    private static boolean isOtherBranch(String branch, String defaultBranch) {
        if (branch == null) {
            return false
        }
        String trimmed = branch.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        return trimmed != defaultBranch
    }

    @Override
    String buildUrl(String token, String chatId) {
        return "https://api.telegram.org/bot${token}/sendMessage"
    }

    @Override
    String buildBody(String chatId, String message) {
        def body = [
            chat_id                 : chatId,
            text                    : message,
            disable_web_page_preview: true,
            parse_mode              : 'MarkdownV2'
        ]
        return new ObjectMapper().writeValueAsString(body)
    }

    @Override
    List<Map<String, String>> buildHeaders(String token) {
        return []
    }

    @Override
    @NonCPS
    MarkdownFlavor getFlavor() {
        return FLAVOR
    }

    @Override
    @NonCPS
    int getMaxMessageLength() {
        return 4096
    }

    @Override
    String getHttpProxyCredentialId(IMNotificationOptions options) {
        return isHttpProxyEnabled(options) ? TELEGRAM_HTTP_PROXY_CREDENTIAL_ID : null
    }

    @Override
    String getProxyAuthenticationCredentialId(IMNotificationOptions options) {
        return isHttpProxyEnabled(options) ? TELEGRAM_HTTP_PROXY_AUTH_CREDENTIAL_ID : null
    }

    @NonCPS
    private static boolean isHttpProxyEnabled(IMNotificationOptions options) {
        return options instanceof TelegramNotificationOptions && options.useHttpProxy == true
    }
}
