import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.RemoveStageExtensions

def call(JobConfiguration config, String stageName) {
    ContextRegistry.registerDefaultContext(this)

    def removeStageExtensions = new RemoveStageExtensions(config, stageName)
    removeStageExtensions.run()
}
