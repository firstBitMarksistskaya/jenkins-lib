package ru.pulsar.jenkins.library.steps

import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper
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

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {

            if (config.initInfobaseOptions.runMigration) {
                Logger.println("Запуск миграции ИБ")

                // Запуск миграции
                steps.catchError {
                    steps.cmd('oscript_modules/bin/vrunner run --command "ЗапуститьОбновлениеИнформационнойБазы;ЗавершитьРаботуСистемы;" --execute \\$runnerRoot/epf/ЗакрытьПредприятие.epf --ibconnection "/F./build/ib"')
                }
            } else {
                Logger.println("Шаг миграции ИБ выключен")
            }

            steps.catchError {
                if (config.initInfobaseOptions.additionalInitializationSteps.length == 0) {
                    FileWrapper[] files = steps.findFiles("tools/vrunner.init*.json")
                    files = files.sort new OrderBy( { it.name })
                    files.each {
                        Logger.println("Первичная инициализация файлом ${it.path}")
                        steps.cmd("oscript_modules/bin/vrunner vanessa --settings ${it.path} --ibconnection \"/F./build/ib\"")
                    }
                } else {
                    config.initInfobaseOptions.additionalInitializationSteps.each {
                        Logger.println("Первичная инициализация командой ${it}")
                        steps.cmd("oscript_modules/bin/vrunner ${it} --ibconnection \"/F./build/ib\"")
                    }
                }
            }
        }

        steps.stash('init-allure', 'build/out/allure/**', true)
        steps.stash('init-cucumber', 'build/out/cucumber/**', true)
    }
}
