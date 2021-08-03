package ru.pulsar.jenkins.library.steps


import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class EdtBackTransform implements Serializable {

    public static final String PROJECT_NAME = 'pb17' // TODO Брать из srcDir
    public static final String WORKSPACE = 'build/edt-workspace'
    public static final String CONFIGURATION_ZIP = 'build/cfgPath.zip'
    public static final String CONFIGURATION_ZIP_STASH = 'cfgPath-zip'

    private final JobConfiguration config;

    EdtBackTransform(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.srcEDT) {
            Logger.println("SRC is not EDT format. No transform is needed.")
            return
        }

        def env = steps.env();

        def workspaceDir = "$env.WORKSPACE/$WORKSPACE"
        def configurationRoot = config.srcDir

        steps.createDir(workspaceDir)

        Logger.println("Конвертация исходников из формата EDT в формат Конфигуратора")

        def ringCommand = "ring edt workspace export --configuration-files '$configurationRoot' --project-name $PROJECT_NAME --workspace-location '$workspaceDir'"

        def ringOpts = ['RING_OPTS=-Dfile.encoding=UTF-8 -Dosgi.nl=ru -Duser.language=ru']
        steps.withEnv(ringOpts) {
            steps.cmd(ringCommand)
        }

        steps.zip(WORKSPACE, CONFIGURATION_ZIP)
        steps.stash(CONFIGURATION_ZIP_STASH, CONFIGURATION_ZIP)
    }

}
