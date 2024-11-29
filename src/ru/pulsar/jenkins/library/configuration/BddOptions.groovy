package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class BddOptions extends StepCoverageOptions implements Serializable {

    @JsonPropertyDescription("""Шаги, запускаемые через vrunner.
    В каждой строке передается отдельная команда 
    vrunner и ее аргументы (например, "vanessa --settings ./tools/vrunner.json").
    По умолчанию содержит одну команду "vanessa --settings ./tools/vrunner.json".
    """)
    String[] vrunnerSteps = [
        'vanessa --settings ./tools/vrunner.json'
    ]

    @JsonPropertyDescription("Выполнять замер покрытия")
    Boolean coverage = false

    @JsonPropertyDescription("Порт, на котором будет запущен сервер отладки для замера покрытия")
    int dbgsPort = 1550

    @Override
    @NonCPS
    String toString() {
        return "BddOptions{" +
            "vrunnerSteps=" + vrunnerSteps +
            "coverage=" + coverage +
            "dbgsPort=" + dbgsPort +
            '}'
    }
}
