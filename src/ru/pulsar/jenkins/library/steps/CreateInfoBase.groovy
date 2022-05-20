package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import org.jenkinsci.plugins.workflow.support.actions.EnvironmentAction
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class CreateInfoBase implements Serializable {

    private final static PRELOAD_DT_LOCAL_PATH = "build/out/preload.dt"

    private final JobConfiguration config

    CreateInfoBase(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        steps.installLocalDependencies()

        steps.createDir('build/out')

        EnvironmentAction env = steps.env()

        String vrunnerPath = VRunner.getVRunnerPath()

        String preloadDTURL = config.initInfoBaseOptions.getPreloadDTURL()
        if (!preloadDTURL.isEmpty()) {

            String vrunnerSettings = config.initInfoBaseOptions.getVrunnerSettings()

            FilePath localPathToPreloadDT = FileUtils.getFilePath("$env.WORKSPACE/$PRELOAD_DT_LOCAL_PATH")
            Logger.println("Скачивание DT в $localPathToPreloadDT")
            localPathToPreloadDT.copyFrom(new URL(preloadDTURL))

            String command = vrunnerPath + " init-dev --dt $localPathToPreloadDT"
            if (steps.fileExists(vrunnerSettings)) {
                command += " --settings $vrunnerSettings"
            }
            VRunner.exec(command)

        }

    }

}
