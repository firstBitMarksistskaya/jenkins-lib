package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.InitInfoBaseOptions
import ru.pulsar.jenkins.library.configuration.InitInfoBaseOptions.Extension
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner
import hudson.FilePath
import ru.pulsar.jenkins.library.utils.FileUtils

class LoadExtensions implements Serializable {

    private final JobConfiguration config
    private final String stageName

    private Extension[] extensionsFiltered

    LoadExtensions(JobConfiguration config, String stageName) {
        this.config = config
        this.stageName = stageName
    }

    Extension[] getExtensionsFiltered() {
        return extensionsFiltered
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        def extensions = this.config.initInfoBaseOptions.extensions

        // NB: расширения, подключаемые на этапе initInfoBase, остаются подключенными на всех остальных этапах
        if (this.stageName == "initInfoBase") {
            // подключаются все расширения, у которых явно указано подключение на текущем этапе
            // и те расширения, в которых этапы подключения не указаны вообще
            this.extensionsFiltered = extensions.findAll({ extension ->
                extension.stages.contains(this.stageName) || extension.stages.length == 0
            })
        } else {
            // на остальных этапах подключаются расширения, которые не были подключены на этапе initInfoBase
            // и у которых явно указано подключение на текущем этапе
            this.extensionsFiltered = extensions.findAll({ extension ->
                !extension.stages.contains("initInfoBase") && extension.stages.contains(this.stageName)
            })
        }

        def env = steps.env()
        String cfeDir = "$env.WORKSPACE/$GetExtensions.EXTENSIONS_OUT_DIR"

        String vrunnerPath = VRunner.getVRunnerPath()

        this.extensionsFiltered.each {
            Logger.println("Установим расширение ${it.name}")
            loadExtension(it, vrunnerPath, steps, cfeDir)
        }
    }

    private void loadExtension (Extension extension, String vrunnerPath, IStepExecutor steps, String cfeDir) {

        String pathToExt = "$cfeDir/${extension.name}.cfe"
        FilePath localPathToExt = FileUtils.getFilePath(pathToExt)

        // Команда загрузки расширения
        String loadCommand = vrunnerPath + ' run --command "Путь=' + localPathToExt + ';ЗавершитьРаботуСистемы;" --execute '
        String executeParameter = '$runnerRoot/epf/ЗагрузитьРасширениеВРежимеПредприятия.epf'
        if (steps.isUnix()) {
            executeParameter = '\\' + executeParameter
        }
        loadCommand += executeParameter
        loadCommand += ' --ibconnection "/F./build/ib"'

        String vrunnerSettings = getVrunnerSettingsForStage(this.config, this.stageName)
        if (vrunnerSettings && steps.fileExists(vrunnerSettings)) {
            loadCommand += " --settings $vrunnerSettings"
        }

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            VRunner.exec(loadCommand)
        }
    }

    private static String getVrunnerSettingsForStage(JobConfiguration config, String stageName) {

        if (!stageName) {
            return ""
        }

        String optionsPropertyName = "${stageName}Options"
        def optionsInstance = config."$optionsPropertyName"

        if (!optionsInstance) {
            return ""
        }

        // For InitInfoBaseOptions, return the vrunner settings path only if the database is loaded from an archive
        if (optionsInstance instanceof InitInfoBaseOptions && !config.templateDBLoaded()) {
            return ""
        }

        return optionsInstance.vrunnerSettings
    }
}
