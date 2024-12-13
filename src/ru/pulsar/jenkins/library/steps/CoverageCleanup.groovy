package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class CoverageCleanup implements Serializable {

    private final JobConfiguration config
    private final String stageName

    CoverageCleanup(JobConfiguration config, String stageName = "") {
        this.config = config
        this.stageName = stageName
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        String pidsFilePath = "build${File.separator}${stageName}-pids"

        def pids = ""
        if (steps.fileExists(pidsFilePath)) {
            pids = steps.readFile(pidsFilePath)
        }

        if (pids.isEmpty()) {
            Logger.println("Нет запущенных процессов dbgs и Coverage41C")
            return
        }

        Logger.println("Завершение процессов dbgs и Coverage41C с pid: $pids")
        def command
        if (steps.isUnix()) {
            command = "kill $pids"
        } else {
            def pidsForCmd = ''
            def pidsArray = pids.split(" ")

            pidsArray.each {
                pidsForCmd += "/PID $it"
            }
            pidsForCmd = pidsForCmd.trim()

            command = "taskkill $pidsForCmd /F"
        }
        steps.cmd(command, true, false)
    }
}
