package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.InitInfoBaseOptions
import ru.pulsar.jenkins.library.configuration.InitInfoBaseOptions.Extension
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.ExclusiveAccess
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

        if (this.extensionsFiltered.length == 0) {
            Logger.println("Расширений для подключения нет")
            return
        }

        def env = steps.env()
        String cfeDir = "$env.WORKSPACE/$GetExtensions.EXTENSIONS_OUT_DIR"

        String vrunnerPath = VRunner.getVRunnerPath()
        String settingsParameter = vrunnerSettingsParameter(this.config, this.stageName)

        // Подключение расширения с собственными объектами меняет структуру базы и требует монопольного доступа
        ExclusiveAccess.run(config) { String uccode ->
            for (Extension extension : this.extensionsFiltered) {
                Logger.println("Установим расширение ${extension.name}")
                loadExtension(extension, vrunnerPath, steps, cfeDir, settingsParameter + uccode)
            }
        }
    }

    private void loadExtension (Extension extension, String vrunnerPath, IStepExecutor steps, String cfeDir, String parameters) {

        String pathToExt = "$cfeDir/${extension.name}.cfe"
        FilePath localPathToExt = FileUtils.getFilePath(pathToExt)

        // Команда загрузки расширения
        String loadCommand = vrunnerPath + ' run --command "Путь=' + localPathToExt + ';ЗавершитьРаботуСистемы;" --execute '
        String executeParameter = '$runnerRoot/epf/ЗагрузитьРасширениеВРежимеПредприятия.epf'
        if (steps.isUnix()) {
            executeParameter = '\\' + executeParameter
        }
        loadCommand += executeParameter
        loadCommand += " --ibconnection \"${config.baseName()}\""
        loadCommand += parameters

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            VRunner.exec(loadCommand)
        }
    }

    // Параметр --settings с файлом настроек vrunner стадии или пустая строка
    static String vrunnerSettingsParameter(JobConfiguration config, String stageName) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String vrunnerSettings = getVrunnerSettingsForStage(config, stageName)
        if (vrunnerSettings && steps.fileExists(vrunnerSettings)) {
            return " --settings $vrunnerSettings"
        }
        return ''
    }

    private static String getVrunnerSettingsForStage(JobConfiguration config, String stageName) {

        if (!stageName) {
            return ""
        }

        // Настройки дымовых тестов лежат в поле smokeTestOptions
        String optionsPropertyName = stageName == 'smoke' ? 'smokeTestOptions' : "${stageName}Options"
        def optionsInstance = config."$optionsPropertyName"

        if (!optionsInstance) {
            return ""
        }

        // For InitInfoBaseOptions, return the vrunner settings path only if the database is loaded from an archive
        // or an existing database is used: both have users, unlike a newly created empty database
        if (optionsInstance instanceof InitInfoBaseOptions && !config.templateDBLoaded() && !config.basePath) {
            return ""
        }

        return optionsInstance.vrunnerSettings
    }
}
