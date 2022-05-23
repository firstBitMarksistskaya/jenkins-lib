package ru.pulsar.jenkins.library.utils

import ru.pulsar.jenkins.library.configuration.JobConfiguration

final class EDT {

    static String ringModule(JobConfiguration config) {
        return config.edtAgentLabel()
    }

}

