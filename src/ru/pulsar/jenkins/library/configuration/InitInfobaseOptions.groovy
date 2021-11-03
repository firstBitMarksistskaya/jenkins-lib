package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class InitInfobaseOptions implements Serializable {

    @JsonPropertyDescription("""
    Способ инициализации информационной базы.
    Поддерживается три варианта:
        * fromStorage - инициализация информационной базы из хранилища конфигурации;
        * fromSource - инициализация информационной базы из исходников конфигурации;
        * defaultBranchFromStorage - инициализация основной ветки из хранилища конфигурации, остальных - из исходников конфигурации.
    По умолчанию содержит значение "fromStorage".""")
    InitInfobaseMethod initMethod = InitInfobaseMethod.FROM_STORAGE;

    @JsonPropertyDescription("Запустить миграцию ИБ")
    boolean runMigration = true

    @JsonPropertyDescription("""Дополнительные шаги, запускаемые через vrunner.
    В каждой строке передается отдельная команда 
    vrunner и ее аргументы (например, "vanessa --settings ./tools/vrunner.first.json")
    """)
    String[] additionalInitializationSteps

    @Override
    @NonCPS
    String toString() {
        return "InitInfobaseOptions{" +
            "initMethod=" + initMethod +
            ", runMigration=" + runMigration +
            ", additionalInitializationSteps=" + additionalInitializationSteps +
            '}';
    }
}
