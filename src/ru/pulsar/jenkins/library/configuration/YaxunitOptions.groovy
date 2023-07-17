package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class YaxunitOptions implements Serializable {

    @JsonPropertyDescription("""Путь к конфигурационному файлу vanessa-runner.
    По умолчанию содержит значение "./tools/vrunner.json".
    """)
    String vrunnerSettings = "./tools/vrunner.json"

    @JsonProperty("extensions")
    @JsonPropertyDescription("""Расширения с тестами.
    Массив объектов с полями name и src, где
        name - имя расширения
        src - путь к расширению (к cfe или к исходникам)
    По умолчанию содержит один элемент - YAXUNIT версии 23.05.
    """)
    Extension[] extensions

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
        return "YaxunitTestOptions{" +
            "vrunnerSettings='" + vrunnerSettings + '\'' +
            ", extensions='" + extensions +
            ", configPath='" + configPath +
            '}'
    }
}
