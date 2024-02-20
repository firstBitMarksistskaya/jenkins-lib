package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonProperty

enum InitExtentionMethod {
    @JsonProperty("fromSource")
    SOURCE,

    @JsonProperty("fromInternet")
    INTERNET

}