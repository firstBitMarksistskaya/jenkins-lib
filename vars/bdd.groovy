import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.Bdd

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def bdd = new Bdd(config)
    bdd.run()
}