package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VersionParser

class SonarScanner implements Serializable {

    private final JobConfiguration config;

    SonarScanner(JobConfiguration config) {
        this.config = config
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

        String projectVersion = computeProjectVersion()
        if (projectVersion) {
            sonarCommand += " -Dsonar.projectVersion=$projectVersion"
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

    private String computeProjectVersion() {
        String projectVersion
        String nameOfModule = config.sonarQubeOptions.infoBaseUpdateModuleName

        if (!nameOfModule.isEmpty()) {
            String rootFile
            if (config.sourceFormat == SourceFormat.EDT) {
                rootFile = "$config.srcDir/src/CommonModules/$nameOfModule/Module.bsl"
            } else {
                rootFile = "$config.srcDir/CommonModules/$nameOfModule/Ext/Module.bsl"
            }
            projectVersion = VersionParser.ssl(rootFile)
        } else if (config.sourceFormat == SourceFormat.EDT) {
            String rootFile = "$config.srcDir/src/Configuration/Configuration.mdo"
            projectVersion = VersionParser.edt(rootFile)
        } else {
            String rootFile = "$config.srcDir/Configuration.xml"
            projectVersion = VersionParser.configuration(rootFile)
        }

        return projectVersion
    }
}
