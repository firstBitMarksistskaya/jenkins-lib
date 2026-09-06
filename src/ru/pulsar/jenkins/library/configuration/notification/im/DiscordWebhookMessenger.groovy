package ru.pulsar.jenkins.library.configuration.notification.im

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.databind.ObjectMapper
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.Secrets
import ru.pulsar.jenkins.library.configuration.notification.IMNotificationOptions

import static ru.pulsar.jenkins.library.configuration.Secrets.UNKNOWN_ID

class DiscordWebhookMessenger implements Messenger {

    private static final DiscordFlavor FLAVOR = new DiscordFlavor()

    @Override
    @NonCPS
    String name() {
        return "Discord (webhook)"
    }

    @Override
    boolean isEnabled(JobConfiguration config) {
        return config.stageFlags.discordWebhook
    }

    @Override
    IMNotificationOptions getOptions(JobConfiguration config) {
        return config.notificationsOptions.imNotificationOptions["discordWebhook"]
    }

    @Override
    String getBotTokenCredentialId(JobConfiguration config, String repoSlug) {
        return null
    }

    @Override
    String getChatIdCredentialId(JobConfiguration config, String repoSlug) {
        Secrets secrets = config.secrets
        return secrets.discordWebhookUrl == UNKNOWN_ID ? repoSlug + "_DISCORD_WEBHOOK_URL" : secrets.discordWebhookUrl
    }

    @Override
    String buildUrl(String token, String chatId) {
        return chatId
    }

    @Override
    String buildBody(String chatId, String message) {
        def body = [
            embeds          : [[description: message]],
            allowed_mentions: [parse: []]
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
