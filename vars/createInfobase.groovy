import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.CreateInfobase

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def createInfobase = new CreateInfobase(config)
    createInfobase.run()
}