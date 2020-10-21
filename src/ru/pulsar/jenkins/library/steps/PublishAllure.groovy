package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger

class PublishAllure implements Serializable {

    private final JobConfiguration config;

    PublishAllure(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        steps.unstash('init-allure')

        def env = steps.env();

        FilePath allurePath = FileUtils.getFilePath("$env.WORKSPACE/build/out/allure")
        if (!allurePath.exists()) {
            Logger.println("Отсутствуют результаты allure для публикации")
            return
        }

        List<String> results = new ArrayList<>();

        Logger.println(allurePath.toString())
        Logger.println(allurePath.listDirectories().toString())

        allurePath.listDirectories().each { FilePath filePath ->
            results.add(filePath.getRemote())
        }
        if (results.isEmpty()) {
            results.add(allurePath.getRemote())
        }

        Logger.println(results.toString())

        steps.allure(results)
    }

}
