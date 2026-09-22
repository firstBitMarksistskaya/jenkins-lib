import ru.pulsar.jenkins.library.configuration.ConfigurationReader
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry

JobConfiguration call(String path = "jobConfiguration.json") {
    ContextRegistry.registerDefaultContext(this)

    if (fileExists(path)) {
        def config = readFile(path)
        return ContextRegistry.registerJobConfiguration(ConfigurationReader.create(config))
    } else {
        return ContextRegistry.registerJobConfiguration(ConfigurationReader.create())
    }

}
