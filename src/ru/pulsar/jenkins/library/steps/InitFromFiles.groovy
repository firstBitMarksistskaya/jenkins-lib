package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class InitFromFiles implements Serializable {

    private final JobConfiguration config;

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

        steps.installLocalDependencies();

        Logger.println("Распаковка файлов")
        def configurationZipStash = "$EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH"
        def configurationZip = "$EdtToDesignerFormatTransformation.CONFIGURATION_ZIP"        
        String srcDir
        String [] srcExtDir = config.srcExtDir.split(" ")

        if (config.sourceFormat == SourceFormat.EDT) {           
            def env = steps.env();
            srcDir = "$env.WORKSPACE/$EdtToDesignerFormatTransformation.CONFIGURATION_DIR"
            steps.unstash(configurationZipStash)
            steps.unzip(srcDir, configurationZip)
            
            if (srcExtDir.length != 0) {
                srcExtDir.each {
                    String saveExtDir = "$env.WORKSPACE/$EdtToDesignerFormatTransformation.EXT_PATH_PREFIX-${it}/${it}-cfg"
                    String configurationExtZipStash = "$EdtToDesignerFormatTransformation.EXT_PATH_PREFIX-${it}_$CONFIGURATION_ZIP_STASH"
                    String configurationExtZip = "$EdtToDesignerFormatTransformation.EXT_PATH_PREFIX-${it}/${it}-cfg.zip"
                    steps.unstash(configurationExtZipStash)
                    steps.unzip(saveExtDir, configurationExtZip)
                }
            }
        } else {
            srcDir = config.srcDir;
        }

        Logger.println("Выполнение загрузки конфигурации из файлов")
        String vrunnerPath = VRunner.getVRunnerPath();
        def initCommand = "$vrunnerPath init-dev --src $srcDir --ibconnection \"/F./build/ib\""
        VRunner.exec(initCommand)
        String inputExtDir
        if (srcExtDir.length != 0) {
            srcExtDir.each {
                if (config.sourceFormat == SourceFormat.EDT) {
                    inputExtDir = "$env.WORKSPACE/$EdtToDesignerFormatTransformation.EXT_PATH_PREFIX-${it}/${it}-cfg"                       
                }else{
                    inputExtDir = "${it}"
                }
                Logger.println("Загрузка расширения ${it} в ИБ")
                VRunner.exec("$vrunnerPath compileext \"$inputExtDir\" ${it} --ibconnection \"/F./build/ib\"")
            }
        }

    }
}
