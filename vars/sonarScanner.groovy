import com.cloudbees.groovy.cps.NonCPS
import groovy.transform.Field
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.utils.VersionParser

@Field def sonarCommand

def call(String rootFile = 'src/cf/Configuration.xml') {

    def config = jobConfiguration() as JobConfiguration

    String scannerHome = tool config.sonarScannerToolName
    sonarCommand = "$scannerHome/bin/sonar-scanner -Dsonar.branch.name=$env.BRANCH_NAME"

    String configurationVersion = VersionParser.configuration(rootFile)
    if (configurationVersion) {
        sonarCommand += " -Dsonar.projectVersion=$configurationVersion"
    }

    withSonarQubeEnv('qa.dev.pulsar.ru') {
        cmd sonarCommand
    }
}

@NonCPS
private static String version(String text) {
    def matcher = text =~ /<Version>(.*)<\/Version>/
    return matcher ? matcher.group(1) : ""
}