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

        FilePath allurePath = FileUtils.getFilePath('build/out/allure')
        if (!allurePath.exists()) {
            Logger.println("Отсутствуют результаты allure для публикации")
            return
        }

        List<String> results = new ArrayList<>();

        def allureSubDirs = allurePath.listDirectories()
        if (allureSubDirs.size() > 0) {
            allureSubDirs.forEach({ filePath -> results.add(getPath(filePath)) })
        } else {
            results.add(getPath(allurePath))
        }

        steps.allure(results)
    }

    private static String getPath(FilePath filePath) {
        filePath.getBaseName() + File.separator + filePath.getName()
    }
}
