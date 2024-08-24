import ru.pulsar.jenkins.library.steps.Start
import ru.pulsar.jenkins.library.ioc.ContextRegistry

void call(String script) {
    ContextRegistry.registerDefaultContext(this)

    Start start = new Start(script)
    start.run()
}
