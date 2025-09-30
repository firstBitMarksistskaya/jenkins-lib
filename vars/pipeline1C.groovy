/* groovylint-disable NestedBlockDepth */
import groovy.transform.Field
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.utils.RepoUtils

import java.util.concurrent.TimeUnit

@Field
JobConfiguration config

@Field
String agent1C

@Field
String agentEdt

/**
 * Safely loads configuration, supporting pipeline restarts.
 * First tries to use the cached config, then attempts to unstash it,
 * and finally loads it fresh as a fallback.
 */
JobConfiguration safeLoadConfig() {
    if (config != null) {
        return config
    }
    
    // Try to unstash configuration from pre-stage
    try {
        unstash 'pipeline-config'
        config = jobConfiguration('pipeline-config.json') as JobConfiguration
        // Ensure agent variables are set after loading config
        if (agent1C == null) {
            agent1C = config.v8AgentLabel()
        }
        if (agentEdt == null) {
            agentEdt = config.edtAgentLabel()
        }
        return config
    } catch (Exception e) {
        // Fallback to loading fresh configuration
        echo "Warning: Could not restore configuration from stash, loading fresh config. This may happen on pipeline restart."
        config = jobConfiguration() as JobConfiguration
        agent1C = config.v8AgentLabel()
        agentEdt = config.edtAgentLabel()
        return config
    }
}

