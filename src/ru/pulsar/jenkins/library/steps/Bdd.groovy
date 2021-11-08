package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class Bdd implements Serializable {

    private final JobConfiguration config;

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

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            steps.installLocalDependencies()

            steps.createDir('build/out')

            steps.catchError {
                config.bddOptions.vrunnerSteps.each {
                    Logger.println("Шаг запуска сценариев командой ${it}")
                    String vrunnerPath = VRunner.getVRunnerPath();
                    VRunner.exec("$vrunnerPath ${it} --ibconnection \"/F./build/ib\"")
                }
            }
        }

        steps.stash('bdd-allure', 'build/out/allure/**', true)
        steps.stash('bdd-cucumber', 'build/out/cucumber/**', true)
    }
}
