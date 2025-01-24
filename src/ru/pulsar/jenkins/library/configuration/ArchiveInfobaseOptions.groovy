package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class ArchiveInfobaseOptions implements Serializable {

    @JsonPropertyDescription("Сохранять всегда")
    Boolean onAlways = false
    @JsonPropertyDescription("Сохранять при успешной сборке")
    Boolean onSuccess = false
    @JsonPropertyDescription("Сохранять при падении сборки")
    Boolean onFailure = false
    @JsonPropertyDescription("Сохранять при нестабильной сборке")
    Boolean onUnstable  = false

    @Override
    @NonCPS
    String toString() {
        return "ArchiveInfobaseOptions{" +
            "onAlways=" + onAlways +
            ", onSuccess=" + onSuccess +
            ", onFailure=" + onFailure +
            ", onUnstable=" + onUnstable +
            '}';
    }
}


