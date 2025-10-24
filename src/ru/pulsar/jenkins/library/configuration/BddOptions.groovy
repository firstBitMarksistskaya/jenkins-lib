package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class BddOptions extends StepCoverageOptions implements Serializable {

    @JsonPropertyDescription("""Путь к конфигурационному файлу vanessa-runner.
    По умолчанию содержит значение "./tools/vrunner.json".
    """)
    String vrunnerSettings = "./tools/vrunner.json"

    @JsonPropertyDescription("""Шаги, запускаемые через vrunner.
    В каждой строке передается отдельная команда 
    vrunner и ее аргументы (например, "vanessa --settings ./tools/vrunner.json").
    По умолчанию содержит одну команду "vanessa --settings ./tools/vrunner.json".
    """)
    String[] vrunnerSteps = [
        'vanessa --settings ./tools/vrunner.json'
    ]

    @JsonPropertyDescription("""Настройки сохранения базы после выполнения всех шагов
    """)
    ArchiveInfobaseOptions archiveInfobase

    @Override
    @NonCPS
    String toString() {
        return "BddOptions{" +
            "vrunnerSettings='" + vrunnerSettings + '\'' +
            "vrunnerSteps=" + vrunnerSteps +
            "archiveInfobase=" + archiveInfobase +
            "coverage=" + coverage +
            "dbgsPort=" + dbgsPort +
            '}'
    }
}