void call() {

    //noinspection GroovyAssignabilityCheck
    pipeline {
        agent none

        options {
            buildDiscarder(logRotator(numToKeepStr: '30'))
            timestamps()
        }

        stages {

            stage('pre-stage') {
                agent {
                    label 'agent'
                }
                options {
                    timeout(time: 1, unit: TimeUnit.HOURS)
                }

                steps {
                    script {
                        config = jobConfiguration() as JobConfiguration
                        agent1C = config.v8AgentLabel()
                        agentEdt = config.edtAgentLabel()
                        RepoUtils.computeRepoSlug(env.GIT_URL)
                        
                        // Stash configuration for pipeline restart support
                        def configJson = writeJSON returnText: true, json: config
                        writeFile file: 'pipeline-config.json', text: configJson, encoding: 'UTF-8'
                        stash name: 'pipeline-config', includes: 'pipeline-config.json'
                    }
                }
            }

            stage('Подготовка') {
                parallel {
                    stage('Подготовка 1C базы') {
                        when {
                            beforeAgent true
                            expression { safeLoadConfig().stageFlags.needInfoBase() }
                        }

                        stages {
                            stage('Трансформация из формата EDT') {
                                agent {
                                    label agentEdt
                                }
                                when {
                                    beforeAgent true
                                    expression { 
                                        def cfg = safeLoadConfig()
                                        cfg.stageFlags.needInfoBase() && cfg.infoBaseFromFiles() && cfg.sourceFormat == SourceFormat.EDT 
                                    }
                                }
                                steps {
                                    timeout(time: config.timeoutOptions.edtToDesignerFormatTransformation, unit: TimeUnit.MINUTES) {
                                        edtToDesignerFormatTransformation config
                                    }
                                }
                            }

                            stage('Подготовка 1С базы') {
                                agent {
                                    label agent1C
                                }

                                stages {
                                    stage('Сборка расширений из исходников') {
                                        when {
                                            expression { safeLoadConfig().needLoadExtensions() }
                                        }
                                        steps {
                                            timeout(time: config.timeoutOptions.getBinaries, unit: TimeUnit.MINUTES) {
                                                createDir('build/out/cfe')
                                                // Соберем или загрузим cfe из исходников и положим их в папку build/out/cfe
                                                getExtensions config
                                            }
                                        }
                                    }
                                    stage('Создание ИБ') {
                                        steps {
                                            timeout(time: config.timeoutOptions.createInfoBase, unit: TimeUnit.MINUTES) {
                                                createDir('build/out/')
                                                    createInfobase config
                                            }
                                        }
                                    }

                                    stage('Загрузка конфигурации') {
                                        steps {
                                            timeout(time: config.timeoutOptions.loadConfiguration, unit: TimeUnit.MINUTES) {
                                                script {
                                                    if (config.infoBaseFromFiles()) {
                                                        // Создание базы загрузкой из файлов
                                                        initFromFiles config
                                                    } else {
                                                        // Создание базы загрузкой конфигурации из хранилища
                                                        initFromStorage config
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    stage('Загрузка расширений в конфигурацию'){
                                        when {
                                            beforeAgent true
                                            expression { safeLoadConfig().needLoadExtensions() }
                                        }
                                        steps {
                                            timeout(time: config.timeoutOptions.loadExtensions, unit: TimeUnit.MINUTES) {
                                                loadExtensions config, 'initInfoBase'
                                            }
                                        }
                                    }

                                    stage('Инициализация ИБ') {
                                        when {
                                            beforeAgent true
                                            expression { safeLoadConfig().stageFlags.initSteps }
                                        }
                                        steps {
                                            timeout(time: config.timeoutOptions.initInfoBase, unit: TimeUnit.MINUTES) {
                                                // Инициализация и первичная миграция
                                                initInfobase config
                                            }
                                        }
                                    }


                                    stage('Архивация ИБ') {
                                        steps {
                                            timeout(time: config.timeoutOptions.zipInfoBase, unit: TimeUnit.MINUTES) {
                                                printLocation()

                                                zipInfobase config, 'initInfoBase'
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }

                    stage('Трансформация в формат EDT') {
                        agent {
                            label agentEdt
                        }
                        when {
                            beforeAgent true
                            expression { 
                                def cfg = safeLoadConfig()
                                cfg.sourceFormat == SourceFormat.DESIGNER && cfg.stageFlags.edtValidate 
                            }
                        }
                        steps {
                            timeout(time: config.timeoutOptions.designerToEdtFormatTransformation, unit: TimeUnit.MINUTES) {
                                designerToEdtFormatTransformation config
                            }
                        }
                    }
                }
            }

            stage('Проверка качества') {
                parallel {
                    stage('EDT контроль') {
                        when {
                            beforeAgent true
                            expression { safeLoadConfig().stageFlags.edtValidate }
                        }
                        stages {
                            stage('Валидация EDT') {
                                agent {
                                    label agentEdt
                                }
                                steps {
                                    timeout(time: config.timeoutOptions.edtValidate, unit: TimeUnit.MINUTES) {
                                        edtValidate config
                                    }
                                }
                            }

                            stage('Трансформация результатов') {
                                agent {
                                    label 'oscript'
                                }
                                steps {
                                    timeout(time: config.timeoutOptions.resultTransformation, unit: TimeUnit.MINUTES) {
                                        transform config
                                    }
                                }
                            }
                        }
                    }

                    stage('BDD сценарии') {
                        agent {
                            label agent1C
                        }
                        when {
                            beforeAgent true
                            expression { safeLoadConfig().stageFlags.bdd }
                        }
                        stages {
                            stage('Распаковка ИБ') {
                                steps {
                                    unzipInfobase()
                                }
                            }

                            stage('Загрузка расширений в конфигурацию') {
                                when {
                                    beforeAgent true
                                    expression { safeLoadConfig().needLoadExtensions('bdd') }
                                }
                                steps {
                                    timeout(time: config.timeoutOptions.loadExtensions, unit: TimeUnit.MINUTES) {
                                        loadExtensions config, 'bdd'
                                    }
                                }
                            }

                            stage('Выполнение BDD сценариев') {
                                steps {
                                    timeout(time: config.timeoutOptions.bdd, unit: TimeUnit.MINUTES) {
                                        bdd config
                                    }
                                }
                            }

                            stage('Архивация ИБ') {
                                steps {
                                    timeout(time: config.timeoutOptions.zipInfoBase, unit: TimeUnit.MINUTES) {
                                        printLocation()

                                        zipInfobase config, 'bdd'
                                    }
                                }
                            }
                        }
                    }

                    stage('Синтаксический контроль') {
                        agent {
                            label agent1C
                        }
                        when {
                            beforeAgent true
                            expression { safeLoadConfig().stageFlags.syntaxCheck }
                        }
                        stages {
                            stage('Распаковка ИБ') {
                                steps {
                                    unzipInfobase()
                                }
                            }

                            stage('Выполнение синтаксического контроля') {
                                steps {
                                    timeout(time: config.timeoutOptions.syntaxCheck, unit: TimeUnit.MINUTES) {
                                        syntaxCheck config
                                    }
                                }
                            }
                        }
                    }

                    stage('Дымовые тесты') {
                        agent {
                            label agent1C
                        }
                        when {
                            beforeAgent true
                            expression { safeLoadConfig().stageFlags.smoke }
                        }
                        stages {
                            stage('Распаковка ИБ') {
                                steps {
                                    unzipInfobase()
                                }
                            }

                            stage('Загрузка расширений в конфигурацию') {
                                when {
                                    beforeAgent true
                                    expression { safeLoadConfig().needLoadExtensions('smoke') }
                                }
                                steps {
                                    timeout(time: config.timeoutOptions.loadExtensions, unit: TimeUnit.MINUTES) {
                                        loadExtensions config, 'smoke'
                                    }
                                }
                            }

                            stage('Выполнение дымовых тестов') {
                                steps {
                                    timeout(time: config.timeoutOptions.smoke, unit: TimeUnit.MINUTES) {
                                        smoke config
                                    }
                                }
                            }
                        }
                    }

                    stage('YAXUnit тесты') {
                        agent {
                            label agent1C
                        }
                        when {
                            beforeAgent true
                            expression { safeLoadConfig().stageFlags.yaxunit }
                        }
                        stages {
                            stage('Распаковка ИБ') {
                                steps {
                                    unzipInfobase()
                                }
                            }

                            stage('Загрузка расширений в конфигурацию') {
                                when {
                                    beforeAgent true
                                    expression { safeLoadConfig().needLoadExtensions('yaxunit') }
                                }
                                steps {
                                    timeout(time: config.timeoutOptions.loadExtensions, unit: TimeUnit.MINUTES) {
                                        loadExtensions config, 'yaxunit'
                                    }
                                }
                            }

                            stage('Выполнение YAXUnit тестов') {
                                steps {
                                    timeout(time: config.timeoutOptions.yaxunit, unit: TimeUnit.MINUTES) {
                                        yaxunit config
                                    }
                                }
                            }
                        }
                    }
                }
            }

            stage('SonarQube') {
                agent {
                    label 'sonar'
                }
                when {
                    beforeAgent true
                    expression { safeLoadConfig().stageFlags.sonarqube }
                }
                steps {
                    timeout(time: config.timeoutOptions.sonarqube, unit: TimeUnit.MINUTES) {
                        sonarScanner config
                    }
                }
            }
        }

        post('post-stage') {
            always {
                node('agent') {
                    saveResults config
                    sendNotifications(config)
                }
            }
        }
    }

}
