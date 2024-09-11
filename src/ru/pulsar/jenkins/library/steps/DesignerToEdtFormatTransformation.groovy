package ru.pulsar.jenkins.library.steps


import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.EDT
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger

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
        def projectName = configurationRoot.getName()
        def edtVersionForRing = EDT.ringModule(config)
        
        steps.deleteDir(workspaceDir)

        Logger.println("Конвертация исходников из формата конфигуратора в формат EDT")

        def ringCommand = "ring $edtVersionForRing workspace import --configuration-files \"$configurationRoot\" --project-name $projectName --workspace-location \"$workspaceDir\""

        steps.ringCommand(ringCommand)

        steps.zip(WORKSPACE, WORKSPACE_ZIP)
        steps.stash(WORKSPACE_ZIP_STASH, WORKSPACE_ZIP)
    }

}
