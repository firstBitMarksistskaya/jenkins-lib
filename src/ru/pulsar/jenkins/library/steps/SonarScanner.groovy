package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VersionParser

class SonarScanner implements Serializable {

    private final JobConfiguration config;
    private final String rootFile

    SonarScanner(JobConfiguration config) {
        this.config = config

        String pathToParent
        String pathToModule
        String nameOfModule = config.sonarQubeOptions.sonarScannerPathNameModule

        if (config.sourceFormat == SourceFormat.EDT) {
            pathToParent = "$config.srcDir/src/Configuration/Configuration.mdo"
            pathToModule = "$config.srcDir/src/CommonModules/$nameOfModule/Module.bsl"
        }
        else {
            pathToParent = "$config.srcDir/Configuration.xml"
            pathToModule = "$config.srcDir/CommonModules/$nameOfModule/Ext/Module.bsl"
        }

        if (nameOfModule.isEmpty()) {
            this.rootFile = pathToParent
        }
        else {
            this.rootFile = pathToModule
        }
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

        String configurationVersion
        if (!config.sonarQubeOptions.sonarScannerPathNameModule.isEmpty()){
            configurationVersion = VersionParser.ssl(rootFile)
        }
        else if (config.sourceFormat == SourceFormat.EDT) {
            configurationVersion = VersionParser.edt(rootFile)
        } else {
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
