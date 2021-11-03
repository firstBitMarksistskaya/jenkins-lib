import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.InitFromFiles

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def initFromFiles = new InitFromFiles(config)
    initFromFiles.run()
}