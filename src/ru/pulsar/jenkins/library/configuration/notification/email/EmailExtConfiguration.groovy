package ru.pulsar.jenkins.library.configuration.notification.email

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class EmailExtConfiguration implements Serializable {
    Boolean attachLog;
    String[] directRecipients
    RecipientProvider[] recipientProviders

    @Override
    @NonCPS
    String toString() {
        return "EmailExtConfiguration{" +
            "attachLog=" + attachLog +
            ", directRecipients=" + directRecipients +
            ", recipientProviders=" + recipientProviders +
            '}';
    }
}
