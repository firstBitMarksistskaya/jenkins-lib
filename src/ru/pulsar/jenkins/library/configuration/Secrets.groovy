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

    @JsonPropertyDescription("Токен авторизации telegram-бота для отправки уведомлений")
    String telegramBotToken

    @JsonPropertyDescription("Discord webhook URL для отправки уведомлений")
    String discordWebhookUrl

    @JsonPropertyDescription("Токен авторизации Discord-бота для отправки уведомлений")
    String discordBotToken

    @JsonPropertyDescription("Идентификатор Discord-канала для отправки уведомлений")
    String discordChatId

    @JsonPropertyDescription("Данные авторизации администратора кластера серверной базы. Нужны, если у кластера есть администраторы")
    String clusterAdmin

    @JsonPropertyDescription("Пароль сервера отладки кластера (-debugServerPwd) для замеров покрытия на серверной базе, secret text. Нужен, если сервер отладки запущен с паролем")
    String clusterDebugServerPassword

    @Override
    @NonCPS
    String toString() {
        return "Secrets{" +
            "storagePath='" + storagePath + '\'' +
            ", storage='" + storage + '\'' +
            ", telegramChatId='" + telegramChatId + '\'' +
            ", telegramBotToken='" + telegramBotToken + '\'' +
            ", discordWebhookUrl='" + discordWebhookUrl + '\'' +
            ", discordBotToken='" + discordBotToken + '\'' +
            ", discordChatId='" + discordChatId + '\'' +
            ", clusterAdmin='" + clusterAdmin + '\'' +
            ", clusterDebugServerPassword='" + clusterDebugServerPassword + '\'' +
            '}';
    }
}
