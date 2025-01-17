package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class SyntaxCheckOptions implements Serializable {

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

    @JsonPropertyDescription("""Выполнять публикацию результатов в отчет Allure.
    По умолчанию выключено.
    """)
    @JsonProperty(defaultValue = "false")
    boolean publishToAllureReport = false

    @JsonPropertyDescription("""Выполнять публикацию результатов в отчет JUnit.
    По умолчанию включено.
    """)
    @JsonProperty(defaultValue = "true")
    boolean publishToJUnitReport = true

    @Override
    @NonCPS
    String toString() {
        return "SyntaxCheckOptions{" +
            ", groupErrorsByMetadata=" + groupErrorsByMetadata +
            ", checkModes=" + checkModes +
            ", vrunnerSettings=" + vrunnerSettings +
            ", publishToAllureReport=" + publishToAllureReport +
            ", publishToJUnitReport=" + publishToJUnitReport +
            '}'
    }
}
