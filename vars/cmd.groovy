def call(String script, boolean returnStatus = false) {
    if (isUnix()) {
        sh script: "${script}", returnStatus: returnStatus, encoding: "UTF-8"
    } else {
        bat script: "chcp 65001 > nul \n${script}", returnStatus: returnStatus, encoding: "UTF-8"
    }
}
