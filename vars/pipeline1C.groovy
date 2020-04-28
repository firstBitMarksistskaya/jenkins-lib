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

            stage('1C') {
                agent {
                    label agent1C
                }

                stages {
                    stage('Подготовка базы') {
                        steps {
                            printLocation()

                            installLocalDependencies()

                            dir("build/out") { echo '' }

                            // Создание базы загрузкой конфигурации из хранилища
                            initFromStorage config

                            zipInfobase()
                        }
                    }

                    stage('Проверка качества') {
                        parallel {
                            stage('EDT контроль') {
                                agent {
                                    label 'edt'
                                }
                                steps {
                                    edtValidate config
                                }
                            }

                            stage('Синтаксический контроль') {
                                steps {
                                    syntaxCheck config
                                }
                            }

                            stage('Дымовые тесты') {
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
                        steps {
                            transform config
                        }
                    }
                }
            }

            stage('SonarQube') {
                agent {
                    label 'sonar'
                }
                steps {
                    sonarScanner config
                }
            }
        }
    }

}
