package ru.pulsar.jenkins.library.configuration.sonarqube

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

enum GenericIssueFormat {
    @JsonProperty("Generic_Issue")
    GENERIC_ISSUE("Generic_Issue"),

    @JsonProperty("Generic_Issue_10_3")
    GENERIC_ISSUE_10_3("Generic_Issue_10_3")

    private String value

    private GenericIssueFormat(String value) {
        this.value = value
    }


    String getValue() {
        return value
    }

}