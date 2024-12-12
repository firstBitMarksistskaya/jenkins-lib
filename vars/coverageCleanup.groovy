import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.CoverageCleanup

def call(JobConfiguration config, String stageName) {
    ContextRegistry.registerDefaultContext(this)

    def coverageCleanup = new CoverageCleanup(config, stageName)
    coverageCleanup.run()

}