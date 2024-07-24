package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonProperty

enum GenericIssueFormat {
    @JsonProperty("Generic_Issue")
    GENERIC_ISSUE,

    @JsonProperty("Generic_Issue_10_3")
    GENERIC_ISSUE_10_3

    @Override
    @NonCPS
    String toString() {
        switch(this) {
            case GENERIC_ISSUE: return "Generic_Issue"
            case GENERIC_ISSUE_10_3: return "Generic_Issue_10_3"
            default: throw new IllegalArgumentException()
        }
    }

}