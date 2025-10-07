package ru.pulsar.jenkins.library.utils

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

import java.nio.file.NoSuchFileException

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

    static Integer readExitStatusFromFile(String path) {

        Logger.println("Читаем статус возврата из файла ${path}")

        try {
            String content = ContextRegistry.getContext().getStepExecutor()
                    .readFile(path)
                    .trim()
                    .replaceAll(/^\uFEFF/, '') // платформа генерирует файл с BOM

            if (!content) {
                Logger.println("Файл со статусом возврата ${path} пуст")
                return 1
            } else {
                return content.toInteger()
            }
        } catch (NoSuchFileException e) {
            Logger.println("Файл со статусом возврата ${path} не найден: ${e.message}")
            return 1
        } catch (NumberFormatException e) {
            Logger.println("В файле со статусом возврата ${path} записано не числовое значение: ${e.message}")
            return 1
        } catch (Exception e) {
            Logger.println("При чтении файла со статусом возврата ${path} возникла ошибка: ${e.message}")
            return 1
        }
    }
}
