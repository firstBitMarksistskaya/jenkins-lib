package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

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

            // TODO: удалить после выхода VAS 1.0.35
            steps.httpRequest(
                'https://cloud.svc.pulsar.ru/index.php/s/WKwmqpFXSjfYjAH/download',
                'oscript_modules/vanessa-automation-single/vanessa-automation-single.epf'
            )

            steps.catchError {
                config.bddOptions.vrunnerSteps.each {
                    Logger.println("Шаг запуска сценариев командой ${it}")
                    steps.cmd("oscript_modules/bin/vrunner ${it} --ibconnection \"/F./build/ib\"")
                }
            }
        }

        steps.zip('build/ib', 'bdd_1Cv8.1CD.zip', '1Cv8.1CD', true)
        steps.stash('bdd-allure', 'build/out/allure/**', true)
        steps.stash('bdd-cucumber', 'build/out/cucumber/**', true)
    }
}
