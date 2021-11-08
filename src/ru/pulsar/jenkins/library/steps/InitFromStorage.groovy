package ru.pulsar.jenkins.library.steps


import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner
import ru.pulsar.jenkins.library.utils.VersionParser

class InitFromStorage implements Serializable {

    private final JobConfiguration config;

    InitFromStorage(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (config.infobaseFromFiles()) {
            Logger.println("init infoBase from storage is disabled")
            return
        }

        steps.installLocalDependencies();

        def storageVersion = VersionParser.storage()
        def storageVersionParameter = storageVersion == "" ? "" : "--storage-ver $storageVersion"

        steps.withCredentials([
            steps.usernamePassword(
                config.secrets.storage,
                'STORAGE_USR',
                'STORAGE_PSW'
            ),
            steps.string(
                config.secrets.storagePath,
                'STORAGE_PATH'
            )
        ]) {
            String vrunnerPath = VRunner.getVRunnerPath();
            steps.cmd "$vrunnerPath init-dev --storage --storage-name $STORAGE_PATH --storage-user $STORAGE_USR --storage-pwd $STORAGE_PSW $storageVersionParameter --ibconnection \"/F./build/ib\""
        }
    }
}
