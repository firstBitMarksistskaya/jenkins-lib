package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class Start implements Serializable {

    private String executable
    private String params
    private String encoding = 'UTF-8'

    Start(String executable, String params) {
        this.executable = executable
        this.params = params
    };

    void run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        if (steps.isUnix()) {
            steps.sh("$executable $params &", false, false , encoding)
        } else {
            steps.bat("chcp 65001 > nul \nstart \"\" /B \"$executable\" \"$params\"", false, false, encoding)
        }
    }
}
