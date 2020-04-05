import ru.pulsar.jenkins.library.configuration.JobConfiguration

def <T extends Closure> void call(JobConfiguration jobConfiguration, T body) {
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