package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import ru.pulsar.jenkins.library.configuration.notification.EmailNotificationOptions
import ru.pulsar.jenkins.library.configuration.notification.IMNotificationOptions
import ru.pulsar.jenkins.library.configuration.notification.TelegramNotificationOptions

@JsonIgnoreProperties(ignoreUnknown = true)
class NotificationsOptions implements Serializable {

    @JsonProperty("email")
    @JsonPropertyDescription("Настройки рассылки результатов сборки через email")
    EmailNotificationOptions emailNotificationOptions;

    @JsonProperty("telegram")
    @JsonPropertyDescription("Настройки рассылки результатов сборки через telegram")
    TelegramNotificationOptions telegramNotificationOptions;

    @JsonIgnore
    Map<String, IMNotificationOptions> imNotificationOptions = [:]

    @JsonAnySetter
    @NonCPS
    void addImNotificationOptions(String key, Map<String, Object> value) {
        if (value == null) {
            return
        }
        IMNotificationOptions opts = new IMNotificationOptions()
        opts.onAlways = value["onAlways"] as Boolean
        opts.onSuccess = value["onSuccess"] as Boolean
        opts.onFailure = value["onFailure"] as Boolean
        opts.onUnstable = value["onUnstable"] as Boolean
        imNotificationOptions[key] = opts
    }

    @Override
    @NonCPS
    String toString() {
        return "NotificationOptions{" +
            "emailNotificationOptions=" + emailNotificationOptions +
            ", telegramNotificationOptions=" + telegramNotificationOptions +
            ", imNotificationOptions=" + imNotificationOptions +
            '}';
    }
}
