package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.InitInfoBaseOptions.Extension
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner
import hudson.FilePath
import ru.pulsar.jenkins.library.utils.FileUtils

class LoadExtensions implements Serializable {

    private final JobConfiguration config;

    LoadExtensions(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        def env = steps.env();
        String cfeDir = "$env.WORKSPACE/$GetExtensions.EXTENSIONS_OUT_DIR"

        String vrunnerPath = VRunner.getVRunnerPath();
               
        config.initInfoBaseOptions.extensions.each {
            Logger.println("Установим расширение ${it.name}")
            loadExtension(it, vrunnerPath, steps, cfeDir)
        }
    }

    private void loadExtension (Extension extension, String vrunnerPath, IStepExecutor steps, String cfeDir) {

        String pathToExt = "$cfeDir/${extension.name}.cfe"
        FilePath localPathToExt = FileUtils.getFilePath(pathToExt)

        // Команда загрузки расширения
        String loadCommand = vrunnerPath + ' run --command "Путь=' + localPathToExt + ';ЗавершитьРаботуСистемы;" --execute '
        String executeParameter = '$runnerRoot/epf/ЗагрузитьРасширениеВРежимеПредприятия.epf'
        if (steps.isUnix()) {
            executeParameter = '\\' + executeParameter
        }
        loadCommand += executeParameter
        loadCommand += ' --ibconnection "/F./build/ib"'

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            VRunner.exec(loadCommand)
        }
    }
}
