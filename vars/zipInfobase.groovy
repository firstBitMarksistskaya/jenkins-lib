def call() {
    zip dir: 'build/ib', glob: '1Cv8.1CD', zipFile: '1Cv8.1CD.zip'
    stash name: "1Cv8.1CD.zip", includes: "1Cv8.1CD.zip", allowEmpty: false
}
