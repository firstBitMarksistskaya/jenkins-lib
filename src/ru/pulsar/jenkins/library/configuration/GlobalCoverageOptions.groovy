package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class GlobalCoverageOptions implements Serializable {
    
    @JsonPropertyDescription('''Путь к исполняемому файлу dbgs.
    По умолчанию ищется с помощью v8find для указанной версии платформы (v8version).
    ''')
    String dbgsPath

    @JsonPropertyDescription('''Путь к исполняемому файлу Coverage41C
    По умолчанию ищется в PATH.
    ''')
    String coverage41CPath

    @Override
    @NonCPS
    String toString() {
        return "coverageOptions{" +
                "dbgsPath=" + dbgsPath +
                ", coverage41CPath=" + coverage41CPath +
                '}'
    }

}
