package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class ResultsTransformOptions implements Serializable {

    @JsonPropertyDescription("Фильтровать замечания по уровню поддержки модуля. По умолчанию включено.")
    Boolean removeSupport = true

    @JsonPropertyDescription("""Настройка фильтрации замечаний по уровню поддержки.
        0 - удалить файлы на замке;
        1 - удалить файлы на замке и на поддержке;
        2 - удалить файлы на замке, на поддержке и снятые с поддержки.
    """)
    Integer supportLevel

    @Override
    @NonCPS
    String toString() {
        return "ResultsTransformOptions{" +
            "removeSupport=" + removeSupport +
            ", supportLevel=" + supportLevel +
            '}';
    }
}
