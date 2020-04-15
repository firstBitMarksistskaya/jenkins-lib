import ru.pulsar.jenkins.library.configuration.JobConfiguration

def call(JobConfiguration jobConfiguration, String storageVersion) {
    withCredentials([
        usernamePassword(
            credentialsId: jobConfiguration.secrets.storage,
            passwordVariable: 'STORAGE_PSW',
            usernameVariable: 'STORAGE_USR'
        ),
        string(
            credentialsId: jobConfiguration.secrets.storagePath,
            variable: 'STORAGE_PATH'
        )
    ]) {
        cmd "oscript_modules/bin/vrunner init-dev --storage --storage-name $STORAGE_PATH --storage-user $STORAGE_USR --storage-pwd $STORAGE_PSW --storage-ver $storageVersion --ibconnection \"/F./build/ib\""
    }
}