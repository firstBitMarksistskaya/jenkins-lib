import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.SmokeTest

def call(JobConfiguration config) {

    ContextRegistry.registerDefaultContext(this)

    def smokeTest = new SmokeTest(config)
    smokeTest.run()

}
