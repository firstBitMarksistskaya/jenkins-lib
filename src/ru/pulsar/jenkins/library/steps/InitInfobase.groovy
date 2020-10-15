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

        if (!config.stageFlags.initSteps) {
            Logger.println("Init step is disabled")
            return
        }

        if (config.initInfobaseOptions.runMigration) {
            Logger.println("Запуск миграции ИБ")

            // Запуск миграции
            steps.cmd('oscript_modules/bin/vrunner run --command "ЗапуститьОбновлениеИнформационнойБазы;ЗавершитьРаботуСистемы;" --execute \\$runnerRoot/epf/ЗакрытьПредприятие.epf --ibconnection "/F./build/ib"')
        } else {
            Logger.println("Шаг миграции ИБ выключен")
        }

        config.initInfobaseOptions.additionalMigrationSteps.each {
            Logger.println("Первичная инициализация командой ${it}")
            steps.cmd("oscript_modules/bin/vrunner ${it} --ibconnection \"/F./build/ib\"")
        }
    }
}
