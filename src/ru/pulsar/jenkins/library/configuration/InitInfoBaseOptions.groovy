package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class InitInfoBaseOptions implements Serializable {

    @JsonPropertyDescription("""
    Способ инициализации информационной базы.
    Поддерживается три варианта:
        * fromStorage - инициализация информационной базы из хранилища конфигурации;
        * fromSource - инициализация информационной базы из исходников конфигурации;
        * defaultBranchFromStorage - инициализация основной ветки из хранилища конфигурации, остальных - из исходников конфигурации.
    По умолчанию содержит значение "fromStorage".""")
    InitInfoBaseMethod initMethod = InitInfoBaseMethod.FROM_STORAGE;

    @JsonPropertyDescription("Запустить миграцию ИБ")
    Boolean runMigration = true

    @JsonPropertyDescription("""Дополнительные шаги, запускаемые через vrunner.
    В каждой строке передается отдельная команда 
    vrunner и ее аргументы (например, "vanessa --settings ./tools/vrunner.first.json")
    """)
    String[] additionalInitializationSteps

    @Override
    @NonCPS
    String toString() {
        return "InitInfoBaseOptions{" +
            "initMethod=" + initMethod +
            ", runMigration=" + runMigration +
            ", additionalInitializationSteps=" + additionalInitializationSteps +
            '}';
    }
}
