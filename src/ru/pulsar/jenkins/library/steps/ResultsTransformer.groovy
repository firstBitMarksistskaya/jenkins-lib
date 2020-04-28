package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class ResultsTransformer implements Serializable {

    private final JobConfiguration config;
    private final String rootDir

    ResultsTransformer(JobConfiguration config, String rootDir = 'src/cf') {
        this.config = config
        this.rootDir = rootDir
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

        steps.unstash('edt-validate')

        Logger.println("Конвертация результата EDT в Generic Issue")

        def genericIssueRelative = "build/out/edt-generic-issue.json"
        def edtValidateFile = "$env.WORKSPACE/build/out/edt-validate.xml"
        def genericIssueFile = "$env.WORKSPACE/$genericIssueRelative"

        steps.cmd("stebi convert $edtValidateFile $genericIssueFile $rootDir")

        steps.archiveArtifacts(genericIssueRelative)
        steps.stash('edt-generic-issue', genericIssueRelative)
    }
}
