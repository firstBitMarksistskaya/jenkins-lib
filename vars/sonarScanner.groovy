import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.SonarScanner

def call(JobConfiguration config, String rootFile = 'src/cf/Configuration.xml') {
    ContextRegistry.registerDefaultContext(this)

    def sonarScanner = new SonarScanner(config, rootFile)
    sonarScanner.run()
}
