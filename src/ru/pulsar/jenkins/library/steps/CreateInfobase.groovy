package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.ExclusiveAccess
import ru.pulsar.jenkins.library.utils.InfobaseConnection
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner
import ru.pulsar.jenkins.library.utils.FileUtils

class CreateInfobase implements Serializable {

    private final JobConfiguration config;

    CreateInfobase(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        def env = steps.env()

        steps.installLocalDependencies();

        String templateDBPath = config.initInfoBaseOptions.templateDBPath
        if (templateDBPath == '') {
            if (config.basePath) {
                // Шаблон не указан, пайплайн подключается к существующей базе.
                Logger.println("Используется существующая информационная база ${config.baseName()}")
            } else {
                // Не указан путь к базе данных, создадим пустую базу данных.
                createBase()
            }
        } else if (templateDBPath.endsWith('.1CD')) {
            if (InfobaseConnection.isServer(config.baseName())) {
                steps.error("Шаблон базы .1CD нельзя загрузить в серверную базу ${config.baseName()}: " +
                    "данные серверной базы хранятся в СУБД, файл базы скопировать некуда. " +
                    "Откройте .1CD как файловую базу, выгрузите ее в .dt и укажите .dt в initInfobase -> templateDBPath.")
            } else {
                // Это файл базы данных 1С, просто скопируем его.
                String pathToInfobase = "${fileBaseDir(env)}/1Cv8.1CD"
                FileUtils.loadFile(templateDBPath, env, pathToInfobase)
            }
        } else if (templateDBPath.endsWith('.dt')) {
            // Это файл дампа БД, скопируем его и загрузим в базу.
            String pathToDt = "$env.WORKSPACE/build/tmp/dump.dt"
            FileUtils.loadFile(templateDBPath, env, pathToDt)
            if (!config.basePath) {
                // Локальную базу перед загрузкой нужно создать. Внешняя база уже существует,
                // а если файловой базы в каталоге нет, платформа создаст ее при загрузке.
                createBase()
            }
            restoreBase('build/tmp/dump.dt', steps)
        } else {
            Logger.println("Неизвестный формат базы данных. Поддерживаются только .1CD и .dt")
        }

    }

    private void createBase() {
        Logger.println("Создание информационной базы")
        String vrunnerPath = VRunner.getVRunnerPath();
        def initCommand = "$vrunnerPath init-dev  --ibconnection \"${config.baseName()}\""
        VRunner.exec(initCommand)
    }

    private void restoreBase(String dtPath, IStepExecutor steps) {
        // Загрузка из dt в vrunner 2.2.2 не работает корректно, потому инициировать через init-dev не получится.
        String vrunnerPath = VRunner.getVRunnerPath();
        def loadDtCommand = "$vrunnerPath restore --ibconnection \"${config.baseName()}\" $dtPath"

        // В существующую базу конфигуратор входит под ее пользователем
        String vrunnerSettings = config.initInfoBaseOptions.vrunnerSettings
        if (config.basePath && steps.fileExists(vrunnerSettings)) {
            loadDtCommand += " --settings $vrunnerSettings"
        }

        ExclusiveAccess.run(config) { String uccode ->
            VRunner.exec(loadDtCommand + uccode)
        }
    }

    private String fileBaseDir(def env) {
        if (!config.basePath) {
            return "$env.WORKSPACE/build/ib"
        }
        // Относительный путь vanessa-runner отсчитывает от корня проекта, то есть от рабочего каталога сборки
        return FileUtils.getAbsolutePath(InfobaseConnection.fileBaseDir(config.baseName()), env)
    }
}
