package ru.pulsar.jenkins.library.utils;

import com.cloudbees.groovy.cps.NonCPS;

import java.io.IOException;
import java.net.ServerSocket;

public class PortPicker {
    @NonCPS
    static int port() throws IOException {
        int result = new ServerSocket(0).getLocalPort();
        return result;
    }
}
