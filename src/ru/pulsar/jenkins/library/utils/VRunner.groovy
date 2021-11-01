package ru.pulsar.jenkins.library.utils

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class VRunner {

    static String getVRunnerPath() {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String vrunnerBinary = steps.isUnix() ? "vrunner" : "vrunner.bat";
        String vrunnerPath = "oscript_modules/bin/$vrunnerBinary";
        if (!steps.fileExists(vrunnerPath)) {
            vrunnerPath = vrunnerBinary;
        }

        return vrunnerPath;
    }
}
