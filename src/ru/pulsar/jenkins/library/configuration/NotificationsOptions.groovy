package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import ru.pulsar.jenkins.library.configuration.notification.EmailNotificationOptions
import ru.pulsar.jenkins.library.configuration.notification.TelegramNotificationOptions

@JsonIgnoreProperties(ignoreUnknown = true)
class NotificationsOptions implements Serializable {

    @JsonProperty("email")
    @JsonPropertyDescription("Настройки рассылки результатов сборки через email")
    EmailNotificationOptions emailNotificationOptions;

    @JsonProperty("telegram")
    @JsonPropertyDescription("Настройки рассылки результатов сборки через telegram")
    TelegramNotificationOptions telegramNotificationOptions;

    @Override
    @NonCPS
    String toString() {
        return "NotificationOptions{" +
            "emailNotificationOptions=" + emailNotificationOptions +
            ", telegramNotificationOptions=" + telegramNotificationOptions +
            '}';
    }
}


