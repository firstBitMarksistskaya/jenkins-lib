package ru.pulsar.jenkins.library.utils

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class Logger implements Serializable {
    static void printLocation() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def env = steps.env();
        steps.echo("Running on node $env.NODE_NAME")
    }

    static void println(String message) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        steps.echo(message)
    }
}
