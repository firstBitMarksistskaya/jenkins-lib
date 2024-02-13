def call() {
    if (fileExists('1Cv8.1CD.zip')) {
        fileOperations([fileDeleteOperation(includes: '1Cv8.1CD.zip')])
    }
    zip dir: 'build/ib', glob: '1Cv8.1CD', zipFile: '1Cv8.1CD.zip'
    stash name: "1Cv8.1CD.zip", includes: "1Cv8.1CD.zip", allowEmpty: false
}
