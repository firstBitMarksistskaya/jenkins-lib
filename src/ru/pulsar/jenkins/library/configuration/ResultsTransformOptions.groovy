package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.annotation.JsonProperty
import ru.pulsar.jenkins.library.configuration.sonarqube.GenericIssueFormat

@JsonIgnoreProperties(ignoreUnknown = true)
class ResultsTransformOptions implements Serializable {

    @JsonPropertyDescription("""Способ преобразования замечаний.
    Поддерживается stebi и edt-ripper.
    По умолчанию содержит значение "stebi".
    """)
    @JsonProperty(defaultValue = "stebi")
    ResultsTransformerType transformer

    @JsonPropertyDescription("Фильтровать замечания по уровню поддержки модуля. Только для stebi. По умолчанию включено.")
    Boolean removeSupport = true

    @JsonPropertyDescription("""Настройка фильтрации замечаний по уровню поддержки. Только для stebi.
        0 - удалить файлы на замке;
        1 - удалить файлы на замке и на поддержке;
        2 - удалить файлы на замке, на поддержке и снятые с поддержки.
    """)
    Integer supportLevel

    @JsonPropertyDescription("""Формат отчета generic issue. Только для stebi.
    Для SonarQube версии ниже 10.3 необходимо использовать Generic_Issue.
    По умолчанию Generic_Issue_10_3
    """)
    @JsonProperty(defaultValue = "Generic_Issue_10_3")
    GenericIssueFormat genericIssueFormat

    @Override
    @NonCPS
    String toString() {
        return "ResultsTransformOptions{" +
            "transformer=" + transformer +
            "removeSupport=" + removeSupport +
            "supportLevel=" + supportLevel +
            "genericIssueFormat=" + genericIssueFormat +
            '}'
    }
}
