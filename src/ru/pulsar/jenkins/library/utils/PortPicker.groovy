package ru.pulsar.jenkins.library.utils

import com.cloudbees.groovy.cps.NonCPS

class PortPicker {

    @NonCPS
    static int getPort() {
        return new ServerSocket(0).getLocalPort()
    }
}
