package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class InitFromFiles implements Serializable {

    private final JobConfiguration config

    InitFromFiles(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.infoBaseFromFiles()) {
            Logger.println("init infoBase from files is disabled")
            return
        }

        steps.installLocalDependencies()

        steps.createDir('build/out')

        Logger.println("Распаковка файлов")

        String srcDir

        if (config.sourceFormat == SourceFormat.EDT) {
            def env = steps.env()
            srcDir = "$env.WORKSPACE/$EdtToDesignerFormatTransformation.CONFIGURATION_DIR"

            steps.unstash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH)
            steps.unzip(srcDir, EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
        } else {
            srcDir = config.srcDir
        }

        String vrunnerPath = VRunner.getVRunnerPath()
        String vrunnerSettings = config.initInfoBaseOptions.getVrunnerSettings()

        String command = vrunnerPath + " update-dev --src $srcDir --ibconnection \"/F./build/ib\""
        if (steps.fileExists(vrunnerSettings)) {
                command += " --settings $vrunnerSettings"
        }

        VRunner.exec(command)

    }
}
