package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.configuration.JobConfiguration

class GitSCMExtension implements Serializable {

    private final JobConfiguration config;

    GitSCMExtension(JobConfiguration config) {
        this.config = config
    }

    def run() {

    }
}
