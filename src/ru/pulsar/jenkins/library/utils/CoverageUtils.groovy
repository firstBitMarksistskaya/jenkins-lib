package ru.pulsar.jenkins.library.utils

import hudson.FilePath
import org.apache.commons.lang3.RandomStringUtils
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.GlobalCoverageOptions
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.StepCoverageOptions
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.steps.Coverable
import ru.pulsar.jenkins.library.steps.CoverageContext

class CoverageUtils {
    static ArrayList<String> getPIDs(String name) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String pids

        if (steps.isUnix()) {
            pids = steps.sh("ps -aux | grep '$name' | awk '{print \$2}'", false, true, 'UTF-8')
        } else {
            pids = steps.bat("chcp 65001 > nul \nfor /f \"tokens=2\" %a in ('tasklist ^| findstr $name') do @echo %a", false, true, 'UTF-8')
        }
        return pids.split('\n').toList()
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

        return new CoverageContext(lockableResource, coverageOpts, port, currentDbgsPids, currentCoverage41CPids)

    }

    static void startCoverage(IStepExecutor steps, GlobalCoverageOptions coverageOpts, CoverageContext coverageContext, FilePath workspaceDir, String srcDir, Coverable coverable) {
        steps.start("${coverageOpts.dbgsPath} --addr=127.0.0.1 --port=$coverageContext.port")
        steps.start("${coverageOpts.coverage41CPath} start -i DefAlias -u http://127.0.0.1:$coverageContext.port -P $workspaceDir -s $srcDir -o ${coverable.getCoverageStashPath()}")
        steps.cmd("${coverageOpts.coverage41CPath} check -i DefAlias -u http://127.0.0.1:$coverageContext.port")

        def newDbgsPids = getPIDs("dbgs")
        def newCoverage41CPids = getPIDs("Coverage41C")

        newDbgsPids.removeAll(coverageContext.dbgsPids)
        newCoverage41CPids.removeAll(coverageContext.coverage41CPids)

        newDbgsPids.addAll(newCoverage41CPids)
        def pids = newDbgsPids.join(" ")

        steps.writeFile(coverable.getCoveragePidsPath(), pids, 'UTF-8')

        Logger.println("Coverage PIDs for cleanup: $pids")
    }

    static void stopCoverage(IStepExecutor steps, GlobalCoverageOptions coverageOpts, CoverageContext coverageContext) {
        steps.cmd("${coverageOpts.coverage41CPath} stop -i DefAlias -u http://127.0.0.1:$coverageContext.port")
    }

}
