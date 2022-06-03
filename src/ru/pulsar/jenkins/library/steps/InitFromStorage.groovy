package ru.pulsar.jenkins.library.steps

import com.cloudbees.groovy.cps.NonCPS
import org.jenkinsci.plugins.workflow.support.actions.EnvironmentAction
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.Secrets
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.RepoUtils
import ru.pulsar.jenkins.library.utils.VRunner
import ru.pulsar.jenkins.library.utils.VersionParser

import static ru.pulsar.jenkins.library.configuration.Secrets.UNKNOWN_ID

class InitFromStorage implements Serializable {

    private final JobConfiguration config

    InitFromStorage(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (config.infoBaseFromFiles()) {
            Logger.println("init infoBase from storage is disabled")
            return
        }

        steps.installLocalDependencies()

        String storageVersion = VersionParser.storage()
        String storageVersionParameter = storageVersion == "" ? "" : "--storage-ver $storageVersion"

        String repoSlug = RepoUtils.getRepoSlug()

        Secrets secrets = config.secrets

        String storageCredentials = secrets.storage == UNKNOWN_ID ? repoSlug + "_STORAGE_USER" : secrets.storage
        String storagePath = secrets.storagePath == UNKNOWN_ID ? repoSlug + "_STORAGE_PATH" : secrets.storagePath

        steps.withCredentials([
            steps.usernamePassword(
                storageCredentials,
                'RUNNER_STORAGE_USER',
                'RUNNER_STORAGE_PWD'
            ),
            steps.string(
                storagePath,
                'RUNNER_STORAGE_NAME'
            )
        ]) {
            String vrunnerPath = VRunner.getVRunnerPath()
            String base = config.baseName()
            VRunner.exec "$vrunnerPath init-dev --storage $storageVersionParameter --ibconnection \"$base\""
        }
    }

}
