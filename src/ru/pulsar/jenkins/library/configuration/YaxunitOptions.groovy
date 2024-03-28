package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class YaxunitOptions implements Serializable {

    @JsonPropertyDescription("""Путь к конфигурационному файлу vanessa-runner.
    По умолчанию содержит значение "./tools/vrunner.json".
    """)
    String vrunnerSettings = "./tools/vrunner.json"

    @JsonPropertyDescription("""Путь к конфигурационному файлу YAXUnit.
    По умолчанию содержит значение "./tools/yaxunit.json".
    """)
    String configPath = "./tools/yaxunit.json"

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
        return "YaxunitOptions{" +
            "vrunnerSettings='" + vrunnerSettings + '\'' +
            ", configPath='" + configPath +
            ", publishToAllureReport='" + publishToAllureReport +
            ", publishToJUnitReport='" + publishToJUnitReport +
            '}'
    }
}
