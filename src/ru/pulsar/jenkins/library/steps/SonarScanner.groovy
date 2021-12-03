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
        if (config.sourceFormat == SourceFormat.EDT){
            this.rootFile = "$config.srcDir/src/Configuration/Configuration.mdo"
        } else {
            this.rootFile = "$config.srcDir/Configuration.xml"
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
        def extPrefix = "$EdtToDesignerFormatTransformation.EXT_PATH_PREFIX"
        def extSuffix = "$EdtToDesignerFormatTransformation.EXT_PATH_SUFFIX"

        if (config.sonarQubeOptions.useSonarScannerFromPath) {
            sonarScannerBinary = "sonar-scanner"
        } else {
            String scannerHome = steps.tool(config.sonarQubeOptions.sonarScannerToolName)
            sonarScannerBinary = "$scannerHome/bin/sonar-scanner"
        }

        String sonarCommand = "$sonarScannerBinary -Dsonar.branch.name=$env.BRANCH_NAME"
        String sonarAddComm = "build/out/edt-generic-issue.json"
        String configurationVersion
        if (config.sourceFormat == SourceFormat.EDT) {
            configurationVersion = VersionParser.edt(rootFile)
        } else {
            configurationVersion = VersionParser.configuration(rootFile)
        }
        
        if (configurationVersion) {
            sonarCommand += " -Dsonar.projectVersion=$configurationVersion"
        }

        if (config.stageFlags.edtValidate) {
            steps.unstash("edt-generic-issue")
            sonarCommand += " -Dsonar.externalIssuesReportPaths=$sonarAddComm"
            if (config.sourceFormat == SourceFormat.EDT) {
                srcExtDir.each{
                    sonarCommand += sonarAddComm.replace(extPrefix,"$extPrefix/$extSuffix${it}")
                }
            }
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
