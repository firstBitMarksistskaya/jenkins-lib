package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonProperty

enum InitInfoBaseMethod {

    @JsonProperty("fromStorage")
    FROM_STORAGE,

    @JsonProperty("fromSource")
    FROM_SOURCE,

    @JsonProperty("fromDT")
    FROM_DT,

    @JsonProperty("notInit")
    NOT_INIT,

    @JsonProperty("defaultBranchFromStorage")
    DEFAULT_BRANCH_FROM_STORAGE

}
