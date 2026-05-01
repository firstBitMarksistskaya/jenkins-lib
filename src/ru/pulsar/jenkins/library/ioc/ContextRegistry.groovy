package ru.pulsar.jenkins.library.ioc

import ru.pulsar.jenkins.library.configuration.JobConfiguration

class ContextRegistry implements Serializable {
    private static IContext context
    private static JobConfiguration jobConfiguration

    static void registerContext(IContext context) {
        ContextRegistry.context = context
    }

    static void registerDefaultContext(Object steps) {
        context = new DefaultContext(steps)
    }

    static JobConfiguration registerJobConfiguration(JobConfiguration config) {
        jobConfiguration = config
        return config
    }

    static IContext getContext() {
        return context
    }

    static JobConfiguration getJobConfiguration() {
        return jobConfiguration
    }
}
