package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class SyntaxCheckOptions implements Serializable {

    @JsonPropertyDescription("Путь к файлу отчета jUnit")
    String pathToJUnitReport

    @JsonPropertyDescription("Группировать выявленные ошибки по объектам метаданных")
    boolean groupErrorsByMetadata;

    @JsonPropertyDescription("Режимы проверки конфигурации")
    String[] checkModes;

    @Override
    @NonCPS
    String toString() {
        return "SyntaxCheckOptions{" +
            "pathToJUnitReport='" + pathToJUnitReport + '\'' +
            ", groupErrorsByMetadata=" + groupErrorsByMetadata +
            ", checkModes=" + checkModes +
            '}';
    }
}
