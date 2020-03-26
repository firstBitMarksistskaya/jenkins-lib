package ru.pulsar.jenkins.library.ioc

import ru.pulsar.jenkins.library.IStepExecutor

interface IContext {
    IStepExecutor getStepExecutor()
}