import hudson.FilePath
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.VRunner

def call(JobConfiguration config) {

    ContextRegistry.registerDefaultContext(this)

    // TODO: Вынести в отдельный класс по аналогии с SonarScanner

    printLocation()

    if (!config.stageFlags.syntaxCheck) {
        echo("Syntax-check step is disabled")
        return
    }

    def options = config.syntaxCheckOptions

    installLocalDependencies()

    unzipInfobase()

    FilePath pathToJUnitReport = FileUtils.getFilePath("$env.WORKSPACE/$options.pathToJUnitReport")

    String outPath = pathToJUnitReport.getParent()
    createDir(outPath)

    String vrunnerPath = VRunner.getVRunnerPath();
    String base = config.baseName()
    String command = "$vrunnerPath syntax-check --ibconnection \"$base\""

    // Временно убрал передачу параметра.
    // См. https://github.com/vanessa-opensource/vanessa-runner/issues/361
    // command += " --workspace $env.WORKSPACE"

    if (options.groupErrorsByMetadata) {
        command += ' --groupbymetadata'
    }

    command += " --junitpath $pathToJUnitReport";

    FilePath vrunnerSettings = FileUtils.getFilePath("$env.WORKSPACE/$options.vrunnerSettings")
    if (vrunnerSettings.exists()) {
        command += " --settings $vrunnerSettings";
    }

    if (!options.exceptionFile.empty && fileExists(options.exceptionFile)) {
        command += " --exception-file $options.exceptionFile"
    }

    if (options.checkModes.length > 0) {
        def checkModes = options.checkModes.join(" ")
        command += " --mode $checkModes"
    }

    // Запуск синтакс-проверки
    VRunner.exec(command, true)

    junit allowEmptyResults: true, testResults: FileUtils.getLocalPath(pathToJUnitReport)

    archiveArtifacts FileUtils.getLocalPath(pathToJUnitReport)
}
