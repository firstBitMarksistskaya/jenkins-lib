package ru.pulsar.jenkins.library.configuration.notification

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import ru.pulsar.jenkins.library.configuration.notification.email.EmailExtConfiguration

@JsonIgnoreProperties(ignoreUnknown = true)
class EmailNotificationOptions implements Serializable {

    @JsonPropertyDescription("Отправлять всегда")
    Boolean onAlways
    @JsonPropertyDescription("Отправлять при успешной сборке")
    Boolean onSuccess
    @JsonPropertyDescription("Отправлять при падении сборки")
    Boolean onFailure
    @JsonPropertyDescription("Отправлять при нестабильной сборке")
    Boolean onUnstable

    @JsonProperty("alwaysOptions")
    EmailExtConfiguration alwaysEmailOptions
    @JsonProperty("successOptions")
    EmailExtConfiguration successEmailOptions
    @JsonProperty("failureOptions")
    EmailExtConfiguration failureEmailOptions
    @JsonProperty("unstableOptions")
    EmailExtConfiguration unstableEmailOptions

    @Override
    @NonCPS
    String toString() {
        return "EmailNotificationOptions{" +
            "onAlways=" + onAlways +
            ", onSuccess=" + onSuccess +
            ", onFailure=" + onFailure +
            ", onUnstable=" + onUnstable +
            ", alwaysEmailOptions=" + alwaysEmailOptions +
            ", successEmailOptions=" + successEmailOptions +
            ", failureEmailOptions=" + failureEmailOptions +
            ", unstableEmailOptions=" + unstableEmailOptions +
            '}';
    }
}


