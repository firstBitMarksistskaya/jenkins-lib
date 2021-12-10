package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger

class PublishAllure implements Serializable {

    private final JobConfiguration config;
    private IStepExecutor steps;

    PublishAllure(JobConfiguration config) {
        this.config = config
    }

    def run() {
        steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        safeUnstash('init-allure')
        safeUnstash('bdd-allure')
        if (config.smokeTestOptions.publishToAllureReport) {
            safeUnstash(SmokeTest.SMOKE_ALLURE_STASH)
        }

        def env = steps.env();

        FilePath allurePath = FileUtils.getFilePath("$env.WORKSPACE/build/out/allure")
        if (!allurePath.exists()) {
            Logger.println("Отсутствуют результаты allure для публикации")
            return
        }

        List<String> results = new ArrayList<>();

        allurePath.listDirectories().each { FilePath filePath ->
            results.add(FileUtils.getLocalPath(filePath))
        }
        if (results.isEmpty()) {
            results.add(FileUtils.getLocalPath(allurePath))
        }

        steps.allure(results)
    }

    private void safeUnstash(String stashName) {
        try {
            steps.unstash(stashName)
        } catch (Exception ignored) {
            Logger.println("Can't unstash $stashName")
        }
    }
}
