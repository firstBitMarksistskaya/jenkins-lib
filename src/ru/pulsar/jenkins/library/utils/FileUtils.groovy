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
        String workspacePath = steps.env().WORKSPACE
        String fileRemotePath = filePath.getRemote()

        // Нормализуем пути: заменяем обратные слэши на прямые
        workspacePath = workspacePath.replace('\\', '/')
        fileRemotePath = fileRemotePath.replace('\\', '/')

        // Проверяем, что файл находится внутри рабочей директории
        if (!fileRemotePath.startsWith(workspacePath)) {
            throw new IllegalArgumentException("File path is not within the workspace directory")
        }

        // Вычисляем относительный путь
        return fileRemotePath.substring(workspacePath.length() + 1)
    }

    static void loadFile(String filePathFrom, def env, String filePathTo) {

        FilePath localPathToFile = getFilePath(filePathTo)

        if (isValidUrl(filePathFrom)) {
            // If the path is a URL, download the file
            localPathToFile.copyFrom(new URL(filePathFrom))
        } else {
            // If the path is a local file, copy the file
            String localPath = getAbsolutePath(filePathFrom, env)
            FilePath localFilePath = getFilePath(localPath)
            localPathToFile.copyFrom(localFilePath)
        }
    }

    private static boolean isValidUrl(String url) {
        try {
            new URL(url).toURI()
            return true
        } catch (MalformedURLException | URISyntaxException e) {
            return false
        }
    }

    private static String getAbsolutePath(String path, def env) {
        if (path.startsWith("/") || path.startsWith("\\") || path.matches("^[A-Za-z]:.*")) {
            return path
        } else {
            return "${env.WORKSPACE}/${path}"
        }
    }
}
