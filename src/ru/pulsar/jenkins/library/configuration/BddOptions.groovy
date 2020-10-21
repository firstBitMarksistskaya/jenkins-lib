package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class BddOptions implements Serializable {

    @JsonPropertyDescription("""Шаги, запускаемые через vrunner.
    В каждой строке передается отдельная команда 
    vrunner и ее аргументы (например, "vanessa --settings ./tools/vrunner.json").
    По умолчанию содержит одну команду "vanessa --settings ./tools/vrunner.json".
    """)
    String[] vrunnerSteps = [
        'vanessa --settings ./tools/vrunner.json'
    ]

    @Override
    @NonCPS
    String toString() {
        return "BddOptions{" +
            "vrunnerSteps=" + vrunnerSteps +
            '}';
    }
}
