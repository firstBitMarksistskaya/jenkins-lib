import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.LoadExtensions

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def loadExtensions = new LoadExtensions(config)
    loadExtensions.run()
}