import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.email.EmailExtConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.EmailNotification

def call(JobConfiguration config, EmailExtConfiguration options) {
    ContextRegistry.registerDefaultContext(this)

    def emailNotification = new EmailNotification(config, options)
    emailNotification.run()
}