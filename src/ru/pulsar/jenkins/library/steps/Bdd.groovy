package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.CoverageUtils
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class Bdd implements Serializable, Coverable {

    private final JobConfiguration config

    public static final String ALLURE_STASH = 'bdd-allure'
    public static final String COVERAGE_STASH_NAME = 'bdd-coverage'
    public static final String COVERAGE_STASH_PATH = 'build/out/bdd-coverage.xml'
    public static final String COVERAGE_PIDS_PATH = 'build/bdd-pids'

    Bdd(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.bdd) {
            Logger.println("BDD step is disabled")
            return
        }

        def options = config.bddOptions
        def env = steps.env()
        def srcDir = config.srcDir
        def workspaceDir = FileUtils.getFilePath("$env.WORKSPACE")

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            steps.installLocalDependencies()
            steps.createDir('build/out')
            List<Integer> returnStatuses = []

            def coverageOpts = config.coverageOptions
            def coverageContext = CoverageUtils.prepareContext(config, options)

            steps.lock(coverageContext.lockableResource) {
                if (coverageContext != null) {
                    CoverageUtils.startCoverage(steps, coverageOpts, coverageContext, workspaceDir, srcDir, this)
                }

                config.bddOptions.vrunnerSteps.each {
                    Logger.println("Шаг запуска сценариев командой ${it}")
                    String vrunnerPath = VRunner.getVRunnerPath()
                    Integer bddReturnStatus = VRunner.exec("$vrunnerPath ${it} --ibconnection \"/F./build/ib\"", true)
                    returnStatuses.add(bddReturnStatus)
                }

                if (Collections.max(returnStatuses) > 2) {
                    steps.error("Получен неожиданный/неверный результат работы. Возможно, работа 1С:Предприятие завершилась некорректно, или возникла ошибка при запуске")
                } else if (returnStatuses.contains(1)) {
                    steps.unstable("Тестирование сценариев завершилось, но часть фич/сценариев упала")
                } else {
                    Logger.println("Тестирование сценариев завершилось успешно")
                }

                if (coverageContext != null) {
                    CoverageUtils.stopCoverage(steps, coverageOpts, coverageContext)
                }
            }
        }

        steps.stash(ALLURE_STASH, 'build/out/allure/**', true)
        steps.stash('bdd-cucumber', 'build/out/cucumber/**', true)
        if (options.coverage) {
            steps.stash(COVERAGE_STASH_NAME, COVERAGE_STASH_PATH, true)
        }

    }

    String getCoverageStashPath() {
        return COVERAGE_STASH_PATH
    }

    String getCoveragePidsPath() {
        return COVERAGE_PIDS_PATH
    }
}
