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

        String dbgsPIDS = env.YAXUNIT_DBGS_PIDS
        String coverage41CPIDS = env.YAXUNIT_COVERAGE41C_PIDS

        def combined = dbgsPIDS + " " + coverage41CPIDS

        if (steps.isUnix()) {
            def command = "pkill $combined"
            steps.sh(command, true, false, encoding)
        } else {
            def winCommand = combined.split(" ")
                    .each { it -> "/PID $it" }
                    .join(" ")
            def command = "taskkill $winCommand /F"
            steps.sh(command, true, false, encoding)
        }
    }
}
