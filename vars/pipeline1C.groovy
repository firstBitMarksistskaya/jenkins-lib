import ru.pulsar.jenkins.library.configuration.JobConfiguration

import java.util.concurrent.TimeUnit

void call() {

    def config = jobConfiguration() as JobConfiguration
    def agent1C = config.v8version

    //noinspection GroovyAssignabilityCheck
    pipeline {
        agent none

        options {
            buildDiscarder(logRotator(numToKeepStr: '30'))
            timeout(time: 2, unit: TimeUnit.HOURS)
            timestamps()
        }

        environment {
            STORAGE_PATH = credentials(jobConfiguration.secrets.storagePath)
            STORAGE = credentials(jobConfiguration.secrets.storage)
        }

        stages {

            stage('pipeline1C') {
                parallel {
                    stage('SonarQube') {
                        agent {
                            label 'sonar'
                        }

                        steps {
                            printLocation()

                            sonarScanner()
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

                                    script {
                                        def storageVersion = versionParser.storage()

                                        // Создание базы загрузкой конфигурации из хранилища
                                        cmd "oscript_modules/bin/vrunner init-dev --storage --storage-name $STORAGE_PATH --storage-user $STORAGE_USR --storage-pwd $STORAGE_PSW --storage-ver $storageVersion --ibconnection \"/F./build/ib\""
                                    }

                                    zipInfobase()
                                }
                            }

                            stages {
                                parallel {
                                    stage('Синтаксический контроль') {
                                        steps {
                                            printLocation()

                                            installLocalDependencies()

                                            unzipInfobase()

                                            // Запуск синтакс-проверки
                                            cmd("oscript_modules/bin/vrunner syntax-check --settings tools/vrunner.json", true)

                                            junit allowEmptyResults: true, testResults: 'build/out/junitsyntax.xml'
                                        }
                                    }

                                    stage('Дымовые тесты') {
                                        steps {
                                            printLocation()

                                            installLocalDependencies()

                                            unzipInfobase()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}