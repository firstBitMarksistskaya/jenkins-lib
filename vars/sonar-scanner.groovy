import com.cloudbees.groovy.cps.NonCPS
import groovy.transform.Field

@Field def sonarCommand

def call(String rootFile = 'src/cf/Configuration.xml') {

    String scannerHome = tool 'sonar-scanner'
    sonarCommand = "$scannerHome/bin/sonar-scanner -Dsonar.branch.name=$env.BRANCH_NAME"

    def configurationText = readFile encoding: 'UTF-8', file: rootFile
    String configurationVersion = version(configurationText)
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