import ru.pulsar.jenkins.library.steps.GetExtensions

def call() {
    unstash '1Cv8.1CD.zip'
    unzip dir: 'build/ib', zipFile: '1Cv8.1CD.zip'
    try {
        unstash GetExtensions.EXTENSIONS_STASH
    } catch (Exception e) {
        echo e.toString()
    }
}