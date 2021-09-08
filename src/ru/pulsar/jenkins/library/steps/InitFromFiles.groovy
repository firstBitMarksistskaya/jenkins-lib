package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class InitFromFiles implements Serializable {

    private final JobConfiguration config;

    InitFromFiles(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.infobaseFromFiles()) {
            Logger.println("init infoBase from files is disabled")
            return
        }

        Logger.println("Распаковка файлов")
        
        def env = steps.env();

        def srcDir = "$env.WORKSPACE/$EdtToDesignerFormatTransformation.CONFIGURATION_DIR"

        steps.unstash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH)
        steps.unzip(srcDir, EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)

        Logger.println("Выполнение загрузки конфигурации из файлов")
        def initCommand = "oscript_modules/bin/vrunner init-dev --src $srcDir --ibconnection \"/F./build/ib\""
        steps.cmd(initCommand)
    }
}
