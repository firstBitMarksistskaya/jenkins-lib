package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class EdtValidate implements Serializable {

    private final JobConfiguration config;
    private final String rootDir

    EdtValidate(JobConfiguration config, String rootDir = 'src/cf') {
        this.config = config
        this.rootDir = rootDir
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.edtValidate) {
            steps.echo("EDT validate step is disabled")
            return
        }

        def env = steps.env();

        def projectDir = "$env.WORKSPACE/build/project"
        def workspaceDir = "$env.WORKSPACE/build/workspace"
        def resultFile = "$env.WORKSPACE/build/edt-validate.xml"
        def configurationRoot = new File(env.WORKSPACE, rootDir).getAbsolutePath()

        steps.createDir(projectDir)
        steps.createDir(workspaceDir)
        steps.createDir(new File(resultFile).getParent())


        Logger.println("Конвертация исходников из формата конфигуратора в формат EDT")

        def ringCommand = "ring edt workspace import --configuration-files '$configurationRoot' --project '$projectDir' --workspace-location '$workspaceDir'"
        
        steps.withEnv(['RING_OPTS="-Dfile.encoding=UTF-8 -Dosgi.nl=ru -Duser.language=ru"']) {
            steps.cmd(ringCommand)
        }

        Logger.println("Выполнение валидации EDT")

        ringCommand = "ring edt workspace validate --workspace-location '$workspaceDir' --file '$resultFile' --project '$projectDir'"

        steps.withEnv(['RING_OPTS="-Dfile.encoding=UTF-8 -Dosgi.nl=ru -Duser.language=ru"']) {
            steps.cmd(ringCommand)
        }
    }
}
