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

    @JsonPropertyDescription("""Ссылка на скачивание YAXUnit.
    По умолчанию содержит ссылку на официальный релиз версии 23.05.
    """)
    String cfe = "https://github.com/bia-technologies/yaxunit/releases/download/23.05/YAXUNIT-23.05.cfe"

    @JsonPropertyDescription("""Имена расширений с тестами.
    По умолчанию содержит один элемент - YAXUNIT.
    """)
    String[] extensionNames = ['YAXUNIT']

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
            ", cfe='" + cfe +
            ", extensionNames='" + extensionNames.toString() +
            ", configPath='" + configPath +
            '}'
    }
}
