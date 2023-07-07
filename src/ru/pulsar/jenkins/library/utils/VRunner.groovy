package ru.pulsar.jenkins.library.utils

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class VRunner {

    static final String DEFAULT_VRUNNER_OPTS = "RUNNER_NOCACHEUSE=1"

    static String getVRunnerPath() {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String vrunnerBinary = steps.isUnix() ? "vrunner" : "vrunner.bat"
        String vrunnerPath = "oscript_modules/bin/$vrunnerBinary"
        if (!steps.fileExists(vrunnerPath)) {
            vrunnerPath = vrunnerBinary
        }

        return vrunnerPath
    }

    static int exec(String command, boolean returnStatus = false) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        steps.withEnv([DEFAULT_VRUNNER_OPTS]) {
            return steps.cmd(command, returnStatus)
        } as int
    }

    static boolean configContainsSetting(String configPath, String settingName) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        if (!steps.fileExists(configPath)) {
            return false
        }

        String fileContent = steps.readFile(configPath)
        return fileContent.contains("\"$settingName\"")
    }

    static String loadExtCommand(String name) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def env = steps.env()
        def vrunnerPath = getVRunnerPath()

        String pathToExt = "$env.WORKSPACE/build/out/${name}.cfe"
        FilePath localPathToExt = FileUtils.getFilePath(pathToExt)

        // Команда загрузки расширения
        String loadCommand = vrunnerPath + ' run --command "Путь=' + localPathToExt + ';ЗавершитьРаботуСистемы;" --execute '
        String executeParameter = '$runnerRoot/epf/ЗагрузитьРасширениеВРежимеПредприятия.epf'
        if (steps.isUnix()) {
            executeParameter = '\\' + executeParameter
        }
        loadCommand += executeParameter
        loadCommand += ' --ibconnection "/F./build/ib"'

        return loadCommand
    }
}
