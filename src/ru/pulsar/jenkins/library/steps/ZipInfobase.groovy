package ru.pulsar.jenkins.library.steps

import hudson.model.Result
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.ArchiveInfobaseOptions
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class ZipInfobase implements Serializable {

    private final JobConfiguration config
    private final String stage

    ZipInfobase(JobConfiguration config, String stage) {
        this.config = config
        this.stage = stage
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        def currentBuild = steps.currentBuild()
        def currentResult = Result.fromString(currentBuild.getCurrentResult())

        def archiveInfobaseOptions = getArchiveInfobaseOptionsForStage(config, stage)

        def archiveInfobase = false
        if (archiveInfobaseOptions.onAlways
                || (archiveInfobaseOptions.onFailure && (currentResult == Result.FAILURE || currentResult == Result.ABORTED))
                || (archiveInfobaseOptions.onUnstable && currentResult == Result.UNSTABLE)
                || (archiveInfobaseOptions.onSuccess && currentResult == Result.SUCCESS)) {
            archiveInfobase = true
        }

        def archiveName = "1Cv8.1CD.${stage}.zip"
        steps.zip('build/ib', '1Cv8.1CD', archiveName, archiveInfobase)
        steps.stash(archiveName, archiveName, false)
    }

    private static ArchiveInfobaseOptions getArchiveInfobaseOptionsForStage(JobConfiguration config, String stageName) {

        def defaultOptions = new ArchiveInfobaseOptions()
        if (!stageName) {
            return defaultOptions
        }

        try {
            return config."${stageName}Options".archiveInfobase
        } catch(Exception e) {
            Logger.println(e.message)
           return defaultOptions
        }
    }
}
