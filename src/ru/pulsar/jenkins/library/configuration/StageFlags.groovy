package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class StageFlags implements Serializable {
    @JsonPropertyDescription("Анализ SonarQube включен")
    boolean sonarQube

    @JsonPropertyDescription("Синтаксический контроль включен")
    boolean syntaxCheck

    @Override
    @NonCPS
    String toString() {
        return "StageFlags{" +
            "sonarQube=" + sonarQube +
            ", syntaxCheck=" + syntaxCheck +
            '}';
    }
}
