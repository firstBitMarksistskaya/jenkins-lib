import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.EdtValidate

def call(JobConfiguration config, String rootDir = 'src/cf') {
    ContextRegistry.registerDefaultContext(this)

    def edtValidate = new EdtValidate(config, rootDir)
    edtValidate.run()
}
