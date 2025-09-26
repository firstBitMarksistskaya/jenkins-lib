package ru.pulsar.jenkins.library.steps

import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner
import ru.pulsar.jenkins.library.utils.FileUtils

class InitInfoBase implements Serializable {

    private final JobConfiguration config

    InitInfoBase(JobConfiguration config) {
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

        def env = steps.env();

        String workspaceAllure = FileUtils.getFilePath("$env.WORKSPACE/build/out/allure").getRemote()
        Logger.println("Очистка каталога Allure: $workspaceAllure")
        steps.deleteDir(workspaceAllure)        
        String workspaceCucumber = FileUtils.getFilePath("$env.WORKSPACE/build/out/cucumber").getRemote()
        Logger.println("Очистка каталога Cucumber: $workspaceCucumber")
        steps.deleteDir(workspaceCucumber)        
        
        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {

            String vrunnerPath = VRunner.getVRunnerPath()

            // Нужны ли настройки vrunner
            def options = config.initInfoBaseOptions
            String settingsIncrement = ''
            String vrunnerSettings = options.vrunnerSettings
            if (config.templateDBLoaded() && steps.fileExists(vrunnerSettings)) {
                settingsIncrement = " --settings $vrunnerSettings"
            }

            if (options.runMigration) {
                Logger.println("Запуск миграции ИБ")

                String command = vrunnerPath + ' run --command "ЗапуститьОбновлениеИнформационнойБазы;ЗавершитьРаботуСистемы;" --execute '
                String executeParameter = '$runnerRoot/epf/ЗакрытьПредприятие.epf'
                if (steps.isUnix()) {
                    executeParameter = '\\' + executeParameter
                }
                command += executeParameter
                command += ' --ibconnection "/F./build/ib"'

                command += settingsIncrement
                // Запуск миграции
                steps.catchError {
                    VRunner.exec(command)
                }
            } else {
                Logger.println("Шаг миграции ИБ выключен")
            }

            steps.catchError {
                if (options.additionalInitializationSteps.length == 0) {
                    FileWrapper[] files = steps.findFiles("tools/vrunner.init*.json")
                    files = files.sort new OrderBy( { it.name })
                    files.each {
                        Logger.println("Первичная инициализация файлом ${it.path}")
                        VRunner.exec("$vrunnerPath vanessa --settings ${it.path} --ibconnection \"/F./build/ib\"")
                    }
                } else {
                    options.additionalInitializationSteps.each {
                        Logger.println("Первичная инициализация командой ${it}")
                        VRunner.exec("$vrunnerPath ${it} --ibconnection \"/F./build/ib\"${settingsIncrement}")
                    }
                }
            }
        }

        steps.stash('init-allure', 'build/out/allure/**', true)
        steps.stash('init-cucumber', 'build/out/cucumber/**', true)
    }
}
