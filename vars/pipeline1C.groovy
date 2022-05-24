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
                    }
                }
            }

            stage('Подготовка') {
                parallel {
                    stage('Подготовка 1C базы') {
                        when {
                            beforeAgent true
                            expression { config.stageFlags.needInfoBase() }
                        }

                        stages {
                            stage('Трансформация из формата EDT') {
                                agent {
                                    label agentEdt
                                }
                                when {
                                    beforeAgent true
                                    expression { config.stageFlags.needInfoBase() && config.infoBaseFromFiles() && config.sourceFormat == SourceFormat.EDT }
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
                                    stage('Создание ИБ') {
                                        steps {
                                            timeout(time: config.timeoutOptions.createInfoBase, unit: TimeUnit.MINUTES) {
                                                createDir('build/out')

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

                                    stage('Инициализация ИБ') {
                                        when {
                                            beforeAgent true
                                            expression { config.stageFlags.initSteps }
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

                                                zipInfobase()
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
                            expression { config.sourceFormat == SourceFormat.DESIGNER && config.stageFlags.edtValidate }
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
                            expression { config.stageFlags.edtValidate }
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
                            expression { config.stageFlags.bdd }
                        }
                        steps {
                            timeout(time: config.timeoutOptions.bdd, unit: TimeUnit.MINUTES) {
                                unzipInfobase()

                                bdd config
                            }
                        }
                    }

                    stage('Синтаксический контроль') {
                        agent {
                            label agent1C
                        }
                        when {
                            beforeAgent true
                            expression { config.stageFlags.syntaxCheck }
                        }
                        steps {
                            timeout(time: config.timeoutOptions.syntaxCheck, unit: TimeUnit.MINUTES) {
                                syntaxCheck config
                            }
                        }
                    }

                    stage('Дымовые тесты') {
                        agent {
                            label agent1C
                        }
                        when {
                            beforeAgent true
                            expression { config.stageFlags.smoke }
                        }
                        steps {
                            timeout(time: config.timeoutOptions.smoke, unit: TimeUnit.MINUTES) {
                                unzipInfobase()

                                smoke config
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
                    expression { config.stageFlags.sonarqube }
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
