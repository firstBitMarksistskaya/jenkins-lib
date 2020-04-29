package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class EdtValidate implements Serializable {

    public static final String RESULT_STASH = 'edt-validate'
    public static final String RESULT_FILE = 'build/out/edt-validate.out'

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
            Logger.println("EDT validate step is disabled")
            return
        }

        steps.unstash(EdtTransform.WORKSPACE_ZIP_STASH)
        steps.unzip(EdtTransform.WORKSPACE, EdtTransform.WORKSPACE_ZIP)

        def env = steps.env();

        def resultFile = "$env.WORKSPACE/$RESULT_FILE"
        def workspaceLocation = "$env.WORKSPACE/$EdtTransform.WORKSPACE"

        steps.createDir(new File(resultFile).getParent())

        Logger.println("Выполнение валидации EDT")

        def ringCommand = "ring edt workspace validate --workspace-location '$workspaceLocation' --file '$resultFile' --project-name-list $EdtTransform.PROJECT_NAME"
        def ringOpts = ['RING_OPTS=-Dfile.encoding=UTF-8 -Dosgi.nl=ru -Duser.language=ru -Xmx2g']
        steps.withEnv(ringOpts) {
            steps.catchError {
                steps.cmd(ringCommand)
            }
        }
        steps.zip(EdtTransform.WORKSPACE, "temp.workspace.zip")
        steps.archiveArtifacts("temp.workspace.zip")
        steps.archiveArtifacts("$EdtTransform.WORKSPACE/.metadata/.log")
        steps.archiveArtifacts(RESULT_FILE)
        steps.stash(RESULT_STASH, RESULT_FILE)
    }
}
