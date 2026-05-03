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

        def isInfobaseInitialized = true

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

            Map<String, Integer> exitStatuses = new LinkedHashMap<>()

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
                def migrationStatusFile = "build/migration-exit-status.log"
                boolean bspConfiguration = isBspConfiguration(steps)
                if (bspConfiguration) {
                    command += " --exitCodePath \"${migrationStatusFile}\""
                } else {
                    Logger.println("Конфигурация не на БСП, запуск миграции ИБ без контроля ${migrationStatusFile}")
                }
                // Запуск миграции
                steps.catchError {
                    Integer exitStatus = VRunner.exec(command, true)
                    if (bspConfiguration) {
                        exitStatuses.put(command, VRunner.readExitStatusFromFile(migrationStatusFile, exitStatus))
                    } else {
                        exitStatuses.put(command, exitStatus)
                    }
                }
            } else {
                Logger.println("Шаг миграции ИБ выключен")
            }

            if (options.additionalInitializationSteps.length == 0) {
                FileWrapper[] files = steps.findFiles("tools/vrunner.init*.json")
                files = files.sort new OrderBy({ it.name })
                files.each {
                    Logger.println("Первичная инициализация файлом ${it.path}")
                    def command = "$vrunnerPath vanessa --settings ${it.path} --ibconnection \"/F./build/ib\""
                    Integer exitStatus = VRunner.exec(command, true)
                    exitStatuses.put(command, exitStatus)
                }
            } else {
                options.additionalInitializationSteps.each {
                    Logger.println("Первичная инициализация командой ${it}")
                    def command = "$vrunnerPath ${it} --ibconnection \"/F./build/ib\"${settingsIncrement}"
                    Integer exitStatus = VRunner.exec(command, true)
                    exitStatuses.put(command, exitStatus)
                }
            }

            if (Collections.max(exitStatuses.values()) >= 2) {
                Logger.println("Получен неожиданный/неверный результат работы шагов инициализации ИБ. Возможно, имеется ошибка в параметрах запуска vanessa-runner")
                isInfobaseInitialized = false
            } else if (exitStatuses.values().contains(1)) {
                Logger.println("Инициализация ИБ завершилась, но некоторые ее шаги выполнились некорректно")
                isInfobaseInitialized = false
            } else {
                Logger.println("Инициализация ИБ завершилась успешно")
            }

            def exitStatusesMessage = "Статусы команд инициализации ИБ:"
            exitStatuses.each { key, value ->
                exitStatusesMessage += "\n${key}: status ${value}"
            }
            Logger.println(exitStatusesMessage)
        }

        steps.stash('init-allure', 'build/out/allure/**', true)
        steps.stash('init-cucumber', 'build/out/cucumber/**', true)

        if (!isInfobaseInitialized) {
            // Throws exception
            steps.error("Инициализация ИБ завершилась с ошибками")
        }
    }

    private boolean isBspConfiguration(IStepExecutor steps) {
        if (config.srcDir == null || config.srcDir.trim().isEmpty()) {
            Logger.println("Не указан srcDir, конфигурация считается не на БСП")
            return false
        }

        String sourceDir = config.srcDir.replace('\\', '/')
        FileWrapper[] xmlFiles = steps.findFiles("${sourceDir}/**/ОбновлениеИнформационнойБазыБСП.xml") ?: new FileWrapper[0]
        FileWrapper[] mdoFiles = steps.findFiles("${sourceDir}/**/ОбновлениеИнформационнойБазыБСП.mdo") ?: new FileWrapper[0]
        boolean bspConfiguration = xmlFiles.length > 0 || mdoFiles.length > 0

        Logger.println("Определение БСП по исходникам ${sourceDir}: ${bspConfiguration ? 'БСП' : 'не БСП'}")
        return bspConfiguration
    }
}
