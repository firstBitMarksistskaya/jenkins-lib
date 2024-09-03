package ru.pulsar.jenkins.library.utils

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

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
}
