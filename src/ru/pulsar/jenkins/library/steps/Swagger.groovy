package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.OscriptModules

class Swagger {
    public static final String OUT = 'build/out/swagger'

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

        def env = steps.env();

        steps.installLocalDependencies()

        String swaggerPath = OscriptModules.getModulePath("swagger");

        steps.cmd(swaggerPath + "generate --src-path $config.srcDir --out $OUT")

        steps.archiveArtifacts(OUT)
    }
}