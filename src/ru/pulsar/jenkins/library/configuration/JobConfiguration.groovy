package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@JsonIgnoreProperties(ignoreUnknown = true)
class JobConfiguration implements Serializable {
    @JsonPropertyDescription("Версия платформы 1С:Предприятие в формате 8.3.хх.хххх.")
    String v8version

    @JsonPropertyDescription("Имя настроенной утилиты sonar-scanner.")
    String sonarScannerToolName

    @JsonPropertyDescription("Идентификаторы сохраненных секретов")
    Secrets secrets;

    @Override
    String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE)
    }
}