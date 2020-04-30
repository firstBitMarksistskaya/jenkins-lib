import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.EdtTransform

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def edtTransform = new EdtTransform(config)
    edtTransform.run()
}
