package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.ExclusiveAccess
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

        Logger.println("Выполнение загрузки конфигурации из файлов")
        String vrunnerPath = VRunner.getVRunnerPath()
        String base = config.baseName()
        def command = "$vrunnerPath update-dev --src $srcDir --ibconnection \"$base\""

        def options = config.initInfoBaseOptions

        String vrunnerSettings = options.vrunnerSettings
        if ((config.templateDBLoaded() || config.basePath) && steps.fileExists(vrunnerSettings)) {
            command += " --settings $vrunnerSettings"
        }

        ExclusiveAccess.run(config) { String uccode ->
            VRunner.exec(command + uccode)
        }
    }
}
