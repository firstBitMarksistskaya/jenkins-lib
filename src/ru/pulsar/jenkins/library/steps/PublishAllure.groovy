package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger

class PublishAllure implements Serializable {

    private final JobConfiguration config
    private IStepExecutor steps

    PublishAllure(JobConfiguration config) {
        this.config = config
    }

    def run() {
        Logger.printLocation()

        if (config == null) {
            Logger.println('jobConfiguration is not initialized')
            return
        }

        steps = ContextRegistry.getContext().getStepExecutor()

        if (config.stageFlags.initSteps) {
            safeUnstash('init-allure')
        }
        if (config.stageFlags.bdd) {
            safeUnstash(Bdd.ALLURE_STASH)
        }
        if (config.stageFlags.yaxunit && config.yaxunitOptions.publishToAllureReport) {
            safeUnstash(Yaxunit.YAXUNIT_ALLURE_STASH)
        }
        if (config.stageFlags.smoke && config.smokeTestOptions.publishToAllureReport) {
            safeUnstash(SmokeTest.ALLURE_STASH)
        }

        def env = steps.env()

        FilePath allurePath = FileUtils.getFilePath("$env.WORKSPACE/build/out/allure")
        if (!allurePath.exists()) {
            Logger.println('Отсутствуют результаты allure для публикации')
            return
        }

        List<String> results = new ArrayList<>()

        int directoryCount = allurePath.listDirectories().size()
        Logger.println("Log: Количество подкаталогов в $allurePath: $directoryCount")

        FilePath workSpacePath = FileUtils.getFilePath("$env.WORKSPACE")
        String basePath = replaceBackslashesWithSlashes(workSpacePath.toString())
        Logger.println("Log: workSpacePath = $workSpacePath, basePath = $basePath")

        allurePath.listDirectories().each { FilePath filePath ->
            FilePath pathCurrent = FileUtils.getFilePath("$filePath")
            String pathdir = FileUtils.getLocalPath(pathCurrent)
            Logger.println("Log: pathCurrent = $pathCurrent, pathdir = $pathdir")

            String rezultPath = getRelativePath(pathdir, basePath)
            Logger.println("Log: pathdir = $pathdir, basePath = $basePath. Результат через getRelativePath() = $rezultPath")
            results.add(rezultPath)

            //String pathdir = FileUtils.getLocalPath(filePath)
            //results.add(FileUtils.getLocalPath(filePath))
            //Logger.println("Log: Результат для добавления в allure getLocalPath($pathCurrent): $pathdir")
        }

        String pathAllure = FileUtils.getLocalPath(allurePath)
        Logger.println("Log: если в подкаталогах allure пусто, то будет добавлен только путь на основе getLocalPath($allurePath): $pathAllure")
        if (results.isEmpty()) {
            Logger.println('Log: результат пустой и фиксиурем путь выше')
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

    private static replaceBackslashesWithSlashes(String path) {
        return path.replace('\\', '/')
    }

    private static String getRelativePath(String absolutePath, String basePath) {
        def normalizedAbsolutePath = new File(absolutePath).canonicalPath
        def normalizedBasePath = new File(basePath).canonicalPath

        def relativePath = normalizedAbsolutePath.replaceFirst(normalizedBasePath, '')

        // Убираем начальный '/' если он есть
        if (relativePath.startsWith('/')) {
            relativePath = relativePath.substring(1)
        }

        return relativePath
    }

}
