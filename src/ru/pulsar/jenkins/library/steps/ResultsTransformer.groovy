package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class ResultsTransformer implements Serializable {

    public static final String RESULT_STASH = 'edt-generic-issue'
    public static final String RESULT_FILE = 'build/out/edt-generic-issue.json'

    private final JobConfiguration config;

    ResultsTransformer(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.sonarqube) {
            Logger.println("No transform is needed.")
            return
        }

        def env = steps.env();

        if (!config.stageFlags.edtValidate) {
            Logger.println("EDT validation is disabled. No transform is needed.")
            return
        }

        steps.unstash(EdtTransform.WORKSPACE_ZIP_STASH)
        steps.unzip(EdtTransform.WORKSPACE_ZIP, EdtTransform.WORKSPACE)

        steps.unstash(EdtValidate.RESULT_STASH)

        Logger.println("Конвертация результата EDT в Generic Issue")

        def edtValidateFile = "$env.WORKSPACE/$EdtValidate.RESULT_FILE"
        def genericIssueFile = "$env.WORKSPACE/$RESULT_FILE"

        steps.cmd("stebi convert $edtValidateFile $genericIssueFile $EdtTransform.WORKSPACE")

        if (config.resultsTransformOptions.removeSupport) {
            def supportLevel = config.resultsTransformOptions.supportLevel
            steps.cmd("stebi transform --remove_support $supportLevel --src $EdtTransform.WORKSPACE $genericIssueFile")
        }

        steps.archiveArtifacts(RESULT_FILE)
        steps.stash(RESULT_STASH, RESULT_FILE)
    }
}
