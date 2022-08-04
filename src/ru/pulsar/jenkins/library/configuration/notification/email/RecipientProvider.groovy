package ru.pulsar.jenkins.library.configuration.notification.email

import com.fasterxml.jackson.annotation.JsonProperty

enum RecipientProvider {

    @JsonProperty("developers")
    DEVELOPERS,
    @JsonProperty("requestor")
    REQUESTOR,
    @JsonProperty("brokenBuildSuspects")
    BROKEN_BUILD_SUSPECTS,
    @JsonProperty("brokenTestsSuspects")
    BROKEN_TESTS_SUSPECTS
}