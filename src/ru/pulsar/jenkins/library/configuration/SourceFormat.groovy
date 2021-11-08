package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonProperty

enum SourceFormat {
    @JsonProperty("edt")
    EDT,

    @JsonProperty("designer")
    DESIGNER

}