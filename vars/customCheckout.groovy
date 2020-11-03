import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.Checkout

void call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def checkout = new Checkout(config)
    checkout.run()
}