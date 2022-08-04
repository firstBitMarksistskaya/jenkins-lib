package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Constants
import ru.pulsar.jenkins.library.utils.EDT
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
        def srcExtDir = config.srcExtDir
        def extPrefix = "$EdtToDesignerFormatTransformation.EXT_PATH_PREFIX"
        def extSuffix = "$EdtToDesignerFormatTransformation.EXT_PATH_SUFFIX"
        def resStash = "$RESULT_STASH"
        def resFileExt = "$RESULT_FILE"

        String workspaceLocation = "$env.WORKSPACE/$DesignerToEdtFormatTransformation.WORKSPACE"
        String workspaceExtLocation
        String workspaceExtProject = "$DesignerToEdtFormatTransformation.WORKSPACE"
        String projectList
        String resultFileExt
        
        if (config.sourceFormat == SourceFormat.DESIGNER) {
            steps.unstash(DesignerToEdtFormatTransformation.WORKSPACE_ZIP_STASH)
            steps.unzip(DesignerToEdtFormatTransformation.WORKSPACE, DesignerToEdtFormatTransformation.WORKSPACE_ZIP)

            projectList = "--project-name-list $DesignerToEdtFormatTransformation.PROJECT_NAME"
        } else {
            String projectDir = new File("$env.WORKSPACE/$config.srcDir").getCanonicalPath()
            projectList = "--project-list \"$projectDir\""
        }

        def resultFile = "$env.WORKSPACE/$RESULT_FILE"
        def edtVersionForRing = EDT.ringModule(config)

        Logger.println("Выполнение валидации EDT")

        def ringCommand = "ring $edtVersionForRing workspace validate --workspace-location \"$workspaceLocation\" --file \"$resultFile\" $projectList"
        def ringOpts = [Constants.DEFAULT_RING_OPTS]
        steps.withEnv(ringOpts) {
            steps.catchError {
                steps.cmd(ringCommand)
            }
        }

        steps.archiveArtifacts("$DesignerToEdtFormatTransformation.WORKSPACE/.metadata/.log")
        steps.archiveArtifacts(RESULT_FILE)
        steps.stash(RESULT_STASH, RESULT_FILE)

        if (config.sourceFormat == SourceFormat.EDT) {
            srcExtDir.each{ 
                projectList = " --project-list $env.WORKSPACE/${it}" 
                resultFileExt = resultFile.replace(extPrefix,"$extPrefix-$extSuffix${it}")
                workspaceExtLocation = workspaceLocation.replace(extPrefix,"$extPrefix-$extSuffix${it}")
                Logger.println("Выполнение валидации EDT расширения ${it}")    
                ringCommand = "ring edt workspace validate --workspace-location \"$workspaceExtLocation\" --file \"$resultFileExt\" $projectList"                
                steps.withEnv(ringOpts) {
                    steps.catchError {
                        steps.cmd(ringCommand)
                    }
                }           
                steps.archiveArtifacts(workspaceExtProject.replace(extPrefix,"$extPrefix-$extSuffix${it}") + "/.metadata/.log")
                steps.archiveArtifacts(resFileExt.replace(extPrefix,"$extPrefix-$extSuffix${it}"))
                steps.stash("$resStash${it}", resFileExt.replace(extPrefix,"$extPrefix-$extSuffix${it}"))
            } 
        }

    }
}
