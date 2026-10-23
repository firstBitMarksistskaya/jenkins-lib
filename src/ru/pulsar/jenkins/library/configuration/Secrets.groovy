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

    @JsonPropertyDescription("Идентификатор учетных данных Jenkins с именем пользователя и паролем администратора кластера. Если идентификатор не задан, используется CLUSTER_ADMIN. Если таких учетных данных нет, команда выполняется без них")
    String clusterAdmin

    @JsonPropertyDescription("Идентификатор секретного текста Jenkins с паролем сервера отладки кластера (-debugServerPwd). Если идентификатор не задан, используется DEBUG_SERVER_PASSWORD. Если таких учетных данных нет, команда выполняется без них")
    String debugServerPassword

    @JsonPropertyDescription("Идентификатор учетных данных Jenkins с именем пользователя и паролем пользователя информационной базы для vanessa-runner и клиентов тестирования Vanessa Automation и Vanessa ADD. Если идентификатор не задан, используется {slug}_INFOBASE_USER. Если таких учетных данных нет, пользователь и пароль берутся из файлов настроек vanessa-runner")
    String infobaseUser

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
            ", clusterAdmin='" + clusterAdmin + '\'' +
            ", debugServerPassword='" + debugServerPassword + '\'' +
            ", infobaseUser='" + infobaseUser + '\'' +
            '}';
    }
}
