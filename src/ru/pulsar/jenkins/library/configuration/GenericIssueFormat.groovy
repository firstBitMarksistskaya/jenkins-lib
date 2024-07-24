package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

enum GenericIssueFormat {
    @JsonProperty("Generic_Issue")
    GENERIC_ISSUE,

    @JsonProperty("Generic_Issue_10_3")
    GENERIC_ISSUE_10_3

    @JsonValue
    String toValue() {
        if (this == GENERIC_ISSUE) {
            return "Generic_Issue"
        } else {
            return "Generic_Issue_10_3"
        }
    }

}