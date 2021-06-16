import ru.pulsar.jenkins.library.steps.Cmd
import ru.pulsar.jenkins.library.ioc.ContextRegistry

int call(String script, boolean returnStatus = false) {
    ContextRegistry.registerDefaultContext(this)

    Cmd cmd = new Cmd(script, returnStatus)
    return cmd.run()
}
