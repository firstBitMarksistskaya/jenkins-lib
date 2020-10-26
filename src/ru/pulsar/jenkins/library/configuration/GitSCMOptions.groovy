package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class GitSCMOptions implements Serializable {

    @JsonPropertyDescription("Дополнительно выполнить git lfs pull")
    boolean lfsPull

    String lfsURI = ""

    @Override
    @NonCPS
    String toString() {
        return "GitSCMOptions{" +
            "lfsPull=" + lfsPull +
            "lfsURI=" + lfsURI +
            '}';
    }
}
