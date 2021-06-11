import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.InitInfobase

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def initInfobase = new InitInfobase(config)
    initInfobase.run()
}