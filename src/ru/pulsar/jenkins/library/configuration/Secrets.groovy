package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class Secrets implements Serializable {

    public static final String UNKNOWN_ID = "UNKNOWN_ID"

    @JsonPropertyDescription("Путь к хранилищу конфигурации")
    String storagePath

    @JsonPropertyDescription("Данные авторизации в хранилище конфигурации")
    String storage

    @JsonPropertyDescription("Идентификатор telegram-чата для отправки уведомлений")
    String telegramChatId

    @JsonPropertyDescription("Идентификатор telegram-чата для уведомлений о сборках прочих веток (не defaultBranch). Если UNKNOWN_ID, используется {slug}_TELEGRAM_CHAT_ID_OTHER_BRANCHES")
    String telegramChatIdOtherBranches

    @JsonPropertyDescription("Токен авторизации telegram-бота для отправки уведомлений")
    String telegramBotToken

    @JsonPropertyDescription("Discord webhook URL для отправки уведомлений")
    String discordWebhookUrl

    @JsonPropertyDescription("Токен авторизации Discord-бота для отправки уведомлений")
    String discordBotToken

    @JsonPropertyDescription("Идентификатор Discord-канала для отправки уведомлений")
    String discordChatId

    @Override
    @NonCPS
    String toString() {
        return "Secrets{" +
            "storagePath='" + storagePath + '\'' +
            ", storage='" + storage + '\'' +
            ", telegramChatId='" + telegramChatId + '\'' +
            ", telegramChatIdOtherBranches='" + telegramChatIdOtherBranches + '\'' +
            ", telegramBotToken='" + telegramBotToken + '\'' +
            ", discordWebhookUrl='" + discordWebhookUrl + '\'' +
            ", discordBotToken='" + discordBotToken + '\'' +
            ", discordChatId='" + discordChatId + '\'' +
            '}';
    }
}
