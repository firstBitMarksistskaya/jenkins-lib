package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonProperty

enum GenericIssueFormat {
    @JsonProperty("Generic_Issue")
    GENERIC_ISSUE,

    @JsonProperty("Generic_Issue_10_3")
    GENERIC_ISSUE_10_3

}