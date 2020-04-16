import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

def call() {
    ContextRegistry.registerDefaultContext(this)

    Logger.printLocation()
}
