package ru.pulsar.jenkins.library.configuration.notification.im

import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.notification.IMNotificationOptions

interface Messenger extends Serializable {

    String name()

    boolean isEnabled(JobConfiguration config)

    IMNotificationOptions getOptions(JobConfiguration config)

    String getBotTokenCredentialId(JobConfiguration config, String repoSlug)

    String getChatIdCredentialId(JobConfiguration config, String repoSlug)

    String buildUrl(String token, String chatId)

    String buildBody(String chatId, String message)

    List<Map<String, String>> buildHeaders(String token)

    MarkdownFlavor getFlavor()

    int getMaxMessageLength()
}
