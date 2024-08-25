package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import org.apache.commons.lang.RandomStringUtils
import ru.pulsar.jenkins.library.IStepExecutor

import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class Yaxunit implements Serializable {

    private final JobConfiguration config

    private final String DEFAULT_YAXUNIT_CONFIGURATION_RESOURCE = 'yaxunit.json'

    public static final String YAXUNIT_ALLURE_STASH = 'yaxunit-allure'
    public static final String COVERAGE_STASH_NAME = 'yaxunit-coverage'
    public static final String COVERAGE_STASH_PATH = 'build/out/yaxunit-coverage.xml'

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

        def srcDir = config.srcDir
        def workspaceDir = FileUtils.getFilePath("$env.WORKSPACE")

        String vrunnerPath = VRunner.getVRunnerPath()
        String ibConnection = ' --ibconnection "/F./build/ib"'

        // Готовим конфиг для yaxunit
        String yaxunitConfigPath = options.configPath
        if (!steps.fileExists(yaxunitConfigPath)) {
            def defaultYaxunitConfig = steps.libraryResource DEFAULT_YAXUNIT_CONFIGURATION_RESOURCE
            steps.writeFile(options.configPath, defaultYaxunitConfig, 'UTF-8')
        }
        def yaxunitConfig = FileUtils.getFilePath(yaxunitConfigPath)

        // Команда запуска тестов
        String runTestsCommand = "$vrunnerPath run --command RunUnitTests=$yaxunitConfig $ibConnection"

        // Переопределяем настройки vrunner
        String vrunnerSettings = options.vrunnerSettings
        if (steps.fileExists(vrunnerSettings)) {
            String vrunnerSettingsParam = " --settings $vrunnerSettings"

            runTestsCommand += vrunnerSettingsParam

        }

        def coverageOpts = config.coverageOptions
        def port = options.dbgsPort
        def lockableResource = RandomStringUtils.random(9, true, false)
        if (options.coverage) {
            lockableResource = "${env.NODE_NAME}_$port"
        }

        steps.lock(null, 1, lockableResource) {
            if (options.coverage) {
                steps.start("${coverageOpts.dbgsPath} --addr=127.0.0.1 --port=$port")
                steps.start("${coverageOpts.coverage41CPath} start -i DefAlias -u http://127.0.0.1:$port -P $workspaceDir -s $srcDir -o $COVERAGE_STASH_PATH")
                steps.cmd("${coverageOpts.coverage41CPath} check -i DefAlias -u http://127.0.0.1:$port")
            }

            // Выполняем команды
            steps.withEnv(logosConfig) {
                VRunner.exec(runTestsCommand, true)
            }

            if (options.coverage) {
                steps.cmd("${coverageOpts.coverage41CPath} stop -i DefAlias -u http://127.0.0.1:$port")
            }
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

        if (options.coverage) {
            steps.stash(COVERAGE_STASH_NAME, COVERAGE_STASH_PATH, true)
        }
    }
}
