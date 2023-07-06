package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class Yaxunit implements Serializable {

    private final JobConfiguration config

    private final String yaxunitPath = 'build/yaxunit.cfe'

    private final String DEFAULT_YAXUNIT_CONFIGURATION_RESOURCE = 'yaxunit.json'

    public static final String YAXUNIT_ALLURE_STASH = 'yaxunit-allure'

    Yaxunit(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.yaxunit) {
            Logger.println("Yaxunit test step is disabled")
            return
        }

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            steps.installLocalDependencies()
        }

        def options = config.yaxunitOptions
        def env = steps.env()

        String vrunnerPath = VRunner.getVRunnerPath()
        String ibConnection = ' --ibconnection "/F./build/ib"'

        // Скачиваем расширение
        String pathToYaxunit = "$env.WORKSPACE/$yaxunitPath"
        FilePath localPathToYaxunit = FileUtils.getFilePath(pathToYaxunit)
        Logger.println("Скачивание Yaxunit в $localPathToYaxunit")
        localPathToYaxunit.copyFrom(new URL(options.cfe))

        // Команда загрузки YAXUnit
        String loadYaxunitCommand = vrunnerPath + ' run --command "Путь=' + pathToYaxunit + ';ЗавершитьРаботуСистемы;" --execute '
        String executeParameter = '$runnerRoot/epf/ЗагрузитьРасширениеВРежимеПредприятия.epf'
        if (steps.isUnix()) {
            executeParameter = '\\' + executeParameter
        }
        loadYaxunitCommand += executeParameter
        loadYaxunitCommand += ' --ibconnection "/F./build/ib"'

        // Команда сборки расширений с тестами и их загрузки в ИБ
        def loadTestExtCommands = []
        for (String extension in options.extensionNames) {
            if (extension == "YAXUNIT") {
                continue
            }
            def loadTestExtCommand = "$vrunnerPath compileext ./src/cfe/$extension $extension --updatedb $ibConnection"
            loadTestExtCommands << loadTestExtCommand
            Logger.println("Команда сборки расширения: $loadTestExtCommands")
        }

        String yaxunitConfigPath = options.configPath
        File yaxunitConfigFile = new File("$env.WORKSPACE/$yaxunitConfigPath")
        if (!steps.fileExists(yaxunitConfigPath)) {
            def defaultYaxunitConfig = steps.libraryResource DEFAULT_YAXUNIT_CONFIGURATION_RESOURCE
            yaxunitConfigFile.write defaultYaxunitConfig
        }
        def yaxunitConfig = yaxunitConfigFile.getCanonicalPath()

        // Команда запуска тестов
        String command = "$vrunnerPath run --command RunUnitTests=$yaxunitConfig $ibConnection"

        // Переопределяем настройки vrunner
        String vrunnerSettings = options.vrunnerSettings
        String[] loadTestExtCommandJoined = loadTestExtCommands
        if (steps.fileExists(vrunnerSettings)) {
            String vrunnerSettingsCommand = " --settings $vrunnerSettings"

            loadYaxunitCommand += vrunnerSettingsCommand

            loadTestExtCommandJoined = loadTestExtCommands.collect { "$it $vrunnerSettingsCommand" }
            command += vrunnerSettingsCommand

        }

        // Выполяем команды
        steps.withEnv(logosConfig) {
            VRunner.exec(loadYaxunitCommand, true)
            for (loadTestExtCommand in loadTestExtCommandJoined) {
                VRunner.exec(loadTestExtCommand, true)
            }
            VRunner.exec(command, true)
        }

        // Сохраняем результаты
        String junitReport = "./build/out/yaxunit/junit.xml"
        FilePath pathToJUnitReport = FileUtils.getFilePath("$env.WORKSPACE/$junitReport")
        String junitReportDir = FileUtils.getLocalPath(pathToJUnitReport.getParent())

        if (options.publishToJUnitReport) {
            steps.junit("$junitReportDir/*.xml", true)
            steps.archiveArtifacts("$junitReportDir/**")
        }

        if (options.publishToAllureReport) {
            String allureReport = "./build/out/allure/yaxunit/junit.xml"
            FilePath pathToAllureReport = FileUtils.getFilePath("$env.WORKSPACE/$allureReport")
            String allureReportDir = FileUtils.getLocalPath(pathToAllureReport.getParent())

            pathToJUnitReport.copyTo(pathToAllureReport)

            steps.stash(YAXUNIT_ALLURE_STASH, "$allureReportDir/**", true)
        }
    }
}
