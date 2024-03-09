package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Extension implements Serializable {
    String name
    String src

    @Override
    @NonCPS
    String toString() {
        return name
    }

}
