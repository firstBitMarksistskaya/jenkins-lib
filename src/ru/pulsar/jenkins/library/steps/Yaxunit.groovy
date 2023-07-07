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

    private final String yaxunitPath = 'build/out/yaxunit.cfe'

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

        // Скачиваем YAXUnit
        String pathToYaxunit = "$env.WORKSPACE/$yaxunitPath"
        FilePath localPathToYaxunit = FileUtils.getFilePath(pathToYaxunit)
        Logger.println("Скачивание YAXUnit в $localPathToYaxunit из ${options.cfe}")
        localPathToYaxunit.copyFrom(new URL(options.cfe))

        def extCommands = []

        // Команда загрузки YAXUnit
        def loadYaxunitCommand = VRunner.loadExtCommand("yaxunit")
        extCommands << loadYaxunitCommand


        // Команды сборки расширений с тестами и их загрузки в ИБ
        for (String extension in options.extensionNames) {
            if (extension.toUpperCase() == "YAXUNIT") {
                continue
            }
            
            // Команда компиляции в cfe
            def compileExtCommand = "$vrunnerPath compileexttocfe --src ./src/cfe/$extension --out build/out/${extension}.cfe"
            extCommands << compileExtCommand
            Logger.println("Команда сборки расширения: $compileExtCommand")

            // Команда загрузки расширения  в ИБ
            def loadTestExtCommand = VRunner.loadExtCommand(extension)
            extCommands << loadTestExtCommand
            Logger.println("Команда загрузки расширения: $loadTestExtCommand")
            
        }

        // Готовим конфиг для yaxunit
        String yaxunitConfigPath = options.configPath
        File yaxunitConfigFile = new File("$env.WORKSPACE/$yaxunitConfigPath")
        if (!steps.fileExists(yaxunitConfigPath)) {
            def defaultYaxunitConfig = steps.libraryResource DEFAULT_YAXUNIT_CONFIGURATION_RESOURCE
            yaxunitConfigFile.write defaultYaxunitConfig
        }
        def yaxunitConfig = yaxunitConfigFile.getCanonicalPath()

        // Команда запуска тестов
        String runTestsCommand = "$vrunnerPath run --command RunUnitTests=$yaxunitConfig $ibConnection"

        // Переопределяем настройки vrunner
        String vrunnerSettings = options.vrunnerSettings
        String[] extCommandsWithSettings = extCommands
        if (steps.fileExists(vrunnerSettings)) {
            String vrunnerSettingsParam = " --settings $vrunnerSettings"

            extCommandsWithSettings = extCommands.collect { "$it $vrunnerSettingsParam" }
            runTestsCommand += vrunnerSettingsParam

        }

        // Выполяем команды
        steps.withEnv(logosConfig) {
            for (extCommand in extCommandsWithSettings) {
                VRunner.exec(extCommand, true)
            }
            VRunner.exec(runTestsCommand, true)
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
