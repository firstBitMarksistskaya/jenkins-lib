import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.utils.VersionParser

def call(JobConfiguration jobConfiguration) {

    def storageVersion = VersionParser.storage()
    def storageVersionParameter = storageVersion == "" ? "" : "--storage-ver $storageVersion"

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
        cmd "oscript_modules/bin/vrunner init-dev --storage --storage-name $STORAGE_PATH --storage-user $STORAGE_USR --storage-pwd $STORAGE_PSW $storageVersionParameter --ibconnection \"/F./build/ib\""
    }
}