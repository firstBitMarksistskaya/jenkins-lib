package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class Yaxunit implements Serializable, Coverable {

    private final JobConfiguration config

    private static final String DEFAULT_YAXUNIT_CONFIGURATION_RESOURCE = 'yaxunit.json'

    public static final String YAXUNIT_ALLURE_STASH = 'yaxunit-allure'
    public static final String COVERAGE_STASH_NAME = 'yaxunit-coverage'
    public static final String COVERAGE_STASH_PATH = 'build/out/yaxunit-coverage.xml'
    public static final String COVERAGE_PIDS_PATH = 'build/yaxunit-pids'

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

        // Готовим конфиг для yaxunit
        String yaxunitConfigPath = options.configPath
        if (!steps.fileExists(yaxunitConfigPath)) {
            Logger.println("Using default yaxunit config")
            def defaultYaxunitConfig = steps.libraryResource DEFAULT_YAXUNIT_CONFIGURATION_RESOURCE
            steps.writeFile(options.configPath, defaultYaxunitConfig, 'UTF-8')
        }
        def yaxunitConfig = FileUtils.getFilePath("$env.WORKSPACE/$yaxunitConfigPath")

        // Команда запуска тестов
        String runTestsCommand = "$vrunnerPath run --command RunUnitTests=$yaxunitConfig $ibConnection"

        // Переопределяем настройки vrunner
        String vrunnerSettings = options.vrunnerSettings
        if (steps.fileExists(vrunnerSettings)) {
            String vrunnerSettingsParam = " --settings $vrunnerSettings"

            runTestsCommand += vrunnerSettingsParam

        }

        steps.withEnv(logosConfig) {

            steps.withCoverage(config, this, options) {
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
                String allureReport = "./build/out/allure/yaxunit/allure.xml"
                FilePath pathToAllureReport = FileUtils.getFilePath("$env.WORKSPACE/$allureReport")
                String allureReportDir = FileUtils.getLocalPath(pathToAllureReport.getParent())

                pathToJUnitReport.copyTo(pathToAllureReport)

                steps.stash(YAXUNIT_ALLURE_STASH, "$allureReportDir/**", true)
            }

        }
    }

    @Override
    String getStageSlug() {
        return "yaxunit"
    }

    @Override
    String getCoverageStashPath() {
        return COVERAGE_STASH_PATH
    }

    @Override
    String getCoverageStashName() {
        return COVERAGE_STASH_NAME
    }

    @Override
    String getCoveragePidsPath() {
        return COVERAGE_PIDS_PATH
    }
}
