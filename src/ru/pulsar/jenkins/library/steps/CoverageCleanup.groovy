package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class CoverageCleanup implements Serializable {

    private final JobConfiguration config
    private final String stageName

    private String encoding = 'UTF-8'

    CoverageCleanup(JobConfiguration config, String stageName = "") {
        this.config = config
        this.stageName = stageName
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        String pidsFilePath = "build/$stageName-pids"

        def pids = ""
        if (steps.fileExists(pidsFilePath)) {
            pids = steps.readFile(pidsFilePath)
        }

        if (pids.isEmpty()) {
            Logger.println("Нет запущенных процессов dbgs и Coverage41C")
            return
        }

        Logger.println("Завершение процессов dbgs и Coverage41C с pid: $pids")

        if (steps.isUnix()) {
            def command = "kill $pids"
            steps.sh(command, true, false, encoding)
        } else {
            def winCommand = pids.split(" ")
                    .each { it -> "/PID $it" }
                    .join(" ")
            def command = "taskkill $winCommand /F"
            steps.sh(command, true, false, encoding)
        }
    }
}
