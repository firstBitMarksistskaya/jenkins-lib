package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class InitInfobase implements Serializable {

    private final JobConfiguration config;

    InitInfobase(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        steps.createDir('build/out')

        if (!config.stageFlags.initSteps) {
            Logger.println("Init step is disabled")
            return
        }

        if (config.initInfobaseOptions.runMigration) {
            Logger.println("Запуск миграции ИБ")

            // Запуск миграции
            steps.catchError {
                steps.cmd('oscript_modules/bin/vrunner run --command "ЗапуститьОбновлениеИнформационнойБазы;ЗавершитьРаботуСистемы;" --execute \\$runnerRoot/epf/ЗакрытьПредприятие.epf --ibconnection "/F./build/ib"')
            }
        } else {
            Logger.println("Шаг миграции ИБ выключен")
        }

        // TODO: удалить после выхода VAS 1.0.35
        steps.httpRequest(
            'https://cloud.svc.pulsar.ru/index.php/s/WKwmqpFXSjfYjAH/download',
            'oscript_modules/vanessa-automation-single/vanessa-automation-single.epf'
        )

        steps.catchError {
            config.initInfobaseOptions.additionalMigrationSteps.each {
                Logger.println("Первичная инициализация командой ${it}")
                steps.cmd("oscript_modules/bin/vrunner ${it} --ibconnection \"/F./build/ib\"")
            }
        }

        steps.stash('init-allure', 'build/out/allure/*', true)
        steps.stash('init-cucumber', 'build/out/cucumber/*', true)
    }
}
