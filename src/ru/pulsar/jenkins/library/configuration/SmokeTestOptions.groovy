package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class SmokeTestOptions extends StepCoverageOptions implements Serializable {

    @JsonPropertyDescription("""Путь к конфигурационному файлу vanessa-runner.
    По умолчанию содержит значение "./tools/vrunner.json".
    """)
    String vrunnerSettings

    @JsonPropertyDescription("""Строка подключения к своей базе стадии в формате `--ibconnection`: серверная база
    или абсолютный путь к файловой базе. Если не указана, стадия идет на базе сборки (basePath).
    Свою базу стадии пайплайн готовит так же, как базу сборки, и параллельно с ней, а стадии на разных базах
    идут параллельно.
    """)
    String basePath

    @JsonPropertyDescription("""Путь к конфигурационному файлу для xddTestRunner.
    По умолчанию содержит значение "./tools/xUnitParams.json".
    """)
    String xddConfigPath

    @JsonPropertyDescription("""Выполнять публикацию результатов в отчет Allure.
    По умолчанию выключено.
    """)
    boolean publishToAllureReport

    @JsonPropertyDescription("""Выполнять публикацию результатов в отчет JUnit.
    По умолчанию включено.
    """)
    boolean publishToJUnitReport

    @Override
    @NonCPS
    String toString() {
        return "SmokeTestOptions{" +
            "vrunnerSettings='" + vrunnerSettings + '\'' +
            ", basePath='" + basePath + '\'' +
            ", xddConfigPath='" + xddConfigPath + '\'' +
            ", publishToAllureReport=" + publishToAllureReport +
            ", publishToJUnitReport=" + publishToJUnitReport +
            ", coverage=" + coverage +
            ", dbgsPort=" + dbgsPort +
            '}'
    }
}
