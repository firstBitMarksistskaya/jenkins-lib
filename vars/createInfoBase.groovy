import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.CreateInfoBase

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def createInfobase = new CreateInfoBase(config)
    createInfobase.run()
}