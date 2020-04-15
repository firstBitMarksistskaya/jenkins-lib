package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class JobConfiguration implements Serializable {
    @JsonPropertyDescription("Версия платформы 1С:Предприятие в формате 8.3.хх.хххх.")
    String v8version

    @JsonPropertyDescription("Идентификаторы сохраненных секретов")
    Secrets secrets;

    @JsonProperty("sonarqube")
    @JsonPropertyDescription("Настройки анализа SonarQube")
    SonarQubeOptions sonarQubeOptions;

    @Override
    @NonCPS
    String toString() {
        return "JobConfiguration{" +
            "v8version='" + v8version + '\'' +
            ", secrets=" + secrets +
            ", sonarQubeOptions=" + sonarQubeOptions +
            '}';
    }
}