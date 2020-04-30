package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class StageFlags implements Serializable {
    @JsonPropertyDescription("Анализ SonarQube включен")
    boolean sonarqube

    @JsonPropertyDescription("Синтаксический контроль включен")
    boolean syntaxCheck

    @JsonPropertyDescription("Валидация EDT включена")
    boolean edtValidate

    @JsonPropertyDescription("Дымовые тесты включены")
    boolean smoke

    @Override
    @NonCPS
    String toString() {
        return "StageFlags{" +
            "sonarqube=" + sonarqube +
            ", syntaxCheck=" + syntaxCheck +
            ", edtValidate=" + edtValidate +
            ", smoke=" + smoke +
            '}';
    }

    boolean needInfobase() {
        return smoke || syntaxCheck
    }
}
