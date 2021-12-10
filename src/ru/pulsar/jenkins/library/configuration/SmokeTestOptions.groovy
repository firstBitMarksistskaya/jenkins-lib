package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class SmokeTestOptions implements Serializable {

    @JsonPropertyDescription("""Путь к конфигурационному файлу vanessa-runner.
    По умолчанию содержит значение "./tools/vrunner.json".
    """)
    String vrunnerSettings

    @JsonPropertyDescription("""Путь к конфигурационному файлу для xddTestRunner.
    По умолчанию содержит значение "./tools/xUnitParams.json".
    """)
    String xddConfigPath;

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
            ", xddConfigPath='" + xddConfigPath + '\'' +
            ", publishToAllureReport=" + publishToAllureReport +
            ", publishToJUnitReport=" + publishToJUnitReport +
            '}'
    }
}
