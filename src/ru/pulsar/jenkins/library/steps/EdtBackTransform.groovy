package ru.pulsar.jenkins.library.steps


import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class EdtBackTransform implements Serializable {

    public static final String WORKSPACE = 'build/workcspace'
    public static final String CONFIGURATION_DIR = 'build/cfg'
    public static final String CONFIGURATION_ZIP = 'build/cfg.zip'
    public static final String CONFIGURATION_ZIP_STASH = 'cfg-zip'

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

        def projectDir = config.srcDir
        def workspaceDir = "$env.WORKSPACE/$WORKSPACE" 
        def configurationRoot = "$env.WORKSPACE/$CONFIGURATION_DIR"

        steps.createDir(workspaceDir)
        steps.createDir(configurationRoot)

        Logger.println("Конвертация исходников из формата EDT в формат Конфигуратора")

        def ringCommand = "ring edt workspace export --configuration-files '$configurationRoot' --project $projectDir --workspace-location '$workspaceDir'"

        def ringOpts = ['RING_OPTS=-Dfile.encoding=UTF-8 -Dosgi.nl=ru -Duser.language=ru']
        steps.withEnv(ringOpts) {
            steps.cmd(ringCommand)
        }

        steps.zip(CONFIGURATION_DIR, CONFIGURATION_ZIP)
        steps.stash(CONFIGURATION_ZIP_STASH, CONFIGURATION_ZIP)
    }

}