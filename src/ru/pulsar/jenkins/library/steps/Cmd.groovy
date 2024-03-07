package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class Cmd implements Serializable {

    private String script;
    private boolean returnStatus
    private boolean returnStdout
    private String encoding = 'UTF-8'

    Cmd(String script, boolean returnStatus = false, boolean returnStdout = false) {
        this.script = script
        this.returnStatus = returnStatus
        this.returnStdout = returnStdout
    };

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def returnValue

        if (returnStatus & returnStdout) {
            steps.error("returnStatus and returnStdout are not supported at the same time")
        }

        if (steps.isUnix()) {
            returnValue = steps.sh("$script", returnStatus, returnStdout, encoding)
        } else {
            returnValue = steps.bat("chcp 65001 > nul \n$script", returnStatus, returnStdout, encoding)
        }
        
        return returnValue
    }
}
