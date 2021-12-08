package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class StageFlags implements Serializable {
    @JsonPropertyDescription("Анализ SonarQube включен")
    Boolean sonarqube

    @JsonPropertyDescription("Синтаксический контроль включен")
    Boolean syntaxCheck

    @JsonPropertyDescription("Валидация EDT включена")
    Boolean edtValidate

    @JsonPropertyDescription("Дымовые тесты включены")
    Boolean smoke

    @JsonPropertyDescription("Предварительные шаги инициализации включены")
    Boolean initSteps

    @JsonPropertyDescription("Запуск BDD сценариев включен")
    Boolean bdd

    @Override
    @NonCPS
    String toString() {
        return "StageFlags{" +
            "sonarqube=" + sonarqube +
            ", syntaxCheck=" + syntaxCheck +
            ", edtValidate=" + edtValidate +
            ", smoke=" + smoke +
            ", initSteps=" + initSteps +
            ", bdd=" + bdd +
            '}';
    }

    boolean needInfoBase() {
        return smoke || syntaxCheck || initSteps || bdd
    }
}
