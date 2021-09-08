package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
public enum SourceFormat{
    @JsonProperty("edt")
    EDT,

    @JsonProperty("designer")
    DESIGNER

    @Override
    @NonCPS
    String toString() {
        return "SourceFormat{" +
                "edt='" + EDT +
                "designer='" + DESIGNER +
                '}';
    }
    boolean infobaseFromFiles(){
        return EDT
    }

}