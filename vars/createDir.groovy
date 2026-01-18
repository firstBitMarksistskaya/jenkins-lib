def call(String path, boolean deleteDir = false) {
    if (deleteDir && fileExists(path)) {
        deleteDir(path)
    }
    dir(path) { echo '' }
}
