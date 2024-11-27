package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import org.apache.commons.lang.RandomStringUtils
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.CoverageUtils
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.StringJoiner
import ru.pulsar.jenkins.library.utils.VRunner

class SmokeTest implements Serializable {

    public static final String ALLURE_STASH = 'smoke-allure'
    public static final String COVERAGE_STASH_NAME = 'smoke-coverage'
    public static final String COVERAGE_STASH_PATH = 'build/out/smoke-coverage.xml'
    public static final String COVERAGE_PIDS_PATH = 'build/smoke-pids'

    private final JobConfiguration config

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

        def srcDir = config.srcDir
        def workspaceDir = FileUtils.getFilePath("$env.WORKSPACE")

        String vrunnerPath = VRunner.getVRunnerPath()
        String command = "$vrunnerPath xunit --ibconnection \"/F./build/ib\""

        String vrunnerSettings = options.vrunnerSettings
        if (steps.fileExists(vrunnerSettings)) {
            command += " --settings $vrunnerSettings"
        }

        String xddTestRunnerPath = "./oscript_modules/add/xddTestRunner.epf"
        if (steps.fileExists(xddTestRunnerPath)) {
            command += " --pathxunit $xddTestRunnerPath"
        }

        if (steps.fileExists(options.xddConfigPath)) {
            command += " --xddConfig $options.xddConfigPath"
        }

        String junitReport = "build/out/jUnit/smoke/smoke.xml"
        FilePath pathToJUnitReport = FileUtils.getFilePath("$env.WORKSPACE/$junitReport")
        String junitReportDir = FileUtils.getLocalPath(pathToJUnitReport.getParent())

        String allureReport = "build/out/allure/smoke/allure.xml"
        FilePath pathToAllureReport = FileUtils.getFilePath("$env.WORKSPACE/$allureReport")
        String allureReportDir = FileUtils.getLocalPath(pathToAllureReport.getParent())

        StringJoiner reportsConfigConstructor = new StringJoiner(";")

        if (options.publishToJUnitReport) {
            steps.createDir(junitReportDir)

            String junitReportCommand = "ГенераторОтчетаJUnitXML{$junitReport}"

            reportsConfigConstructor.add(junitReportCommand)
        }

        if (options.publishToAllureReport) {
            steps.createDir(allureReportDir)

            String allureReportCommand = "ГенераторОтчетаAllureXMLВерсия2{$allureReport}"

            reportsConfigConstructor.add(allureReportCommand)
        }

        if (reportsConfigConstructor.length() > 0) {
            String reportsConfig = reportsConfigConstructor.toString()
            command += " --reportsxunit \"$reportsConfig\""
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

        def coverageOpts = config.coverageOptions
        def port = options.dbgsPort
        def currentDbgsPids = CoverageUtils.getPIDs("dbgs")
        def currentCoverage41CPids = CoverageUtils.getPIDs("Coverage41C")
        def lockableResource = RandomStringUtils.random(9, true, false)
        if (options.coverage) {
            lockableResource = "${env.NODE_NAME}_$port"
        }

        steps.lock(null, 1, lockableResource) {
            if (options.coverage) {
                steps.start("${coverageOpts.dbgsPath} --addr=127.0.0.1 --port=$port")
                steps.start("${coverageOpts.coverage41CPath} start -i DefAlias -u http://127.0.0.1:$port -P $workspaceDir -s $srcDir -o $COVERAGE_STASH_PATH")
                steps.cmd("${coverageOpts.coverage41CPath} check -i DefAlias -u http://127.0.0.1:$port")

                def newDbgsPids = CoverageUtils.getPIDs("dbgs")
                def newCoverage41CPids = CoverageUtils.getPIDs("Coverage41C")

                newDbgsPids.removeAll(currentDbgsPids)
                newCoverage41CPids.removeAll(currentCoverage41CPids)

                newDbgsPids.addAll(newCoverage41CPids)
                def pids = newDbgsPids.join(" ")

                steps.writeFile(COVERAGE_PIDS_PATH, pids, 'UTF-8')

                Logger.println("Coverage PIDs for cleanup: $pids")

            }

            steps.withEnv(logosConfig) {
                VRunner.exec(command, true)
            }

            if (options.coverage) {
                steps.cmd("${coverageOpts.coverage41CPath} stop -i DefAlias -u http://127.0.0.1:$port")
            }
        }

        if (options.publishToAllureReport) {
            steps.stash(ALLURE_STASH, "$allureReportDir/**", true)
            steps.archiveArtifacts("$allureReportDir/**")
        }

        if (options.publishToJUnitReport) {
            steps.junit("$junitReportDir/*.xml", true)
            steps.archiveArtifacts("$junitReportDir/**")
        }

        if (options.coverage) {
            steps.stash(COVERAGE_STASH_NAME, COVERAGE_STASH_PATH, true)
        }

    }
}
