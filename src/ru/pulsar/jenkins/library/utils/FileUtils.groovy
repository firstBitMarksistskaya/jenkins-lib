package ru.pulsar.jenkins.library.utils

import hudson.FilePath
import jenkins.model.Jenkins
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class FileUtils {

    static FilePath getFilePath(String path) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def env = steps.env();

        String nodeName = env.NODE_NAME;
        if (nodeName == null) {
            steps.error 'Переменная среды NODE_NAME не задана. Запуск вне node или без agent?'
        }

        if (nodeName == "master") {
            return new FilePath(new File(path));
        } else {
            return new FilePath(Jenkins.getInstanceOrNull().getComputer(nodeName).getChannel(), path);
        }
    }
}
