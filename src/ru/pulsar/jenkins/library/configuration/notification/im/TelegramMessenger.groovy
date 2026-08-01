package ru.pulsar.jenkins.library.configuration.notification.im

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.databind.ObjectMapper
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.Secrets
import ru.pulsar.jenkins.library.configuration.notification.IMNotificationOptions

import static ru.pulsar.jenkins.library.configuration.Secrets.UNKNOWN_ID

class TelegramMessenger implements Messenger {

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
        return config.notificationsOptions.imNotificationOptions["telegram"]
    }

    @Override
    String getBotTokenCredentialId(JobConfiguration config, String repoSlug) {
        Secrets secrets = config.secrets
        return secrets.telegramBotToken == UNKNOWN_ID ? "TELEGRAM_BOT_TOKEN" : secrets.telegramBotToken
    }

    @Override
    String getChatIdCredentialId(JobConfiguration config, String repoSlug) {
        Secrets secrets = config.secrets
        return secrets.telegramChatId == UNKNOWN_ID ? repoSlug + "_TELEGRAM_CHAT_ID" : secrets.telegramChatId
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
}
