package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class SyntaxCheck {

    public static final String ALLURE_STASH = 'syntax-check-allure'

    private final JobConfiguration config

    SyntaxCheck(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.syntaxCheck) {
            Logger.println("Syntax-check step is disabled")
            return
        }

        def env = steps.env()

        def options = config.syntaxCheckOptions

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            steps.installLocalDependencies()

            String junitReport = "build/out/jUnit/syntax-check/syntax-check.xml"
            FilePath pathToJUnitReport = FileUtils.getFilePath("$env.WORKSPACE/$junitReport")
            String junitReportDir = FileUtils.getLocalPath(pathToJUnitReport.getParent())

            String allureReport = "build/out/allure/syntax-check/allure.xml"
            FilePath pathToAllureReport = FileUtils.getFilePath("$env.WORKSPACE/$allureReport")
            String allureReportDir = FileUtils.getLocalPath(pathToAllureReport.getParent())

            String vrunnerPath = VRunner.getVRunnerPath()
            String command = "$vrunnerPath syntax-check --ibconnection \"/F./build/ib\""

            // Временно убрал передачу параметра.
            // См. https://github.com/vanessa-opensource/vanessa-runner/issues/361
            // command += " --workspace $env.WORKSPACE"

            if (options.groupErrorsByMetadata) {
                command += ' --groupbymetadata'
            }

            if (options.publishToJUnitReport) {
                steps.createDir(junitReportDir)
                command += " --junitpath $pathToJUnitReport"
            }

            if (options.publishToAllureReport) {
                steps.createDir(allureReportDir)
                command += " --allure-results2 $allureReportDir"
            }

            FilePath vrunnerSettings = FileUtils.getFilePath("$env.WORKSPACE/$options.vrunnerSettings")
            if (vrunnerSettings.exists()) {
                command += " --settings $vrunnerSettings"
            }

            if (!options.exceptionFile.empty && steps.fileExists(options.exceptionFile)) {
                command += " --exception-file $options.exceptionFile"
            }

            if (options.checkModes.length > 0) {
                def checkModes = options.checkModes.join(" ")
                command += " --mode $checkModes"
            }

            // Запуск синтакс-проверки
            VRunner.exec(command, true)

            if (options.publishToAllureReport) {
                steps.stash(ALLURE_STASH, "$allureReportDir/**", true)
                
                // Архивируем результат в отдельный архив и отправляем в артефакты.
                String archivePath = "build/out/allure/syntax.zip"
                steps.zip("$allureReportDir", archivePath)
                steps.archiveArtifacts("$archivePath")
                //steps.archiveArtifacts("$allureReportDir/**")
            }

            if (options.publishToJUnitReport) {
                steps.junit("$junitReportDir/*.xml", true)
                steps.archiveArtifacts("$junitReportDir/**")
            }
        }
    }
}