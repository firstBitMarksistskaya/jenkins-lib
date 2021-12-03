import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.Swagger

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def swagger = new Swagger(config)
    swagger.run()
}

