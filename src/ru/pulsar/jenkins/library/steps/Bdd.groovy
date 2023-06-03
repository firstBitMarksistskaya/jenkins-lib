package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class Bdd implements Serializable {

    private final JobConfiguration config

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

        def env = steps.env();
        def srcDir = config.srcDir
        def projectDir = new File("$env.WORKSPACE").getCanonicalPath()

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            steps.installLocalDependencies()
            steps.createDir('build/out')
            List<Integer> returnStatuses = []

            def coverageOpts = config.coverageOptions;
            if (options.coverage) {
                steps.start("${coverageOpts.dbgsPath} --addr=127.0.0.1 --port=1550")
                steps.start("${coverageOpts.coverage41CPath} start -i DefAlias -u http://127.0.0.1:1550 -P $projectDir -s $srcDir -o build/out/bdd-coverage.xml")
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

            if (options.coverage) {
                steps.cmd("${coverageOpts.coverage41CPath} stop -i DefAlias -u http://127.0.0.1:1550")
            }
        }

        steps.stash('bdd-allure', 'build/out/allure/**', true)
        steps.stash('bdd-cucumber', 'build/out/cucumber/**', true)
        if (options.coverage) {
            steps.stash('bdd-coverage', 'build/out/bdd-coverage.xml', true)
        }

    }
}
