package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonProperty

enum InitInfobaseMethod {

    @JsonProperty("fromStorage")
    FROM_STORAGE,

    @JsonProperty("fromSource")
    FROM_SOURCE,

    @JsonProperty("defaultBranchFromStorage")
    DEFAULT_BRANCH_FROM_STORAGE

}
