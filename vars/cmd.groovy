import ru.pulsar.jenkins.library.steps.Cmd
import ru.pulsar.jenkins.library.ioc.ContextRegistry

def call(String script, boolean returnStatus = false, boolean returnStdout = false ) {
    ContextRegistry.registerDefaultContext(this)

    Cmd cmd = new Cmd(script, returnStatus, returnStdout)
    return cmd.run()
}
