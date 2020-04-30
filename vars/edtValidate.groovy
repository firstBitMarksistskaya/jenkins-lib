import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.EdtValidate

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def edtValidate = new EdtValidate(config)
    edtValidate.run()
}
