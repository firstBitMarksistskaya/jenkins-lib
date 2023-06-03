package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class CoverageOptions implements Serializable {
    
    @JsonPropertyDescription('''Путь к исполняемому файлу dbgs.
    По умолчанию равен /opt/1cv8/current/dbgs.
    ''')
    String dbgsPath

    @JsonPropertyDescription('''Путь к исполняемому файлу Coverage41C
    По умолчанию равен Coverage41C.
    ''')
    String coverage41CPath

    @Override
    @NonCPS
    String toString() {
        return "coverageOptions{" +
                "dbgsPath=" + dbgsPath +
                ", coverage41CPath=" + coverage41CPath +
                '}';
    }

}
