import groovy.transform.Field
import ru.pulsar.jenkins.library.configuration.JobConfiguration

import java.util.concurrent.TimeUnit

@Field
JobConfiguration config

@Field
def agent1C

void call() {

    //noinspection GroovyAssignabilityCheck
    pipeline {
        agent none

        options {
            buildDiscarder(logRotator(numToKeepStr: '30'))
            timeout(time: 2, unit: TimeUnit.HOURS)
            timestamps()
        }

        stages {

            stage('pre-stage') {
                agent {
                    label 'agent'
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
                            stage('Создание ИБ') {
                                steps {
                                    printLocation()

                                    installLocalDependencies()

                                    createDir('build/out')

                                    // Создание базы загрузкой конфигурации из хранилища
                                    initFromStorage config
                                }
                            }

                            stage('Инициализация ИБ') {
                                when {
                                    beforeAgent true
                                    expression { config.stageFlags.initSteps }
                                }
                                steps {
                                    // Инициализация и первичная миграция
                                    initInfobase config
                                }
                            }

                            stage('Архивация ИБ') {
                                steps {
                                    printLocation()

                                    zipInfobase()
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
                            expression { config.stageFlags.edtValidate }
                        }
                        steps {
                            edtTransform config
                        }
                    }
                }
            }

            stage('Проверка качества') {
                parallel {
                    stage('EDT контроль') {
                        agent {
                            label 'edt'
                        }
                        when {
                            beforeAgent true
                            expression { config.stageFlags.edtValidate }
                        }
                        steps {
                            edtValidate config
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
                            // Инициализация и первичная миграция
                            bdd config
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
                            syntaxCheck config
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
                            smoke config
                        }
                    }
                }
            }

            stage('Трансформация результатов') {
                agent {
                    label 'oscript'
                }
                when {
                    beforeAgent true
                    expression { config.stageFlags.edtValidate }
                }
                steps {
                    transform config
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
                    sonarScanner config
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
