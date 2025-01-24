import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.ZipInfobase

def call(JobConfiguration config, String stageName) {
    ContextRegistry.registerDefaultContext(this)

    def zipInfobase = new ZipInfobase(config, stageName)
    zipInfobase.run()
}
