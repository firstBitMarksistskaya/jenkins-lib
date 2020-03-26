package ru.pulsar.jenkins.library.ioc

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.StepExecutor

class DefaultContext implements IContext, Serializable {
    private steps

    DefaultContext(steps) {
        this.steps = steps
    }

    @Override
    IStepExecutor getStepExecutor() {
        return new StepExecutor(this.steps)
    }
}
