package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonProperty

enum InitExtensionMethod {
    @JsonProperty("fromSource")
    SOURCE,

    @JsonProperty("fromFile")
    FILE

}