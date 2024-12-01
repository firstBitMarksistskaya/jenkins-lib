package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.StepCoverageOptions
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.CoverageUtils

class WithCoverage implements Serializable {

    private final JobConfiguration config
    private final Coverable stage
    private final StepCoverageOptions coverageOptions
    private final Closure body

    WithCoverage(JobConfiguration config, Coverable stage, StepCoverageOptions coverageOptions, Closure body) {
        this.config = config
        this.stage = stage
        this.coverageOptions = coverageOptions
        this.body = body
    }

    def run() {

        def context = CoverageUtils.prepareContext(config, coverageOptions)

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        //noinspection GroovyMissingReturnStatement
        steps.lock(context.lockableResource) {
            if (coverageOptions.coverage) {
                CoverageUtils.startCoverage(steps, config, context, stage)
            }

            body()

            if (coverageOptions.coverage) {
                CoverageUtils.stopCoverage(steps, config, context)
            }
        }

        if (coverageOptions.coverage) {
            steps.stash(stage.getCoverageStashName(), stage.getCoverageStashPath(), true)
        }

    }
}
