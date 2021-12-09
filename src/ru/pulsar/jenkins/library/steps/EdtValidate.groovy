package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Constants
import ru.pulsar.jenkins.library.utils.Logger

class EdtValidate implements Serializable {

    public static final String RESULT_STASH = 'edt-validate'
    public static final String RESULT_FILE = 'build/out/edt-validate.out'

    private final JobConfiguration config;

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

        def env = steps.env();

        String workspaceLocation = "$env.WORKSPACE/$DesignerToEdtFormatTransformation.WORKSPACE"
        String projectList;

        if (config.sourceFormat == SourceFormat.DESIGNER) {
            steps.unstash(DesignerToEdtFormatTransformation.WORKSPACE_ZIP_STASH)
            steps.unzip(DesignerToEdtFormatTransformation.WORKSPACE, DesignerToEdtFormatTransformation.WORKSPACE_ZIP)

            projectList = "--project-name-list $DesignerToEdtFormatTransformation.PROJECT_NAME"
        } else {
            String projectDir = new File("$env.WORKSPACE/$config.srcDir").getCanonicalPath()
            projectList = "--project-list '$projectDir'"
        }

        def resultFile = "$env.WORKSPACE/$RESULT_FILE"

        Logger.println("Выполнение валидации EDT")

        def ringCommand = "ring edt workspace validate --workspace-location \"$workspaceLocation\" --file \"$resultFile\" $projectList"
        def ringOpts = [Constants.DEFAULT_RING_OPTS]
        steps.withEnv(ringOpts) {
            steps.catchError {
                steps.cmd(ringCommand)
            }
        }

        steps.archiveArtifacts("$DesignerToEdtFormatTransformation.WORKSPACE/.metadata/.log")
        steps.archiveArtifacts(RESULT_FILE)
        steps.stash(RESULT_STASH, RESULT_FILE)
    }
}
