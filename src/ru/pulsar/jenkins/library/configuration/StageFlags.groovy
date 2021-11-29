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

    @JsonPropertyDescription("Предварительные шаги инициализации включены")
    boolean initSteps

    @JsonPropertyDescription("Запуск BDD сценариев включен")
    boolean bdd

    @JsonPropertyDescription("Подготовка Swagger документации")
    boolean swagger

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
            ", swagger=" + swagger +
            '}';
    }

    boolean needInfobase() {
        return smoke || syntaxCheck || initSteps || bdd
    }
}
