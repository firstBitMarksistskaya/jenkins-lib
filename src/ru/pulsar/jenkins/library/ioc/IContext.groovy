package ru.pulsar.jenkins.library.ioc

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration

interface IContext {
    IStepExecutor getStepExecutor()
    JobConfiguration getJobConfiguration()
    void registerJobConfiguration(JobConfiguration config)
}
