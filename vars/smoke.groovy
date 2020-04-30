import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry

def call(JobConfiguration config) {

    ContextRegistry.registerDefaultContext(this)

    // TODO: Вынести в отдельный класс по аналогии с SonarScanner

    printLocation()

    if (!config.stageFlags.smoke) {
        echo("Smoke tests step is disabled")
        return
    }

    def options = config.syntaxCheckOptions

    installLocalDependencies()

    unzipInfobase()

}
