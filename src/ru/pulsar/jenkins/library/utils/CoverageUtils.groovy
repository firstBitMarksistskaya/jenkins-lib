package ru.pulsar.jenkins.library.utils

import org.apache.commons.lang3.RandomStringUtils
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.StepCoverageOptions
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.Coverable
import ru.pulsar.jenkins.library.steps.CoverageContext

class CoverageUtils {
    static List<String> getPIDs(String name) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String pids
        def script

        if (steps.isUnix()) {
            script = "ps -aux | grep '$name' | grep -v grep | awk '{print \$2}'"
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
        def lockableResource = RandomStringUtils.random(9, true, false)
        if (options.coverage) {
            lockableResource = "${env.NODE_NAME}_$port"
        }

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

        Logger.println("Coverage PIDs for cleanup: $pids")
    }

    static void stopCoverage(IStepExecutor steps, JobConfiguration config, CoverageContext coverageContext) {

        def coverageOpts = config.coverageOptions

        steps.cmd("${coverageOpts.coverage41CPath} stop -i DefAlias -u http://127.0.0.1:$coverageContext.port")
    }

    static String findDbgs(IStepExecutor steps, JobConfiguration config) {

        String dbgsPath = config.coverageOptions.dbgsPath
        if (!dbgsPath.isEmpty()) {
            Logger.println("Using dbgsPath from config: $dbgsPath")
            return dbgsPath.strip()
        }

        def dbgsFindScript = steps.libraryResource("dbgs.os")
        final dbgsFindScriptPath = "build/dbgs.os"
        final dbgsPathResult = "build/dbgsPath"
        steps.writeFile(dbgsFindScriptPath, dbgsFindScript, 'UTF-8')

        steps.cmd("oscript ${dbgsFindScriptPath} ${config.v8version} > ${dbgsPathResult}", false, false)

        dbgsPath = steps.readFile(dbgsPathResult).strip()

        if (dbgsPath.isEmpty()) {
            steps.error("Не удалось найти путь к dbgs")
        }

        Logger.println("Found dbgs: ${dbgsPath}")
        return dbgsPath

    }

}
