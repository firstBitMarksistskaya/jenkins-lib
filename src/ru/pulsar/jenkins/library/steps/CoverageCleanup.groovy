package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class CoverageCleanup implements Serializable {

    private final JobConfiguration config

    private String encoding = 'UTF-8'

    CoverageCleanup(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (steps.isUnix()) {
            def command = "pkill Coverage41C ; pkill dbgs"
            steps.sh(command, true, false, encoding)
        } else {
            def command = "taskkill /IM Coverage41C /F & taskkill /IM dbgs /F"
            steps.sh(command, true, false, encoding)
        }
    }
}
