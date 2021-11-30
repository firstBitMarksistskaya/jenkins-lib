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

        if (!config.infobaseFromFiles()) {
            Logger.println("init infoBase from files is disabled")
            return
        }

        steps.installLocalDependencies();

        Logger.println("Распаковка файлов")
        def extPrefix = "$EdtToDesignerFormatTransformation.EXT_PATH_PREFIX"
        def extSuffix = "$EdtToDesignerFormatTransformation.EXT_PATH_SUFFIX"
        def configurationZipStash = "$EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH"
        def configurationZip = "$EdtToDesignerFormatTransformation.CONFIGURATION_ZIP"        
        String srcDir
                 
        if (config.sourceFormat == SourceFormat.EDT) {
            String saveExtDir
            String configurationExtZipStash
            String configurationExtZip
            
            def env = steps.env();
            srcDir = "$env.WORKSPACE/$EdtToDesignerFormatTransformation.CONFIGURATION_DIR"
            steps.unstash(configurationZipStash)
            steps.unzip(srcDir, configurationZip)
            
            if (config.srcExtDir.length != 0) {
                config.srcExtDir.each {
                    saveExtDir = srcDir.replace(extPrefix,"$extPrefix/$extSuffix${it}") 
                    configurationExtZipStash = "ext_${it}_$configurationZipStash"
                    configurationExtZip = configurationZip.replace(extPrefix,"$extPrefix/$extSuffix${it}")
                    
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
        if (config.srcExtDir.length != 0) {
                config.srcExtDir.each {
                    if (config.sourceFormat == SourceFormat.EDT) {
                        inputExtDir = srcDir.replace(extPrefix,"$extPrefix/$extSuffix${it}")                        
                    }else{
                        inputExtDir = "${it}"
                    }
                    Logger.println("Загрузка расширения ${it} в ИБ")
                    VRunner.exec("$vrunnerPath compileext --inputpath \"$inputExtDir\" --ibconnection \"/F./build/ib\"")
                }
            }

    }
}
