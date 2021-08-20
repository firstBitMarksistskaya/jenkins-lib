package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VersionParser

class SonarScanner implements Serializable {

    private final JobConfiguration config;
    private String rootFile

    SonarScanner(JobConfiguration config) {
        this.config = config
        this.rootFile = "$config.srcDir/Configuration.xml"
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.sonarqube) {
            steps.echo("SonarQube step is disabled")
            return
        }

        def env = steps.env();

        def sonarScannerBinary

        if (config.sonarQubeOptions.useSonarScannerFromPath) {
            sonarScannerBinary = "sonar-scanner"
        } else {
            String scannerHome = steps.tool(config.sonarQubeOptions.sonarScannerToolName)
            sonarScannerBinary = "$scannerHome/bin/sonar-scanner"
        }

        String sonarCommand = "$sonarScannerBinary -Dsonar.branch.name=$env.BRANCH_NAME"

        String configurationVersion = ""
        if (config.stageFlags.srcEDT) {
            rootFile = "$config.srcDir/Configuration/Configuration.mdo"
            configurationVersion = VersionParser.edt(rootFile)
        }else {
            configurationVersion = VersionParser.configuration(rootFile)
        }
        
        if (configurationVersion) {
            sonarCommand += " -Dsonar.projectVersion=$configurationVersion"
        }

        if (config.stageFlags.edtValidate) {
            steps.unstash("edt-generic-issue")
            sonarCommand += " -Dsonar.externalIssuesReportPaths=build/out/edt-generic-issue.json"
        }

        def sonarQubeInstallation = config.sonarQubeOptions.sonarQubeInstallation
        if (sonarQubeInstallation == '') {
            sonarQubeInstallation = null
        }

        steps.withSonarQubeEnv(sonarQubeInstallation) {
            steps.cmd(sonarCommand)
        }
    }
}
