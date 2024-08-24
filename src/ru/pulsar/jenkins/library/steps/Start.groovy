package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class Start implements Serializable {

    private String script
    private String encoding = 'UTF-8'

    Start(String script) {
        this.script = script
    };

    void run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        if (steps.isUnix()) {
            steps.sh("$script &", false, false , encoding)
        } else {
            steps.bat("chcp 65001 > nul \nstart $script", false, false, encoding)
        }
    }
}
