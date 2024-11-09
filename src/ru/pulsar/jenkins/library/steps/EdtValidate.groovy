package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.EDT
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VersionParser

class EdtValidate implements Serializable {

    public static final String RESULT_STASH = 'edt-validate'
    public static final String RESULT_FILE = 'build/out/edt-validate.out'

    private final JobConfiguration config

    EdtValidate(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.edtValidate) {
            Logger.println("EDT validate step is disabled")
            return
        }

        def env = steps.env()

        String workspaceLocation = "$env.WORKSPACE/$DesignerToEdtFormatTransformation.WORKSPACE"
        String projectList

        if (config.sourceFormat == SourceFormat.DESIGNER) {
            steps.unstash(DesignerToEdtFormatTransformation.WORKSPACE_ZIP_STASH)
            steps.unzip(DesignerToEdtFormatTransformation.WORKSPACE, DesignerToEdtFormatTransformation.WORKSPACE_ZIP)

            def srcDir = config.srcDir
            def configurationRoot = FileUtils.getFilePath("$env.WORKSPACE/$srcDir")

            def projectName = configurationRoot.getName()

            projectList = "--project-name-list $projectName"
        } else {
            def srcDir = config.srcDir
            def projectDir = FileUtils.getFilePath("$env.WORKSPACE/$srcDir")
            projectList = "--project-list \"$projectDir\""
        }

        def resultFile = "$env.WORKSPACE/$RESULT_FILE"
        def edtVersionForRing = EDT.ringModule(config)

        Logger.println("Выполнение валидации EDT")

        if (VersionParser.compare(config.edtVersion, "2024") < 0) {

            Logger.println("Версия EDT меньше 2024.1.X, для валидации используется ring")

            def ringCommand = "ring $edtVersionForRing workspace validate --workspace-location \"$workspaceLocation\" --file \"$resultFile\" $projectList"
            steps.catchError {
                steps.ringCommand(ringCommand)
            }

        } else {

            Logger.println("Версия EDT больше 2024.1.X, для валидации используется 1cedtcli")

            def edtcliCommand = "1cedtcli -data \"$workspaceLocation\" -command validate --file \"$resultFile\" $projectList"
            steps.catchError {
                def stdOut = steps.cmd(edtcliCommand, false, true)
                Logger.println(stdOut)
            }

        }

        steps.archiveArtifacts("$DesignerToEdtFormatTransformation.WORKSPACE/.metadata/.log")
        steps.archiveArtifacts(RESULT_FILE)
        steps.stash(RESULT_STASH, RESULT_FILE)
    }
}
