package ru.pulsar.jenkins.library.steps

import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

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

            String vrunnerPath = VRunner.getVRunnerPath();

            if (config.initInfobaseOptions.runMigration) {
                Logger.println("Запуск миграции ИБ")

                // Запуск миграции
                steps.catchError {
                    VRunner.exec(vrunnerPath + ' run --command "ЗапуститьОбновлениеИнформационнойБазы;ЗавершитьРаботуСистемы;" --execute \\$runnerRoot/epf/ЗакрытьПредприятие.epf --ibconnection "/F./build/ib"')
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
                        VRunner.exec("$vrunnerPath vanessa --settings ${it.path} --ibconnection \"/F./build/ib\"")
                    }
                } else {
                    config.initInfobaseOptions.additionalInitializationSteps.each {
                        Logger.println("Первичная инициализация командой ${it}")
                        VRunner.exec("$vrunnerPath ${it} --ibconnection \"/F./build/ib\"")
                    }
                }
                if (config.sourceFormat == SourceFormat.EDT) {
                    if (config.srcExtPath.length != 0) {
                        config.srcExtPath.each {
                            Logger.println("Загрузка расширения ${it} в ИБ")
                            VRunner.exec("$vrunnerPath compileext --inputpath \"build/cfg/ext${it}\" --extensionName --ibconnection \"/F./build/ib\"")
                        }
                }
                

            }
        }

        steps.stash('init-allure', 'build/out/allure/**', true)
        steps.stash('init-cucumber', 'build/out/cucumber/**', true)
    }
}
