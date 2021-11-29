package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

import java.nio.file.Paths

class Swagger {
    public static final String OUT = 'build/out/'

    private final JobConfiguration config;

    Swagger(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        def env = steps.env();

        if (!config.stageFlags.swagger) {
            Logger.println("Swagger documentation is disabled")
            return
        }

        steps.installLocalDependencies()

        String SRC_PATH =  new File("$env.WORKSPACE/$config.srcDir").getCanonicalPath()

        steps.cmd("oscript_modules/bin/swagger generate --src-path $SRC_PATH --out $OUT")

        steps.archiveArtifacts(OUT)
    }
}