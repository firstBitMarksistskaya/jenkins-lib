package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class SmokeTest implements Serializable {

    public static final String SMOKE_ALLURE_STASH = 'smoke-allure'

    private final JobConfiguration config;

    SmokeTest(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.smoke) {
            Logger.println("Smoke test step is disabled")
            return
        }

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            steps.installLocalDependencies()
        }

        def options = config.smokeTestOptions
        def env = steps.env()

        String vrunnerPath = VRunner.getVRunnerPath()
        String command = "$vrunnerPath xunit --ibconnection \"/F./build/ib\""

        String vrunnerSettings = options.vrunnerSettings
        if (steps.fileExists(vrunnerSettings)) {
            command += " --settings $vrunnerSettings";
        }

        String xddTestRunnerPath = "./oscript_modules/add/xddTestRunner.epf"
        if (steps.fileExists(xddTestRunnerPath)) {
            command += " --pathxunit $xddTestRunnerPath"
        }

        String xddConfigPath = "./tools/xUnitParams.json"
        if (steps.fileExists(xddConfigPath)) {
            command += " --xddConfig $xddConfigPath"
        }

        if (options.publishToAllureReport || options.publishToJUnitReport) {
            
            command += " --reportsxunit \"%REPORTS%\""

            String allureReportCommand = ""
            if (options.publishToAllureReport) {

                String allureReport = "build/out/allure/smoke/allure.xml"
                FilePath pathToAllureReport = FileUtils.getFilePath("$env.WORKSPACE/$allureReport")
                String allureReportDir = FileUtils.getLocalPath(pathToAllureReport.getParent())

                steps.createDir(allureReportDir)

                allureReportCommand = "ГенераторОтчетаAllureXMLВерсия2{$allureReport}"

            }

            String junitReportCommand = ""
            if (options.publishToJUnitReport) {

                String junitReport = "build/out/jUnit/smoke/smoke.xml"
                FilePath pathToJUnitReport = FileUtils.getFilePath("$env.WORKSPACE/$junitReport")
                String junitReportDir = FileUtils.getLocalPath(pathToJUnitReport.getParent())

                steps.createDir(junitReportDir)

                junitReportCommand = "ГенераторОтчетаJUnitXML{$junitReport}"
            }
            
            def commandsList = [allureReportCommand, junitReportCommand]
            commandsList.removeAll([""])
            def reportsCommand = commandsList.join(";")
            command.replace("%REPORTS%", reportsCommand)
        }
        if (steps.isUnix()) {
            command = command.replace(';', '\\;')
        }

        if (!VRunner.configContainsSetting(vrunnerSettings, "testsPath")) {
            String testsPath = "oscript_modules/add/tests/smoke"
            if (!steps.fileExists(testsPath)) {
                testsPath = '$addRoot/tests/smoke'
                if (steps.isUnix()) {
                    testsPath = '\\' + testsPath
                }
            }
            command += " $testsPath"
        }

        steps.withEnv(logosConfig) {
            VRunner.exec(command)
        }

        steps.stash(SMOKE_ALLURE_STASH, "$allureReportDir/**", true)

        if (options.publishToJUnitReport) {
            steps.junit("$junitReportDir/*.xml", true)
        }

        steps.archiveArtifacts("$junitReportDir/**")
        steps.archiveArtifacts("$allureReportDir/**")
    }
}
