import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.EdtTransform

def call(JobConfiguration config, String rootDir = 'src/cf') {
    ContextRegistry.registerDefaultContext(this)

    def edtTransform = new EdtTransform(config, rootDir)
    edtTransform.run()
}
