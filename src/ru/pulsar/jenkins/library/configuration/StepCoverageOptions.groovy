package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonPropertyDescription

class StepCoverageOptions {

    @JsonPropertyDescription("Выполнять замер покрытия")
    Boolean coverage = false

    @JsonPropertyDescription("Порт, на котором будет запущен сервер отладки для замера покрытия")
    int dbgsPort = 1550

}
