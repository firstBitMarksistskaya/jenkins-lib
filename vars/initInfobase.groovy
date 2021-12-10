import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.InitInfoBase

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def initInfobase = new InitInfoBase(config)
    initInfobase.run()
}