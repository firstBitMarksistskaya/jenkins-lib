package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class Cmd implements Serializable {

    private String script;
    private boolean returnStatus
    private String encoding = 'UTF-8'

    Cmd(String script, boolean returnStatus = false) {
        this.script = script
        this.returnStatus = returnStatus
    };

    int run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        int returnValue

        if (steps.isUnix()) {
            returnValue = steps.sh("$script", returnStatus, encoding)
        } else {
            returnValue = steps.bat("chcp 65001 > nul \n$script", returnStatus, encoding)
        }
        
        return returnValue
    }
}
