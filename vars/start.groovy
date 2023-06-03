import ru.pulsar.jenkins.library.steps.Start
import ru.pulsar.jenkins.library.ioc.ContextRegistry

void call(String script, boolean returnStatus = false) {
    ContextRegistry.registerDefaultContext(this)

    Start start = new Start(script, returnStatus)
    start.run()
}
