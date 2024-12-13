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
        def env = steps.env()

        def executable_name = getExecutableName(executable)
        if (steps.isUnix()) {
            steps.sh("$executable $params > \"./build/${env.STAGE_NAME}-start-${executable_name}.log 2>&1 &", false, false , encoding)
        } else {
            steps.bat("chcp 65001 > nul \nstart \"\" /B \"$executable\" $params > \"./build/${env.STAGE_NAME}-start-${executable_name}.log\" 2>&1", false, false, encoding)
        }
    }

    static String getExecutableName(String executable) {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String normalizedPath = executable.replace("\\", "/")
        String executableName = normalizedPath.substring(normalizedPath.lastIndexOf("/") + 1)

        // Remove the file extension if it exists on Windows systems (e.g., .exe, .cmd, .bat)
        if (!steps.isUnix()) {
            int extensionIndex = executableName.lastIndexOf(".")
            if (extensionIndex != -1) {
                executableName = executableName.substring(0, extensionIndex)
            }
        }

        return executableName
    }
}
