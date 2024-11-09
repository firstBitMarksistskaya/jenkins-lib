package ru.pulsar.jenkins.library.steps


import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.EDT
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VersionParser

class DesignerToEdtFormatTransformation implements Serializable {

    public static final String WORKSPACE = 'build/edt-workspace'
    public static final String WORKSPACE_ZIP = 'build/edt-workspace.zip'
    public static final String WORKSPACE_ZIP_STASH = 'edt-workspace-zip'

    private final JobConfiguration config

    DesignerToEdtFormatTransformation(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.edtValidate) {
            Logger.println("EDT validate step is disabled. No transform is needed.")
            return
        }

        def env = steps.env()

        def workspaceDir = FileUtils.getFilePath("$env.WORKSPACE/$WORKSPACE")
        def srcDir = config.srcDir
        def configurationRoot = FileUtils.getFilePath("$env.WORKSPACE/$srcDir")
        
        steps.deleteDir(workspaceDir)

        Logger.println("Конвертация исходников из формата конфигуратора в формат EDT")

        if (VersionParser.compare(config.edtVersion, "2024") < 0) {

            Logger.println("Версия EDT меньше 2024.1.X, используется ring")

            def edtVersionForRing = EDT.ringModule(config)
            def ringCommand = "ring $edtVersionForRing workspace import --configuration-files \"$configurationRoot\" --project-name $projectName --workspace-location \"$workspaceDir\""

            steps.ringCommand(ringCommand)

        } else {

            Logger.println("Версия EDT больше 2024.1.X, используется 1cedtcli")

            def projectName = configurationRoot.getName()
            def edtcliCommand = "1cedtcli -data \"$workspaceDir\" -command import --configuration-files \"$configurationRoot\" --project-name $projectName"

            def stdOut = steps.cmd(edtcliCommand, false, true)

            Logger.println(stdOut)

        }

        steps.zip(WORKSPACE, WORKSPACE_ZIP)
        steps.stash(WORKSPACE_ZIP_STASH, WORKSPACE_ZIP)
    }

}
