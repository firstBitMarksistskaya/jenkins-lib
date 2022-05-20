package ru.pulsar.jenkins.library.utils

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class OscriptModules {
    static String getAppExecutable(String executableName) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String executableBinary = steps.isUnix() ? executableName : "${executableName}.bat";
        String executablePath = "oscript_modules/bin/$executableBinary";
        if (!steps.fileExists(executablePath)) {
            executablePath = executableBinary;
        }

        return executablePath;
    }
}