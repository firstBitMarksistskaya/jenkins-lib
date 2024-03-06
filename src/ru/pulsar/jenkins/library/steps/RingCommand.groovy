package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class RingCommand implements Serializable {

    private String script;

    RingCommand(String script) {
        this.script = script
    };

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String ringMessage = steps.cmd(script, false, true)
        if (ringMessage.contains("error")) {
            steps.error(ringMessage)
        }
    }
}
