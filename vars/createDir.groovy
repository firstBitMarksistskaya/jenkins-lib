def call(String path, boolean cleanDir = false) {
    if (cleanDir && fileExists(path)) {
        dir(path) {
            deleteDir()
        }
    }
    if (isUnix()) {
        sh "mkdir -p '${path}'"
    } else {
        bat "@if not exist \"${path}\" mkdir \"${path}\""
    }
}
