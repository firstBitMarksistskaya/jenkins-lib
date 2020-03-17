def call(String command, boolean returnStatus = false) {
    if (isUnix()) {
        sh script: "${command}", returnStatus: returnStatus, encoding: "UTF-8"
    } else {
        bat script: "chcp 65001 > nul \n${command}", returnStatus: returnStatus, encoding: "UTF-8"
    }
}
