import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.Yaxunit

def call(JobConfiguration config) {

    ContextRegistry.registerDefaultContext(this)

    def yaxunit = new Yaxunit(config)
    yaxunit.run()

}
