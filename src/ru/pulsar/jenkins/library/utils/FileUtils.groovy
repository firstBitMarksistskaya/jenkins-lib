package ru.pulsar.jenkins.library.utils

import hudson.FilePath
import jenkins.model.Jenkins
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

import java.nio.file.Path

class FileUtils {

    static FilePath getFilePath(String path) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def env = steps.env();

        String nodeName = env.NODE_NAME;
        if (nodeName == null) {
            steps.error 'Переменная среды NODE_NAME не задана. Запуск вне node или без agent?'
        }

        if (nodeName == "master" || nodeName == "built-in") {
            return new FilePath(new File(path));
        } else {
            return new FilePath(Jenkins.getInstanceOrNull().getComputer(nodeName).getChannel(), path);
        }
    }

    static String getLocalPath(FilePath filePath) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def env = steps.env();

        Path workspacePath = new File(env.WORKSPACE).toPath()
        Path rawFilePath = new File(filePath.getRemote()).toPath()

        return workspacePath.relativize(rawFilePath)
            .toString()
            .replaceAll('\\\\\\\\', '/')
            .replaceAll('\\\\', '/')
            .toString()
    }
}
