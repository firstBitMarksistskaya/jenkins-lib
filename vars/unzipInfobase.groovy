import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.steps.GetExtensions

def call(JobConfiguration config = null) {
    // Внешняя база доступна всем агентам по адресу, архив нужен только локальной базе
    if (config == null || !config.basePath) {
        unstash '1Cv8.1CD.zip'
        unzip dir: 'build/ib', zipFile: '1Cv8.1CD.zip'
    }
    // Собранные расширения нужны для подключения расширений стадии на этом агенте при любой базе
    try {
        unstash GetExtensions.EXTENSIONS_STASH
    } catch (Exception e) {
        echo e.toString()
    }
}
