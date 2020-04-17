import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry

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

    def outPath = new File(options.pathToJUnitReport).getParent()
    dir(outPath) { echo '' }

    String command = "oscript_modules/bin/vrunner syntax-check --ibconnection \"/F./build/ib\""

    // Временно убрал передачу параметра.
    // См. https://github.com/vanessa-opensource/vanessa-runner/issues/361
    // command += " --workspace $env.WORKSPACE"

    if (options.groupErrorsByMetadata) {
        command += " --groupbymetadata"
    }

    command += " --junitpath " + options.pathToJUnitReport;

    command += " --mode " + options.checkModes.join(" ")

    // Запуск синтакс-проверки
    cmd(command, true)

    junit allowEmptyResults: true, testResults: options.pathToJUnitReport

    archiveArtifacts options.pathToJUnitReport
}
