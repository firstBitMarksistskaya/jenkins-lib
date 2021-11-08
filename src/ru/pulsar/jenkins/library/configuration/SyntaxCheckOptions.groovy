package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class SyntaxCheckOptions implements Serializable {

    @JsonPropertyDescription("""Путь к файлу отчета jUnit
    По умолчанию содержит значение "./build/out/jUnit/syntax.xml"
    """)
    String pathToJUnitReport = "./build/out/jUnit/syntax.xml"

    @JsonPropertyDescription("""Группировать выявленные ошибки по объектам метаданных.
    По умолчанию включено.
    """)
    boolean groupErrorsByMetadata = true

    @JsonPropertyDescription("Режимы проверки конфигурации")
    String[] checkModes

    @JsonPropertyDescription("""Путь к файлу с указанием пропускаемых ошибок.
    Формат файла: в каждой строке файла указан текст пропускаемого исключения или его часть
    Кодировка: UTF-8
    """)
    String exceptionFile = "./tools/syntax-check-exception-file.txt"

    @JsonPropertyDescription("""Путь к конфигурационному файлу vanessa-runner.
    По умолчанию содержит значение "./tools/vrunner.json".
    """)
    String vrunnerSettings = "./tools/vrunner.json"

    @Override
    @NonCPS
    String toString() {
        return "SyntaxCheckOptions{" +
            "pathToJUnitReport='" + pathToJUnitReport + '\'' +
            ", groupErrorsByMetadata=" + groupErrorsByMetadata +
            ", checkModes=" + checkModes +
            ", vrunnerSettings=" + vrunnerSettings +
            '}';
    }
}
