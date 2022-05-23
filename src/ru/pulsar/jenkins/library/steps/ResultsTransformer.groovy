package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

import java.nio.file.Paths

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

        def env = steps.env();

        if (!config.stageFlags.edtValidate) {
            Logger.println("EDT validation is disabled. No transform is needed.")
            return
        }

        steps.unstash(EdtValidate.RESULT_STASH)

        Logger.println("Конвертация результата EDT в Generic Issue")

        def edtValidateFile = "$env.WORKSPACE/$EdtValidate.RESULT_FILE"
        def genericIssueFile = "$env.WORKSPACE/$RESULT_FILE"

        String srcDir = config.sourceFormat == SourceFormat.DESIGNER ? config.srcDir : Paths.get(config.srcDir, "src")
        steps.cmd("stebi convert -r $edtValidateFile $genericIssueFile $srcDir")

        if (config.resultsTransformOptions.removeSupport) {
            def supportLevel = config.resultsTransformOptions.supportLevel
            steps.cmd("stebi transform --remove_support $supportLevel --src $srcDir $genericIssueFile")
        }

        steps.archiveArtifacts(RESULT_FILE)
        steps.stash(RESULT_STASH, RESULT_FILE)
    }
}
