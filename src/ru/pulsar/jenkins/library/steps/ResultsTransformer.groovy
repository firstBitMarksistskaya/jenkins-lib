package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.ResultsTransformerType
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger

import java.nio.file.Paths

class ResultsTransformer implements Serializable {

    public static final String RESULT_STASH = 'edt-issues'
    public static final String RESULT_FILE = 'build/out/edt-validate/edt-issues.json'

    private final JobConfiguration config

    ResultsTransformer(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        def env = steps.env()

        if (!config.stageFlags.edtValidate) {
            Logger.println("EDT validation is disabled. No transform is needed.")
            return
        }

        steps.unstash(EdtValidate.RESULT_STASH)

        ResultsTransformerType transformerType = config.resultsTransformOptions.transformer

        def edtValidateFile = "$env.WORKSPACE/$EdtValidate.RESULT_FILE"
        def srcDir
        if (config.sourceFormat == SourceFormat.DESIGNER) {
            srcDir = FileUtils.getFilePath("$env.WORKSPACE/$config.srcDir")
        } else {
            def src = Paths.get(config.srcDir, "src")
            srcDir = FileUtils.getFilePath("$env.WORKSPACE/$src")
        }

        if (transformerType == ResultsTransformerType.STEBI) {

            Logger.println("Конвертация результата EDT в Generic Issue с помощью stebi")

            def genericIssueFile = "$env.WORKSPACE/$RESULT_FILE"
            def genericIssuesFormat = config.resultsTransformOptions.genericIssueFormat.getValue()

            steps.cmd("stebi convert --Format $genericIssuesFormat -r $edtValidateFile $genericIssueFile $srcDir")

            if (config.resultsTransformOptions.removeSupport) {
                def supportLevel = config.resultsTransformOptions.supportLevel
                steps.cmd("stebi transform --Format $genericIssuesFormat --remove_support $supportLevel --src $srcDir $genericIssueFile")
            }

        } else {

            Logger.println("Конвертация результата EDT в Issues с помощью edt-ripper")

            srcDir = FileUtils.getFilePath("$env.WORKSPACE/$config.srcDir")

            def projectName = srcDir.getName()
            def srcDirExceptLast = srcDir.getParent()

            steps.cmd("edt-ripper parse $edtValidateFile $srcDirExceptLast $projectName $env.WORKSPACE/$RESULT_FILE")

        }

        steps.stash(RESULT_STASH, RESULT_FILE)

        // Архивируем результат в отдельный архив и отправляем в артефакты.
        def resultDir = FileUtils.getFilePath("$edtValidateFile").getParent()
        String archivePath = "edt-validate-ResultsTransformer.zip"
        Boolean archiveArtifacts = true
        steps.zip("$resultDir", archivePath, '', archiveArtifacts)

    }
}
