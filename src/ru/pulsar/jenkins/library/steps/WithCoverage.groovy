package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.StepCoverageOptions
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
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

        def context = prepareContext(config, coverageOptions)
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        steps.lock(context.lockableResource) {
            try {

                startCoverage(steps, config, context, stage)

                body()

                stopCoverage(steps, config, context)

                steps.stash(stage.getCoverageStashName(), stage.getCoverageStashPath(), true)

            } catch (Exception e) {
                Logger.println("При выполнении блока произошла ошибка: ${e.message}")
                throw e
            } finally {

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

    static List<String> getPIDs(String name) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String pids
        def script

        if (steps.isUnix()) {
            script = "ps -C '$name' -o pid= || true"
            pids = steps.sh(script, false, true, 'UTF-8')
        } else {
            script = """@echo off
                chcp 65001 > nul
                tasklist | findstr "${name}" > nul
                if errorlevel 1 (
                    exit /b 0
                ) else (
                    for /f "tokens=2" %%a in ('tasklist ^| findstr "${name}"') do (@echo %%a)
                )"""
            pids = steps.bat(script, false, true, 'UTF-8')
        }
        return pids.split('\r?\n').toList()
    }

    static CoverageContext prepareContext(JobConfiguration config, StepCoverageOptions options) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env()

        def coverageOpts = config.coverageOptions
        def port = options.dbgsPort
        def currentDbgsPids = getPIDs("dbgs")
        def currentCoverage41CPids = getPIDs("Coverage41C")
        def lockableResource = "${env.NODE_NAME}_$port"

        return new CoverageContext(lockableResource, config.srcDir, coverageOpts, port, currentDbgsPids, currentCoverage41CPids)

    }

    static void startCoverage(IStepExecutor steps, JobConfiguration config, CoverageContext coverageContext, Coverable stage) {

        def env = steps.env()
        def srcDir = config.srcDir
        def workspaceDir = FileUtils.getFilePath("$env.WORKSPACE")

        def coverageOpts = config.coverageOptions

        String dbgsPath = findDbgs(steps, config)

        steps.start(dbgsPath, "--addr=127.0.0.1 --port=${coverageContext.port}")
        steps.start(coverageOpts.coverage41CPath, "start -i DefAlias -u http://127.0.0.1:${coverageContext.port} -P $workspaceDir -s $srcDir -o ${stage.getCoverageStashPath()}")
        steps.cmd("${coverageOpts.coverage41CPath} check -i DefAlias -u http://127.0.0.1:${coverageContext.port}")

        def newDbgsPids = getPIDs("dbgs")
        def newCoverage41CPids = getPIDs("Coverage41C")

        newDbgsPids.removeAll(coverageContext.dbgsPids)
        newCoverage41CPids.removeAll(coverageContext.coverage41CPids)

        newDbgsPids.addAll(newCoverage41CPids)
        def pids = newDbgsPids.join(" ")

        steps.writeFile(stage.getCoveragePidsPath(), pids, 'UTF-8')

        Logger.println("PID процессов dbgs и Coverage41C для ${stage.getStageSlug()}: $pids")
    }

    static void stopCoverage(IStepExecutor steps, JobConfiguration config, CoverageContext coverageContext) {

        def coverageOpts = config.coverageOptions

        steps.cmd("${coverageOpts.coverage41CPath} stop -i DefAlias -u http://127.0.0.1:$coverageContext.port")
    }

    static String findDbgs(IStepExecutor steps, JobConfiguration config) {
        if (steps == null || config == null) {
            throw new IllegalArgumentException("Некорректные параметры поиска dbgs")
        }

        String dbgsPath = config.coverageOptions.dbgsPath
        if (!dbgsPath.isEmpty()) {
            Logger.println("Использую путь к dbgs из параметра dbgsPath: $dbgsPath")
            return dbgsPath.strip()
        }

        final dbgsFindScriptPath = "build/tmp/dbgs_${System.currentTimeMillis()}.os"
        final dbgsPathResult = "build/tmp/dbgsPath_${System.currentTimeMillis()}"

        def dbgsFindScript = steps.libraryResource("dbgs.os")
        steps.writeFile(dbgsFindScriptPath, dbgsFindScript, 'UTF-8')

        steps.cmd("oscript ${dbgsFindScriptPath} ${config.v8version} > ${dbgsPathResult}")
        dbgsPath = steps.readFile(dbgsPathResult).strip()

        if (dbgsPath.isEmpty()) {
            steps.error("Не удалось найти путь к dbgs")
        }

        Logger.println("Найден путь к dbgs: ${dbgsPath}")
        return dbgsPath
    }

}
