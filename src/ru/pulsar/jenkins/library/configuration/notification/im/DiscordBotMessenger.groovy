package ru.pulsar.jenkins.library.configuration.notification.im

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.databind.ObjectMapper
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.Secrets
import ru.pulsar.jenkins.library.configuration.notification.IMNotificationOptions

import static ru.pulsar.jenkins.library.configuration.Secrets.UNKNOWN_ID

class DiscordBotMessenger implements Messenger {

    private static final String DISCORD_API_URL = "https://discord.com/api/v10/channels"

    private static final DiscordFlavor FLAVOR = new DiscordFlavor()

    @Override
    @NonCPS
    String name() {
        return "Discord (bot)"
    }

    @Override
    boolean isEnabled(JobConfiguration config) {
        return config.stageFlags.discordBot
    }

    @Override
    IMNotificationOptions getOptions(JobConfiguration config) {
        return config.notificationsOptions.imNotificationOptions["discordBot"]
    }

    @Override
    String getBotTokenCredentialId(JobConfiguration config, String repoSlug) {
        Secrets secrets = config.secrets
        return secrets.discordBotToken == UNKNOWN_ID ? "DISCORD_BOT_TOKEN" : secrets.discordBotToken
    }

    @Override
    String getChatIdCredentialId(JobConfiguration config, String repoSlug) {
        Secrets secrets = config.secrets
        return secrets.discordChatId == UNKNOWN_ID ? repoSlug + "_DISCORD_CHAT_ID" : secrets.discordChatId
    }

    @Override
    String buildUrl(String token, String chatId) {
        return "${DISCORD_API_URL}/${chatId}/messages"
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
        return [[name: 'Authorization', value: "Bot ${token}".toString()]]
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
