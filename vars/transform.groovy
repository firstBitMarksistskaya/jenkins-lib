import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.ResultsTransformer

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def transformer = new ResultsTransformer(config)
    transformer.run()
}