package ru.pulsar.jenkins.library.utils

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class OscriptModules {
    static String getModulePath(String moduleName) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String moduleBinary = steps.isUnix() ? moduleName : "$moduleName"".bat";
        String modulePath = "oscript_modules/bin/$moduleBinary";
        if (!steps.fileExists(modulePath)) {
            modulePath = moduleBinary;
        }

        return modulePath;
    }
}