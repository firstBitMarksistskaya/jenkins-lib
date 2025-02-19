import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.SyntaxCheck

def call(JobConfiguration config) {

    ContextRegistry.registerDefaultContext(this)

    def syntaxCheck = new SyntaxCheck(config)
    syntaxCheck.run()

}
