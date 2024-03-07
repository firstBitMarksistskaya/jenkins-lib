package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Constants

class RingCommand implements Serializable {

    private String script
    private boolean returnStatus
    private boolean returnStdout

    RingCommand(String script) {
        this.script = script
        this.returnStatus = false
        this.returnStdout = true
    };

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def ringOpts = [Constants.DEFAULT_RING_OPTS]
        steps.withEnv(ringOpts) {
            String ringMessage = steps.cmd(script, returnStatus, returnStdout)
            if (ringMessage.contains("error")) {
                steps.error(ringMessage)
            }
        }
    }
}
