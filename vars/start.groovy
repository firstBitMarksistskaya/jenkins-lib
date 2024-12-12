import ru.pulsar.jenkins.library.steps.Start
import ru.pulsar.jenkins.library.ioc.ContextRegistry

void call(String executable, String params) {
    ContextRegistry.registerDefaultContext(this)

    Start start = new Start(executable, params)
    start.run()
}
