package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.StepCoverageOptions
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.CoverageUtils
import ru.pulsar.jenkins.library.utils.Logger

class WithCoverage implements Serializable {

    private final JobConfiguration config
    private final Coverable stage
    private final StepCoverageOptions coverageOptions
    private final Closure body

    WithCoverage(JobConfiguration config, Coverable stage, StepCoverageOptions coverageOptions, Closure body) {
        this.config = config
        this.stage = stage
        this.coverageOptions = coverageOptions
        this.body = body
    }

    def run() {

        if (!coverageOptions.coverage) {
            body()
            return
        }

        def context = CoverageUtils.prepareContext(config, coverageOptions)
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        steps.lock(context.lockableResource) {
            try {

                CoverageUtils.startCoverage(steps, config, context, stage)

                body()

                steps.stash(stage.getCoverageStashName(), stage.getCoverageStashPath(), true)

            } catch (Exception e) {
                throw new Exception("При выполнении блока произошла ошибка: ${e}")
            } finally {

                CoverageUtils.stopCoverage(steps, config, context)

                String pidsFilePath = "build/${stage.getStageSlug()}-pids"

                def pids = ""
                if (steps.fileExists(pidsFilePath)) {
                    pids = steps.readFile(pidsFilePath)
                }

                if (pids.isEmpty()) {
                    Logger.println("Нет запущенных процессов dbgs и Coverage41C")
                    return
                }

                Logger.println("Завершение процессов dbgs и Coverage41C с pid: $pids")
                def command
                if (steps.isUnix()) {
                    command = "kill $pids || true"
                } else {
                    def pidsForCmd = ''
                    def pidsArray = pids.split(" ")

                    pidsArray.each {
                        pidsForCmd += "/PID $it"
                    }
                    pidsForCmd = pidsForCmd.trim()

                    command = "taskkill $pidsForCmd /F > nul"

                }
                steps.cmd(command, false, false)
            }
        }
    }
}
