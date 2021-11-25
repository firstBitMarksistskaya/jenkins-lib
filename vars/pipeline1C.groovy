/* groovylint-disable NestedBlockDepth */
import groovy.transform.Field
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat

import java.util.concurrent.TimeUnit

@Field
JobConfiguration config

@Field
String agent1C

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
                        agent1C = config.v8version
                    }
                }
            }

            stage('Подготовка') {
                parallel {
                    stage('Подготовка 1C базы') {
                        agent {
                            label agent1C
                        }
                        when {
                            beforeAgent true
                            expression { config.stageFlags.needInfobase() }
                        }

                        stages {
                            stage('Трансформация из формата EDT') {
                                agent {
                                    label 'edt'
                                }
                                when {
                                    beforeAgent true
                                    expression { config.stageFlags.needInfobase() && config.infobaseFromFiles() && config.sourceFormat == SourceFormat.EDT }
                                }
                                steps {
                                    timeout(time: config.stageTimeout, unit: TimeUnit.HOURS) {
                                        edtToDesignerFormatTransformation config
                                    }
                                }
                            }

                            stage('Создание ИБ') {
                                steps {
                                    timeout(time: config.stageTimeout, unit: TimeUnit.HOURS) {
                                        createDir('build/out')

                                        script {
                                            if (config.infobaseFromFiles()) {
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
                                    timeout(time: config.stageTimeout, unit: TimeUnit.HOURS) {
                                        // Инициализация и первичная миграция
                                        initInfobase config
                                    }
                                }
                            }

                            stage('Архивация ИБ') {
                                steps {
                                    timeout(time: config.stageTimeout, unit: TimeUnit.HOURS) {
                                        printLocation()

                                        zipInfobase()
                                    }
                                }

                            }
                        }

                    }

                    stage('Трансформация в формат EDT') {
                        agent {
                            label 'edt'
                        }
                        when {
                            beforeAgent true
                            expression { config.sourceFormat == SourceFormat.DESIGNER && config.stageFlags.edtValidate }
                        }
                        steps {
                            timeout(time: config.stageTimeout, unit: TimeUnit.HOURS) {
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
                                    label 'edt'
                                }
                                steps {
                                    timeout(time: config.stageTimeout, unit: TimeUnit.HOURS) {
                                        edtValidate config
                                    }
                                }
                            }

                            stage('Трансформация результатов') {
                                agent {
                                    label 'oscript'
                                }
                                steps {
                                    timeout(time: config.stageTimeout, unit: TimeUnit.HOURS) {
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
                            timeout(time: config.stageTimeout, unit: TimeUnit.HOURS) {
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
                            timeout(time: config.stageTimeout, unit: TimeUnit.HOURS) {
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
                            timeout(time: config.stageTimeout, unit: TimeUnit.HOURS) {
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
                    timeout(time: config.stageTimeout, unit: TimeUnit.HOURS) {
                        sonarScanner config
                    }
                }
            }
        }

        post('post-stage') {
            always {
                node('agent') {
                    saveResults config
                }
            }
        }
    }

}
