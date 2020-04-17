package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class SonarQubeOptions implements Serializable {

    @JsonPropertyDescription(
        "Имя настроенного SonarQube-сервера (SonarQube installations).\nЕсли настроен только один сервер, то может быть оставлено пустым."
    )
    String sonarQubeInstallation;

    @JsonPropertyDescription("Использовать sonar-scanner, доступный в PATH")
    boolean useSonarScannerFromPath

    @JsonPropertyDescription(
        "Имя настроенной утилиты sonar-scanner.\nПрименяется, если useSonarScannerFromPath установлено в false."
    )
    String sonarScannerToolName

    @Override
    @NonCPS
    String toString() {
        return "SonarQubeOptions{" +
            "useSonarScannerFromPath=" + useSonarScannerFromPath +
            ", sonarScannerToolName='" + sonarScannerToolName + '\'' +
            ", sonarQubeInstallation='" + sonarQubeInstallation + '\'' +
            '}';
    }
}
