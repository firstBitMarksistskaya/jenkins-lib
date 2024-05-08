import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.LoadExtensions

def call(JobConfiguration config, String stageName = "") {
    ContextRegistry.registerDefaultContext(this)

    def loadExtensions = new LoadExtensions(config, stageName)
    loadExtensions.run()
}