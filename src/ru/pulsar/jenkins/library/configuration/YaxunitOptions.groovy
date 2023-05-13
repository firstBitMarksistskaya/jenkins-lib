package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class YaxunitOptions implements Serializable {

    @JsonPropertyDescription("""Путь к конфигурационному файлу vanessa-runner.
    По умолчанию содержит значение "./tools/vrunner.json".
    """)
    String vrunnerSettings

    @Override
    @NonCPS
    String toString() {
        return "YaxunitTestOptions{" +
            "vrunnerSettings='" + vrunnerSettings + '\'' +
            ", configPath='" + configPath +
            '}'
    }
}
