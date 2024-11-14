package ru.pulsar.jenkins.library.edt

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.InitExtensionMethod
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.steps.DesignerToEdtFormatTransformation
import ru.pulsar.jenkins.library.steps.EdtToDesignerFormatTransformation
import ru.pulsar.jenkins.library.steps.EdtValidate
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger

class NativeEdtCliConverter implements IEdtCliEngine {

    @Override
    void edtToDesignerTransformConfiguration(IStepExecutor steps, JobConfiguration config) {

        def env = steps.env()

        String workspaceDir = FileUtils.getFilePath("$env.WORKSPACE/$EdtToDesignerFormatTransformation.WORKSPACE").getRemote()
        String projectWorkspaceDir = FileUtils.getFilePath("$workspaceDir/cf").getRemote()
        def configurationRoot = FileUtils.getFilePath("$env.WORKSPACE/$EdtToDesignerFormatTransformation.CONFIGURATION_DIR")
        String configurationRootFullPath = configurationRoot.getRemote()

        Logger.println("Конвертация исходников конфигурации из формата EDT в формат Конфигуратора с помощью 1cedtcli")

        steps.deleteDir(configurationRoot)

        def projectName = configurationRoot.getName()
        def edtcliCommand = "1cedtcli -data \"$projectWorkspaceDir\" -command export --configuration-files \"$configurationRootFullPath\" --project-name \"$projectName\""

        steps.cmd(edtcliCommand)

    }

    @Override
    void edtToDesignerTransformExtensions(IStepExecutor steps, JobConfiguration config) {

        def env = steps.env()

        String workspaceDir = FileUtils.getFilePath("$env.WORKSPACE/$EdtToDesignerFormatTransformation.WORKSPACE").getRemote()
        String extensionRoot = FileUtils.getFilePath("$env.WORKSPACE/$EdtToDesignerFormatTransformation.EXTENSION_DIR").getRemote()

        config.initInfoBaseOptions.extensions.each {

            if (it.initMethod != InitExtensionMethod.SOURCE) {
                return
            }

            Logger.println("Конвертация исходников расширения ${it.name} из формата EDT в формат Конфигуратора с помощью 1cedtcli")
            def currentExtensionWorkspaceDir = FileUtils.getFilePath("$workspaceDir/cfe/${it.name}")

            def edtcliCommand = "1cedtcli -data \"$currentExtensionWorkspaceDir\" -command export --configuration-files \"$extensionRoot/${it.name}\" --project-name ${it.name}"

            steps.cmd(edtcliCommand)

        }

    }

    @Override
    void designerToEdtTransform(IStepExecutor steps, JobConfiguration config) {

        def env = steps.env()

        def workspaceDir = FileUtils.getFilePath("$env.WORKSPACE/$DesignerToEdtFormatTransformation.WORKSPACE")
        def srcDir = config.srcDir
        def configurationRoot = FileUtils.getFilePath("$env.WORKSPACE/$srcDir")
        def projectName = configurationRoot.getName()

        steps.deleteDir(workspaceDir)

        Logger.println("Конвертация исходников из формата конфигуратора в формат EDT с помощью 1cedtcli")

        def edtcliCommand = "1cedtcli -data \"$workspaceDir\" -command import --configuration-files \"$configurationRoot\" --project-name \"$projectName\""

        steps.cmd(edtcliCommand)

    }

    @Override
    void edtValidate(IStepExecutor steps, JobConfiguration config, String projectList) {

        def env = steps.env()

        String workspaceLocation = "$env.WORKSPACE/$DesignerToEdtFormatTransformation.WORKSPACE"
        def resultFile = "$env.WORKSPACE/$EdtValidate.RESULT_FILE"

        def edtcliCommand = "1cedtcli -data \"$workspaceLocation\" -command validate --file \"$resultFile\" $projectList"
        steps.catchError {
            steps.cmd(edtcliCommand)
        }

    }
}
