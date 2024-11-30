package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonPropertyDescription

class StepCoverageOptions implements Serializable {

    @JsonPropertyDescription("Выполнять замер покрытия")
    Boolean coverage = false

    @JsonPropertyDescription("Порт, на котором будет запущен сервер отладки для замера покрытия")
    Integer dbgsPort = 1550

}
