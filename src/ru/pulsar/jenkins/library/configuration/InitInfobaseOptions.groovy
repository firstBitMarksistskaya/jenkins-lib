package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class InitInfobaseOptions implements Serializable {

    @JsonPropertyDescription("Запустить миграцию ИБ")
    boolean runMigration = true

    @JsonPropertyDescription("""Дополнительные шаги, запускаемые через vrunner.
    В каждой строке передается отдельная команда 
    vrunner и ее аргументы (например, "vanessa --settings ./tools/vrunner.first.json")
    """)
    String[] additionalMigrationSteps

    @Override
    @NonCPS
    String toString() {
        return "InitInfobaseOptions{" +
            "runMigration=" + runMigration +
            ", additionalMigrationSteps=" + additionalMigrationSteps +
            '}';
    }
}
