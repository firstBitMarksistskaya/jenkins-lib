package ru.pulsar.jenkins.library.steps

import com.cloudbees.groovy.cps.NonCPS
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.StepCoverageOptions
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger

import java.nio.charset.StandardCharsets
import java.util.Base64

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

    static String startDbgs(IStepExecutor steps, String executable, int port,
                            String stdoutLogPath, String stderrLogPath) {
        if (steps.isUnix()) {
            String command = quotePosix(executable) +
                " --addr=127.0.0.1 --port=${port}" +
                ' > ' + quotePosix(stdoutLogPath) +
                ' 2>&1 & printf \'%s\\n\' "$!"'
            String rawPid = steps.sh(command, false, true, 'UTF-8')
            return requirePositivePid(rawPid)
        }

        String script = buildWindowsStartScript(
            executable, port, stdoutLogPath, stderrLogPath)
        String encodedScript = Base64.encoder.encodeToString(
            script.getBytes(StandardCharsets.UTF_16LE))
        String rawPid = steps.bat(
            "@echo off\r\npowershell.exe -NoProfile -NonInteractive -EncodedCommand ${encodedScript}",
            false, true, 'UTF-8')
        return requirePositivePid(rawPid)
    }

    @NonCPS
    private static String buildWindowsStartScript(String executable, int port,
                                                   String stdoutLogPath,
                                                   String stderrLogPath) {
        return "\$p = Start-Process" +
            " -FilePath '${quotePowerShell(executable)}'" +
            " -ArgumentList '--addr=127.0.0.1','--port=${port}'" +
            " -RedirectStandardOutput '${quotePowerShell(stdoutLogPath)}'" +
            " -RedirectStandardError '${quotePowerShell(stderrLogPath)}'" +
            ' -PassThru -WindowStyle Hidden' +
            System.lineSeparator() +
            '$p.Id'
    }

    @NonCPS
    private static String quotePowerShell(String value) {
        return value.replace("'", "''")
    }

    @NonCPS
    private static String quotePosix(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'"
    }

    @NonCPS
    private static String requirePositivePid(Object rawPid) {
        String pid = rawPid == null ? '' : rawPid.toString().trim()
        if (!(pid ==~ /[1-9][0-9]*/)) {
            throw new IllegalStateException("Не удалось получить PID dbgs: '${pid}'")
        }
        return pid
    }

    static int stopDbgs(IStepExecutor steps, String rawPid) {
        String pid = requirePositivePid(rawPid)
        if (steps.isUnix()) {
            return steps.sh(
                "kill ${pid} >/dev/null 2>&1",
                true, false, 'UTF-8') as int
        }
        return steps.bat(
            "@echo off\r\ntaskkill /PID ${pid} /F > nul 2>&1",
            true, false, 'UTF-8') as int
    }

    def run() {

        if (!coverageOptions.coverage) {
            body()
            return
        }

        def context = prepareContext(config, coverageOptions)
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        steps.lock(context.lockableResource) {
            Exception primaryFailure = null
            try {
                steps.writeFile(stage.getCoveragePidsPath(), '', 'UTF-8')

                startCoverage(steps, config, context, stage)

                body()

                stopCoverage(steps, config, context)

                steps.stash(stage.getCoverageStashName(), stage.getCoverageStashPath(), true)

            } catch (Exception e) {
                primaryFailure = e
                Logger.println("При выполнении блока произошла ошибка: ${e.message}")
                throw e
            } finally {
                try {
                    cleanupOwnedDbgs(steps, context)
                } catch (Exception cleanupFailure) {
                    if (primaryFailure != null) {
                        primaryFailure.addSuppressed(cleanupFailure)
                        Logger.println("Ошибка завершения dbgs: ${cleanupFailure.message}")
                    } else {
                        throw cleanupFailure
                    }
                }
            }
        }
    }

    static CoverageContext prepareContext(JobConfiguration config, StepCoverageOptions options) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env()

        def coverageOpts = config.coverageOptions
        def port = options.dbgsPort
        def lockableResource = "${env.NODE_NAME}_$port"

        return new CoverageContext(lockableResource, config.srcDir, coverageOpts, port)

    }

    static void startCoverage(IStepExecutor steps, JobConfiguration config, CoverageContext coverageContext, Coverable stage) {

        def env = steps.env()
        def srcDir = config.srcDir
        def workspaceDir = FileUtils.getFilePath("$env.WORKSPACE")

        def coverageOpts = config.coverageOptions

        String dbgsPath = findDbgs(steps, config)

        String logPrefix = "./build/${env.STAGE_NAME}-start-dbgs"
        String dbgsPid = startDbgs(
            steps, dbgsPath, coverageContext.port,
            "${logPrefix}.log", "${logPrefix}-error.log")
        coverageContext.dbgsPid = dbgsPid
        steps.writeFile(stage.getCoveragePidsPath(), dbgsPid, 'UTF-8')
        Logger.println("PID процесса dbgs для ${stage.getStageSlug()}: ${dbgsPid}")

        steps.start(coverageOpts.coverage41CPath, "start -i DefAlias -u http://127.0.0.1:${coverageContext.port} -P $workspaceDir -s $srcDir -o ${stage.getCoverageStashPath()}")
        sleep(1000)
        steps.cmd("${coverageOpts.coverage41CPath} check -i DefAlias -u http://127.0.0.1:${coverageContext.port}")
    }

    static void cleanupOwnedDbgs(IStepExecutor steps, CoverageContext coverageContext) {
        String ownedPid = coverageContext.dbgsPid
        if (ownedPid) {
            int status = stopDbgs(steps, ownedPid)
            if (status != 0) {
                Logger.println(
                    "Процесс dbgs с pid ${ownedPid} уже завершён или недоступен, код: ${status}")
            }
        } else {
            Logger.println('PID процесса dbgs для текущего запуска не назначен')
        }
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

        steps.cmd("oscript ${dbgsFindScriptPath} ${config.v8version} ${dbgsPathResult}")

        dbgsPath = steps.readFile(dbgsPathResult).strip()

        if (dbgsPath.isEmpty()) {
            steps.error("Не удалось найти путь к dbgs")
        }

        Logger.println("Найден путь к dbgs: ${dbgsPath}")
        return dbgsPath
    }

}
