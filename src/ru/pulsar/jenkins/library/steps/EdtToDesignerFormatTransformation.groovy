package ru.pulsar.jenkins.library.steps


import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Constants
import ru.pulsar.jenkins.library.utils.EDT
import ru.pulsar.jenkins.library.utils.Logger

class EdtToDesignerFormatTransformation implements Serializable {

    public static final String EXT_PATH_PREFIX = 'build'
    public static final String WORKSPACE = 'build/edt-workspace'
    public static final String CONFIGURATION_DIR = 'build/cfg'
    public static final String CONFIGURATION_ZIP = 'build/cfg.zip'
    public static final String CONFIGURATION_ZIP_STASH = 'cfg-zip'

    private final JobConfiguration config;

    EdtToDesignerFormatTransformation(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (config.sourceFormat != SourceFormat.EDT) {
            Logger.println("SRC is not in EDT format. No transform is needed.")
            return
        }

        def env = steps.env();
 
        def srcDir = config.srcDir
        def projectDir = new File("$env.WORKSPACE/$srcDir").getCanonicalPath()
        def workspaceDir = "$env.WORKSPACE/$WORKSPACE" 
        def configurationRoot = "$env.WORKSPACE/$CONFIGURATION_DIR"
        def edtVersionForRing = EDT.ringModule(config)
        def extPrefix = "$EXT_PATH_PREFIX"

        steps.deleteDir(workspaceDir)
        steps.deleteDir(configurationRoot)

        String ringCommandExt
        String [] srcExtDir = config.srcExtDir.split(" ")

        Logger.println("Конвертация исходников из формата EDT в формат Конфигуратора")

        def ringCommand = "ring $edtVersionForRing workspace export --workspace-location \"$workspaceDir\" --project \"$projectDir\" --configuration-files \"$configurationRoot\""
        def ringStr = Constants.DEFAULT_RING_OPTS
        if (config.ringMemory != "4g") {
            ringStr.replace("-Xmx4g","-Xmx" + config.ringMemory)  
        }
        def ringOpts = [ringStr]
        steps.withEnv(ringOpts) {
            steps.cmd(ringCommand)
            steps.zip(CONFIGURATION_DIR, CONFIGURATION_ZIP)
            steps.stash(CONFIGURATION_ZIP_STASH, CONFIGURATION_ZIP)
            if (config.initInfoBaseOptions.saveXMLartifacts) {
                steps.archiveArtifacts(CONFIGURATION_ZIP)
            } 
            for (String ext : srcExtDir) {
                String workspaceExtDir = "$env.WORKSPACE/$extPrefix-$ext/edt-workspace"
                String projectExtDir = new File("$env.WORKSPACE/$ext").getCanonicalPath()
                String configurationExtDir = "$extPrefix-$ext/$ext-cfg"
                String configurationExtRoot = "$env.WORKSPACE/$configurationExtDir"
                String configurationExtZip = "$extPrefix-$ext/$ext-cfg.zip" 
                ringCommandExt = "ring $edtVersionForRing workspace export --workspace-location \"$workspaceExtDir\" --project \"$projectExtDir\" --configuration-files \"$configurationExtRoot\""
                
                steps.deleteDir(workspaceExtDir)
                steps.deleteDir(configurationExtRoot)

                Logger.println("Конвертация исходников расширения $ext из формата EDT в формат Конфигуратора")                
                steps.cmd(ringCommandExt)
                
                steps.zip(configurationExtDir, configurationExtZip)
                steps.stash("$ext-$CONFIGURATION_ZIP_STASH", configurationExtZip)
                if (config.initInfoBaseOptions.saveXMLartifacts) {
                    steps.archiveArtifacts(configurationExtZip)
                }
            }  
        }      
    }
}
