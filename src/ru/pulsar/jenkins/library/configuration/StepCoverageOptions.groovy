package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

class StepCoverageOptions implements Serializable {

    @JsonPropertyDescription("Выполнять замер покрытия")
    @JsonProperty(defaultValue = "false")
    Boolean coverage = false

    @JsonPropertyDescription("Порт, на котором будет запущен сервер отладки для замера покрытия")
    @JsonProperty(defaultValue = "1550")
    Integer dbgsPort = 1550

}
