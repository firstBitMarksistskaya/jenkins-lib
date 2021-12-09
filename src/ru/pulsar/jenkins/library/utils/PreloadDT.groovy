package ru.pulsar.jenkins.library.utils

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class PreloadDT {

    final static PRELOAD_DT_LOCAL_PATH = "build/out/preload.dt"

    static preloadDT(String url, String vrunnerSettings) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env()

        String vrunnerPath = VRunner.getVRunnerPath()

        FilePath localPathToPreloadDT = FileUtils.getFilePath("$env.WORKSPACE/$PRELOAD_DT_LOCAL_PATH")
        Logger.println("Скачивание DT в $localPathToPreloadDT")
        localPathToPreloadDT.copyFrom(new URL("$url"))

        String command = vrunnerPath + " init-dev --dt $localPathToPreloadDT"
        if (steps.fileExists(vrunnerSettings)) {
            command += " --settings $vrunnerSettings"
        }
        VRunner.exec(command)

    }
}
