package ru.pulsar.jenkins.library.steps

import groovy.io.FileType
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.OscriptModules

class Swagger {
    public static final String OUT = 'build/out/swagger/'
    public static final String OUT_HTML = 'build/out/swagger/html/'

    private final JobConfiguration config;

    Swagger(JobConfiguration config) {
        this.config = config
    }

    def run() {
        if (!config.stageFlags.swagger) {
            Logger.println("Swagger documentation is disabled")
            return
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        steps.installLocalDependencies()

        String swaggerPath = OscriptModules.getAppExecutable("swagger");

        steps.cmd(swaggerPath + " generate --src-path $config.srcDir --out $OUT")

        def dir = new File(OUT)
        dir.eachFile(FileType.FILES){
            String reportdir = "$OUT_HTML$it.name"
            Logger.println(it.name)
            Logger.println(reportdir)
            Logger.println(it.path)
            steps.cmd("bootprint openapi $it.path $reportdir")
            publishHTML (target : [allowMissing: false,
                                   alwaysLinkToLastBuild: true,
                                   keepAll: true,
                                   reportDir: $reportdir,
                                   reportFiles: 'index.html',
                                   reportName: 'Swagger API',
                                   reportTitles: 'API $it.name'])
        }

        steps.archiveArtifacts(OUT)
    }
}