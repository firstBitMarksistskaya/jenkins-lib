import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.RingCommand

def call(String script ) {
    ContextRegistry.registerDefaultContext(this)

    RingCommand ringCommand = new RingCommand(script)
    return ringCommand.run()
}