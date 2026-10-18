import ru.pulsar.jenkins.library.configuration.JobConfiguration

import java.util.concurrent.TimeUnit

/**
 * Подготовка своей базы тестовой стадии на текущем агенте теми же шагами, что и база сборки в стадии
 * "Подготовка ИБ". Конфигурация передается копией, в которой basePath - база стадии.
 * Возвращает признак успешной инициализации базы.
 */
def call(JobConfiguration config) {
    // Исходники на агенте стадии получает Declarative, а этот агент выделен шагом node
    if (hasScm()) {
        checkout scm
    }
    restoreDebugOverridesIfNeeded()

    if (config.needLoadExtensions()) {
        stage('Сборка расширений из исходников') {
            timeout(time: config.timeoutOptions.getBinaries, unit: TimeUnit.MINUTES) {
                createDir('build/out/cfe')
                // stash для тестовых стадий сохраняет подготовка базы сборки
                getExtensions config, false
            }
        }
    }

    stage('Создание ИБ') {
        timeout(time: config.timeoutOptions.createInfoBase, unit: TimeUnit.MINUTES) {
            createDir('build/out/')
            createInfobase config
        }
    }

    if (config.needLoadConfiguration()) {
        stage('Загрузка конфигурации') {
            timeout(time: config.timeoutOptions.loadConfiguration, unit: TimeUnit.MINUTES) {
                if (config.infoBaseFromFiles()) {
                    initFromFiles config
                } else {
                    initFromStorage config
                }
            }
        }
    }

    if (config.needLoadExtensions()) {
        stage('Загрузка расширений в конфигурацию') {
            timeout(time: config.timeoutOptions.loadExtensions, unit: TimeUnit.MINUTES) {
                loadExtensions config, 'initInfoBase'
            }
        }
    }

    boolean initialized = true
    if (config.stageFlags.initSteps) {
        stage('Инициализация ИБ') {
            timeout(time: config.timeoutOptions.initInfoBase, unit: TimeUnit.MINUTES) {
                initialized = initInfobase config
            }
        }
    }
    return initialized
}

// Задание без SCM, например Pipeline со скриптом в настройках, исходники не получает
private boolean hasScm() {
    try {
        return scm != null
    } catch (Exception ignored) {
        return false
    }
}
