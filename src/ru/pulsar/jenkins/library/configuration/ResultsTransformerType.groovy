package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonProperty

enum ResultsTransformerType {
    @JsonProperty("stebi")
    STEBI,

    @JsonProperty("edt-ripper")
    EDT_RIPPER

}