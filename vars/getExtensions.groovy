import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.GetExtensions

def call(JobConfiguration config, boolean stashExtensions = true) {
    ContextRegistry.registerDefaultContext(this)

    def getExtensions = new GetExtensions(config, stashExtensions)
    getExtensions.run()
}