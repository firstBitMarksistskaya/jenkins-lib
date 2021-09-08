import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.DesignerToEdtFormatTransformation

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def edtTransform = new DesignerToEdtFormatTransformation(config)
    edtTransform.run()
}
