package ru.pulsar.jenkins.library.ioc

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.StepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration

class DefaultContext implements IContext, Serializable {
    private steps
    private JobConfiguration jobConfiguration

    DefaultContext(steps) {
        this.steps = steps
    }

    @Override
    IStepExecutor getStepExecutor() {
        return new StepExecutor(this.steps)
    }

    @Override
    JobConfiguration getJobConfiguration() {
        return jobConfiguration
    }

    @Override
    void registerJobConfiguration(JobConfiguration config) {
        jobConfiguration = config
    }
}
