package ru.pulsar.jenkins.library.edt

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.InitExtensionMethod
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.steps.DesignerToEdtFormatTransformation
import ru.pulsar.jenkins.library.steps.EdtToDesignerFormatTransformation
import ru.pulsar.jenkins.library.steps.EdtValidate
import ru.pulsar.jenkins.library.utils.EDT
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger

class RingConverter implements IEdtCliEngine {

    @Override
    void edtToDesignerTransformConfiguration(IStepExecutor steps, JobConfiguration config) {

        def env = steps.env()
        def edtVersionForRing = EDT.ringModule(config)
        def srcDir = config.srcDir

        String workspaceDir = FileUtils.getFilePath("$env.WORKSPACE/$WORKSPACE").getRemote()
        String projectWorkspaceDir = FileUtils.getFilePath("$workspaceDir/cf").getRemote()
        String projectDir = FileUtils.getFilePath("$env.WORKSPACE/$srcDir").getRemote()
        String configurationRoot = FileUtils.getFilePath("$env.WORKSPACE/$EdtToDesignerFormatTransformation.CONFIGURATION_DIR").getRemote()

        Logger.println("Конвертация исходников конфигурации из формата EDT в формат Конфигуратора с помощью ring")

        steps.deleteDir(configurationRoot)

        def ringCommand = "ring $edtVersionForRing workspace export --workspace-location \"$projectWorkspaceDir\" --project \"$projectDir\" --configuration-files \"$configurationRoot\""
        steps.ringCommand(ringCommand)

    }

    @Override
    void edtToDesignerTransformExtensions(IStepExecutor steps, JobConfiguration config) {

        def env = steps.env()

        String workspaceDir = FileUtils.getFilePath("$env.WORKSPACE/$WORKSPACE").getRemote()
        def edtVersionForRing = EDT.ringModule(config)
        String extensionRoot = FileUtils.getFilePath("$env.WORKSPACE/$EdtToDesignerFormatTransformation.EXTENSION_DIR").getRemote()

        config.initInfoBaseOptions.extensions.each {

            if (it.initMethod != InitExtensionMethod.SOURCE) {
                return
            }

            Logger.println("Конвертация исходников расширения ${it.name} из формата EDT в формат Конфигуратора с помощью ring")


            def projectDir = FileUtils.getFilePath("$env.WORKSPACE/${it.path}")
            def currentExtensionWorkspaceDir = FileUtils.getFilePath("$workspaceDir/cfe/${it.name}")
            def ringCommand = "ring $edtVersionForRing workspace export --workspace-location \"$currentExtensionWorkspaceDir\" --project \"$projectDir\" --configuration-files \"$extensionRoot/${it.name}\""
            steps.ringCommand(ringCommand)
        }
    }

    @Override
    void designerToEdtTransform(IStepExecutor steps, JobConfiguration config) {

        def env = steps.env()

        def workspaceDir = FileUtils.getFilePath("$env.WORKSPACE/$WORKSPACE")
        def srcDir = config.srcDir
        def configurationRoot = FileUtils.getFilePath("$env.WORKSPACE/$srcDir")
        def projectName = configurationRoot.getName()

        steps.deleteDir(workspaceDir)
        steps.deleteDir(configurationRoot)

        Logger.println("Конвертация исходников из формата конфигуратора в формат EDT с помощью ring")

        String edtVersionForRing = EDT.ringModule(config)
        String ringCommand = "ring $edtVersionForRing workspace import --configuration-files \"$configurationRoot\" --project-name $projectName --workspace-location \"$workspaceDir\""

        steps.ringCommand(ringCommand)

    }

    @Override
    void edtValidate(IStepExecutor steps, JobConfiguration config, String projectList) {

        def env = steps.env()

        def edtVersionForRing = EDT.ringModule(config)
        String workspaceLocation = "$env.WORKSPACE/$DesignerToEdtFormatTransformation.WORKSPACE"

        def resultFile = "$env.WORKSPACE/$EdtValidate.RESULT_FILE"

        Logger.println("Версия EDT меньше 2024.1.X, для валидации используется ring")

        def ringCommand = "ring $edtVersionForRing workspace validate --workspace-location \"$workspaceLocation\" --file \"$resultFile\" $projectList"
        steps.catchError {
            steps.ringCommand(ringCommand)
        }

    }
}
