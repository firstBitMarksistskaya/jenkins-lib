package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class Logger implements Serializable {
    static void printLocation() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def env = System.getenv();
        steps.echo("Running on node $env.NODE_NAME")
    }
}
