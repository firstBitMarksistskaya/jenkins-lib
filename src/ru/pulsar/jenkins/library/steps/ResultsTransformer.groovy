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
        def extPrefix = "$EdtToDesignerFormatTransformation.EXT_PATH_PREFIX"
        def extSuffix = "$EdtToDesignerFormatTransformation.EXT_PATH_SUFFIX"
        def srcExtDir = config.srcExtDir

        String srcDir = config.sourceFormat == SourceFormat.DESIGNER ? config.srcDir : Paths.get(config.srcDir, "src")
        steps.cmd("stebi convert $edtValidateFile $genericIssueFile $srcDir")

        if (config.resultsTransformOptions.removeSupport) {
            def supportLevel = config.resultsTransformOptions.supportLevel
            steps.cmd("stebi transform --remove_support $supportLevel --src $srcDir $genericIssueFile")
        }

        steps.archiveArtifacts(RESULT_FILE)
        steps.stash(RESULT_STASH, RESULT_FILE)

        if (config.sourceFormat == SourceFormat.EDT) {
            String edtResStah = EdtValidate.RESULT_STASH
            String edtValidateExtFile
            String genericIssueExtFile
            def exrResultFile = "$RESULT_FILE"

            srcExtDir.each{
                steps.unstash("$edtResStah${it}")
                edtValidateExtFile = edtValidateFile.replace(extPrefix,"$extPrefix/$extSuffix${it}")
                genericIssueExtFile = genericIssueFile.replace(extPrefix,"$extPrefix/$extSuffix${it}")
                srcDir = "${it}/src"
                Logger.println("Конвертация результата валидации расширения ${it} EDT в Generic Issue")
                steps.cmd("stebi convert $edtValidateExtFile $genericIssueExtFile $srcDir")

                if (config.resultsTransformOptions.removeSupport) {
                    def supportLevel = config.resultsTransformOptions.supportLevel
                    steps.cmd("stebi transform --remove_support $supportLevel --src $srcDir $genericIssueExtFile")
                }
                
                steps.archiveArtifacts(exrResultFile.replace(extPrefix,"$extPrefix/$extSuffix${it}"))
                steps.stash("$RESULT_STASH${it}", exrResultFile.replace(extPrefix,"$extPrefix/$extSuffix${it}"))

            } 
        }
    }
}
