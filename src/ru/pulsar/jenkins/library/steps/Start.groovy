package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class Start implements Serializable {

    private String script
    private boolean returnStatus
    private String encoding = 'UTF-8'

    Start(String script, boolean returnStatus = false) {
        this.script = script
        this.returnStatus = returnStatus
    };

    void run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        if (steps.isUnix()) {
            steps.sh("$script &", returnStatus, encoding)
        } else {
            steps.bat("chcp 65001 > nul \nstart $script", returnStatus, encoding)
        }
    }
}
