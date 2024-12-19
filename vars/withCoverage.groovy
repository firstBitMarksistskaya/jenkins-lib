import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.StepCoverageOptions
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.Coverable
import ru.pulsar.jenkins.library.steps.WithCoverage

def call(JobConfiguration config, Coverable stage, StepCoverageOptions options, Closure body) {
    ContextRegistry.registerDefaultContext(this)

    WithCoverage withCoverage = new WithCoverage(config, stage, options, body)
    return withCoverage.run()
}