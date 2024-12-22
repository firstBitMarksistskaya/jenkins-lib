import ru.pulsar.jenkins.library.configuration.JobConfiguration

def call(JobConfiguration config) {

    def archiveName = '1Cv8.1CD.zip'

    zip dir: 'build/ib', glob: '1Cv8.1CD', zipFile: archiveName, archive: config.initInfoBaseOptions.archiveInfobase
    stash name: archiveName, includes: archiveName, allowEmpty: false

}
