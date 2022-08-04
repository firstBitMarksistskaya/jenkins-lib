import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.SendNotifications

def call(JobConfiguration config) {
    ContextRegistry.registerDefaultContext(this)

    def sendNotifications = new SendNotifications(config)
    sendNotifications.run()
}