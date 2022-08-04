package ru.pulsar.jenkins.library.configuration.notification

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class TelegramNotificationOptions implements Serializable {

    @JsonPropertyDescription("Отправлять всегда")
    Boolean onAlways
    @JsonPropertyDescription("Отправлять при успешной сборке")
    Boolean onSuccess
    @JsonPropertyDescription("Отправлять при падении сборки")
    Boolean onFailure
    @JsonPropertyDescription("Отправлять при нестабильной сборке")
    Boolean onUnstable

    @Override
    @NonCPS
    String toString() {
        return "TelegramNotificationOptions{" +
            "onAlways=" + onAlways +
            ", onSuccess=" + onSuccess +
            ", onFailure=" + onFailure +
            ", onUnstable=" + onUnstable +
            '}';
    }
}


