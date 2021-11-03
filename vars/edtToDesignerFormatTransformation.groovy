import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.EdtToDesignerFormatTransformation

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def edtBackTransform = new EdtToDesignerFormatTransformation(config)
    edtBackTransform.run()
}
