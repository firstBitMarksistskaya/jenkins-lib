package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class ResultsTransformOptions implements Serializable {

    @JsonPropertyDescription("""Способ преобразования замечаний.
    Поддерживается stebi и edt-ripper
    По умолчанию содержит значение "stebi".
    """)
    ResultsTransformerType transformer = ResultsTransformerType.STEBI

    @JsonPropertyDescription("Фильтровать замечания по уровню поддержки модуля. Только для stebi. По умолчанию включено.")
    Boolean removeSupport = true

    @JsonPropertyDescription("""Настройка фильтрации замечаний по уровню поддержки. Только для stebi.
        0 - удалить файлы на замке;
        1 - удалить файлы на замке и на поддержке;
        2 - удалить файлы на замке, на поддержке и снятые с поддержки.
    """)
    Integer supportLevel

    @JsonPropertyDescription("""Формат отчета generic issue. Только для stebi.
    Для SonarQube 10.3+ необходимо использовать Generic_Issue_10_3.
    По умолчанию Generic_Issue
    """)
    GenericIssueFormat genericIssueFormat = GenericIssueFormat.GENERIC_ISSUE

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
