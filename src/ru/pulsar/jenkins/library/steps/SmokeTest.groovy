package ru.pulsar.jenkins.library.steps

import com.cloudbees.groovy.cps.NonCPS
import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.InfobaseCredentials
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.RunnerSettings
import ru.pulsar.jenkins.library.utils.StringJoiner
import ru.pulsar.jenkins.library.utils.VRunner

class SmokeTest implements Serializable, Coverable {

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

        String vrunnerPath = VRunner.getVRunnerPath()
        String base = config.baseName()
        String command = "$vrunnerPath xunit --ibconnection \"$base\""

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
            boolean deleteDir = true
            steps.createDir(junitReportDir, deleteDir)

            String junitReportCommand = "ГенераторОтчетаJUnitXML{$junitReport}"

            reportsConfigConstructor.add(junitReportCommand)
        }

        if (options.publishToAllureReport) {
            boolean deleteDir = true
            steps.createDir(allureReportDir, deleteDir)

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

        steps.withEnv(logosConfig) {

            steps.withCoverage(config, this, options) {
                InfobaseCredentials.withCredentials(config) { boolean credentialsBound ->
                    boolean testClient = credentialsBound && checkTestClient(steps, vrunnerSettings)
                    try {
                        VRunner.exec(command, true)
                    } finally {
                        if (testClient) {
                            InfobaseCredentials.clearTestClientEventLog(config)
                        }
                    }
                }
            }

            if (options.publishToAllureReport) {
                steps.stash(ALLURE_STASH, "$allureReportDir/**", true)
                
                // Архивируем результат в отдельный архив и отправляем в артефакты.
                String archivePath = "build/out/allure/smoke.zip"
                Boolean archiveArtifacts = true
                steps.zip("$allureReportDir", archivePath, '', archiveArtifacts)
            }

            if (options.publishToJUnitReport) {
                steps.junit("$junitReportDir/*.xml", true)
                steps.archiveArtifacts("$junitReportDir/**")
            }
        }
    }

    /**
     * Проверяет тест-клиент xunit под учетными данными ИБ из Jenkins и возвращает, запускается ли он.
     * Тест-клиент получает пользователя и пароль менеджера тестирования, только если в --testclient они
     * не указаны (::порт). Иначе тест-клиент вошел бы в базу с паролем из файла настроек
     */
    static boolean checkTestClient(IStepExecutor steps, String vrunnerSettings) {
        if (!steps.fileExists(vrunnerSettings)) {
            return false
        }

        String settingsPath = FileUtils.getAbsolutePath(vrunnerSettings, steps.env())
        String testClient = RunnerSettings.value(settingsPath, 'xunit', '--testclient')
        if (!testClient) {
            return false
        }
        if (!testClient.startsWith('::')) {
            steps.error("В настройках vanessa-runner $vrunnerSettings в --testclient указаны пользователь или пароль " +
                "тест-клиента. С учетными данными ИБ из Jenkins укажите --testclient в виде ::порт: " +
                "тест-клиент получит пользователя и пароль менеджера тестирования")
        }

        String problem = InfobaseCredentials.testClientPasswordProblem(steps.env(), steps.isUnix()) ?:
            addTestClientProblem(steps.env())
        if (problem) {
            steps.error("Тест-клиент дымовых тестов: $problem")
        }
        return true
    }

    // ADD разбирает строку /C по ";", а параметры тест-клиента "пользователь:пароль:порт" по ":"
    @NonCPS
    private static String addTestClientProblem(def env) {
        String user = InfobaseCredentials.envValue(env, InfobaseCredentials.USER_VARIABLE) ?: ''
        String password = InfobaseCredentials.envValue(env, InfobaseCredentials.PASSWORD_VARIABLE) ?: ''
        if ((user + password) =~ /[:;]/) {
            return 'имя пользователя и пароль ИБ не должны содержать символы : и ;: Vanessa ADD получает ' +
                'тест-клиент в строке /C в виде пользователь:пароль:порт'
        }
        return null
    }

    @Override
    String getStageSlug() {
        return "smoke"
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
