package ru.pulsar.jenkins.library.steps

import hudson.model.Result
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.ArchiveInfobaseOptions
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.ExclusiveAccess
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.InfobaseConnection
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class ZipInfobase implements Serializable {

    private final JobConfiguration config
    private final String stage

    ZipInfobase(JobConfiguration config, String stage) {
        this.config = config
        this.stage = stage
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        def currentBuild = steps.currentBuild()
        def currentResult = Result.fromString(currentBuild.getCurrentResult())

        def archiveInfobaseOptions = getArchiveInfobaseOptionsForStage(config, stage)

        String archiveName = savedBaseFile(stage, '1Cv8.1CD', 'zip')

        // опция отвечает только за то, будет ли файл сохранен в виде артефакта
        def archiveInfobase = false
        if (archiveInfobaseOptions.onAlways
                || (archiveInfobaseOptions.onFailure && (currentResult == Result.FAILURE || currentResult == Result.ABORTED))
                || (archiveInfobaseOptions.onUnstable && currentResult == Result.UNSTABLE)
                || (archiveInfobaseOptions.onSuccess && currentResult == Result.SUCCESS)) {
            archiveInfobase = true
        }

        // Внешнюю базу тестовые стадии берут по адресу, поэтому она сохраняется только как артефакт
        if (config.basePath) {
            if (!archiveInfobase) {
                Logger.println("Сохранение базы ${config.baseName()} при текущем результате сборки $currentResult не включено")
                return
            }
            if (InfobaseConnection.isServer(config.baseName())) {
                dumpServerBase(steps, savedBaseFile(stage, '1Cv8', 'dt'))
            } else {
                zipExternalFileBase(steps, archiveName)
            }
            return
        }

        if (steps.fileExists(archiveName)) {
            steps.fileOperations([steps.fileDeleteOperation(archiveName)])
        }
        steps.zip('build/ib', archiveName, '1Cv8.1CD', archiveInfobase)
        steps.stash(archiveName, archiveName, false)
    }

    // Имя файла сохраненной базы: после инициализации без имени стадии, после стадии - с ним
    private static String savedBaseFile(String stage, String baseName, String extension) {
        return stage == 'initInfoBase' ? "${baseName}.${extension}" : "${baseName}.${stage}.${extension}"
    }

    // У серверной базы нет файла, который можно упаковать: она выгружается в dt. Выгрузке нужен
    // монопольный доступ, а в базу конфигуратор входит под пользователем из файла настроек этапа инициализации
    private void dumpServerBase(IStepExecutor steps, String dtPath) {
        String command = "${VRunner.getVRunnerPath()} dump $dtPath --ibconnection \"${config.baseName()}\""
        String vrunnerSettings = config.initInfoBaseOptions.vrunnerSettings
        if (steps.fileExists(vrunnerSettings)) {
            command += " --settings $vrunnerSettings"
        }

        if (steps.fileExists(dtPath)) {
            steps.fileOperations([steps.fileDeleteOperation(dtPath)])
        }
        Logger.println("Выгрузка базы ${config.baseName()} в $dtPath")
        ExclusiveAccess.run(config) { String uccode ->
            VRunner.exec(command + uccode)
        }
        steps.archiveArtifacts(dtPath)
    }

    // Файл внешней файловой базы упаковывается из ее каталога. Stash не нужен: тестовые стадии работают с базой
    // по адресу, а большой файл зря поехал бы на контроллер
    private void zipExternalFileBase(IStepExecutor steps, String archiveName) {
        def env = steps.env()
        // Относительный путь vanessa-runner отсчитывает от корня проекта, то есть от рабочего каталога сборки
        String baseDir = FileUtils.getAbsolutePath(InfobaseConnection.fileBaseDir(config.baseName()), env)

        Logger.println("Упаковка файла базы ${config.baseName()} в $archiveName")
        steps.zip(baseDir, archiveName, '1Cv8.1CD', true)
    }

    private static ArchiveInfobaseOptions getArchiveInfobaseOptionsForStage(JobConfiguration config, String stageName) {

        def defaultOptions = new ArchiveInfobaseOptions()
        if (!stageName) {
            return defaultOptions
        }

        try {
            return config."${stageName}Options".archiveInfobase
        } catch(MissingPropertyException | NullPointerException e) {
            Logger.println("Ошибка при получении настроек архивации для этапа ${stageName}: ${e.message}")
            Logger.println(e.toString())
            return defaultOptions
        }
    }
}
