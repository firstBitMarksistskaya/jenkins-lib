import ru.pulsar.jenkins.library.configuration.JobConfiguration

def call(JobConfiguration jobConfiguration, Closure body) {
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
        body()
    }
}