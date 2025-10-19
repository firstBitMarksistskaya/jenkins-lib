package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.edt.EdtCliEngineFactory
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger

class EdtValidate implements Serializable {

    public static final String RESULT_STASH = 'edt-validate'
    public static final String RESULT_FILE = 'build/out/edt-validate/edt-validate.out'

    private final JobConfiguration config

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

        def env = steps.env()

        String projectList

        if (config.sourceFormat == SourceFormat.DESIGNER) {
            steps.unstash(DesignerToEdtFormatTransformation.WORKSPACE_ZIP_STASH)
            steps.unzip(DesignerToEdtFormatTransformation.WORKSPACE, DesignerToEdtFormatTransformation.WORKSPACE_ZIP)

            def srcDir = config.srcDir
            def configurationRoot = FileUtils.getFilePath("$env.WORKSPACE/$srcDir")

            def projectName = configurationRoot.getName()

            projectList = "--project-name-list $projectName"
        } else {
            def srcDir = config.srcDir
            def projectDir = FileUtils.getFilePath("$env.WORKSPACE/$srcDir")
            projectList = "--project-list \"$projectDir\""
        }

        Logger.println("Выполнение валидации EDT")

        def engine = EdtCliEngineFactory.getEngine(config.edtVersion)

        engine.edtValidate(steps, config, projectList)

        // * Каратаев Олег - Убрана отправка в артефакты, т.к. непонятен смысл наличия данных артефактов в сборке
        //steps.archiveArtifacts("$DesignerToEdtFormatTransformation.WORKSPACE/.metadata/.log")
        //steps.archiveArtifacts(RESULT_FILE)
        steps.stash(RESULT_STASH, RESULT_FILE)
    }
}
