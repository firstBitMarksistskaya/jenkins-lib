package ru.pulsar.jenkins.library.configuration.notification.im

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.databind.ObjectMapper
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.Secrets
import ru.pulsar.jenkins.library.configuration.notification.IMNotificationOptions

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import static ru.pulsar.jenkins.library.configuration.Secrets.UNKNOWN_ID

class MaxMessenger implements Messenger {

    private static final String MAX_API_URL = "https://platform-api2.max.ru/messages"

    private static final StandardMarkdownFlavor FLAVOR = new StandardMarkdownFlavor()

    @Override
    @NonCPS
    String name() {
        return "MAX"
    }

    @Override
    boolean isEnabled(JobConfiguration config) {
        return config.stageFlags.max
    }

    @Override
    IMNotificationOptions getOptions(JobConfiguration config) {
        return config.notificationsOptions.imNotificationOptions["max"]
    }

    @Override
    String getBotTokenCredentialId(JobConfiguration config, String repoSlug) {
        Secrets secrets = config.secrets
        return secrets.maxBotToken == UNKNOWN_ID ? "MAX_BOT_TOKEN" : secrets.maxBotToken
    }

    @Override
    String getChatIdCredentialId(JobConfiguration config, String repoSlug) {
        Secrets secrets = config.secrets
        return secrets.maxChatId == UNKNOWN_ID ? repoSlug + "_MAX_CHAT_ID" : secrets.maxChatId
    }

    @Override
    String buildUrl(String token, String chatId) {
        String encodedChatId = URLEncoder.encode(chatId, StandardCharsets.UTF_8.name())
        return "${MAX_API_URL}?chat_id=${encodedChatId}"
    }

    @Override
    String buildBody(String chatId, String message) {
        def body = [
            text                : message,
            format              : 'markdown',
            disable_link_preview: true
        ]
        return new ObjectMapper().writeValueAsString(body)
    }

    @Override
    List<Map<String, String>> buildHeaders(String token) {
        return [[name: 'Authorization', value: token]]
    }

    @Override
    @NonCPS
    MarkdownFlavor getFlavor() {
        return FLAVOR
    }

    @Override
    @NonCPS
    int getMaxMessageLength() {
        return 4000
    }
}
