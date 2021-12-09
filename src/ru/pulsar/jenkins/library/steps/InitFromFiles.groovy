package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class InitFromFiles implements Serializable {

    private final JobConfiguration config
    final static PRELOAD_DT_LOCAL_PATH = "build/out/preload.dt"

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

        String preloadDTURL = config.initInfobaseOptions.getPreloadDTURL()
        if (!preloadDTURL.isEmpty()) {

            FilePath localPathToPreloadDT = FileUtils.getFilePath("$env.WORKSPACE/$PRELOAD_DT_LOCAL_PATH")
            Logger.println("Скачивание DT в $localPathToPreloadDT")
            localPathToPreloadDT.copyFrom(new URL("$preloadDTURL"))

            Logger.println("Загрузка DT")

            String command = vrunnerPath + " init-dev --dt $localPathToPreloadDT"
            String vrunnerSettings = config.initInfobaseOptions.vrunnerSettings
            if (steps.fileExists(vrunnerSettings)) {
                command += " --settings $vrunnerSettings"
            }
            VRunner.exec(command)

            command = vrunnerPath + " update-dev --src $srcDir --ibconnection \"/F./build/ib\""
            if (steps.fileExists(vrunnerSettings)) {
                command += " --settings $vrunnerSettings"
            }
            VRunner.exec(command)

            } else {
        Logger.println("Выполнение загрузки конфигурации из файлов")
        def initCommand = "$vrunnerPath init-dev --src $srcDir --ibconnection \"/F./build/ib\""
        VRunner.exec(initCommand)
            }
    }
}
