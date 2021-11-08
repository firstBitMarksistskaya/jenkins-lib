import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.InitFromStorage

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def initFromStorage = new InitFromStorage(config)
    initFromStorage.run()
}