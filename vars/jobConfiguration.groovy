import ru.pulsar.jenkins.library.configuration.ConfigurationReader
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry

JobConfiguration call(String path = "") {
    ContextRegistry.registerDefaultContext(this)

    def config = readFile(path)
    return ConfigurationReader.create(config)
}