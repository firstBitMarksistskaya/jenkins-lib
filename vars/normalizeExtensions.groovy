import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.NormalizeExtensions

def call(JobConfiguration config, String stageName) {
    ContextRegistry.registerDefaultContext(this)

    def normalizeExtensions = new NormalizeExtensions(config, stageName)
    normalizeExtensions.run()
}
