import hudson.FilePath
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry

def call(JobConfiguration config) {

    ContextRegistry.registerDefaultContext(this)

    printLocation()

    def options = config.syntaxCheckOptions
    if (!options.enabled) {
        echo("Syntax-check step is disabled")
        return
    }

    installLocalDependencies()

    unzipInfobase()

    def junitPath = new FilePath(new File(options.pathToJUnitReport))
    junitPath.mkdirs()

    String command = "oscript_modules/bin/vrunner syntax-check --ibconnection \"/F./build/ib\""

    if (options.groupErrorsByMetadata) {
        command += " --groupbymetadata"
    }

    command += " --junitpath " + options.pathToJUnitReport;

    command += " --mode " + options.checkModes.join(" ")

    // Запуск синтакс-проверки
    cmd(command, true)

    junit allowEmptyResults: true, testResults: options.pathToJUnitReport

}
