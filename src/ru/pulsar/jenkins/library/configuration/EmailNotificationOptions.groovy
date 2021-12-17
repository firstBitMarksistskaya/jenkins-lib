package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import ru.pulsar.jenkins.library.configuration.email.EmailExtConfiguration

@JsonIgnoreProperties(ignoreUnknown = true)
class EmailNotificationOptions implements Serializable {

    Boolean onAlways
    Boolean onSuccess
    Boolean onFailure
    Boolean onUnstable

    EmailExtConfiguration alwaysEmailOptions
    EmailExtConfiguration successEmailOptions
    EmailExtConfiguration failureEmailOptions
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


