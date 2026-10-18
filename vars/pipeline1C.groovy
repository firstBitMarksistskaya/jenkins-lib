/* groovylint-disable NestedBlockDepth */
import groovy.transform.Field
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.utils.InfobaseConnection
import ru.pulsar.jenkins.library.utils.InfobaseLocks
import ru.pulsar.jenkins.library.utils.RepoUtils

import java.util.concurrent.TimeUnit

@Field
JobConfiguration config

@Field
String agent1C

@Field
String agentEdt

@Field
// Успешность инициализации баз сборки по InfobaseConnection.key. База без записи считается готовой:
// шаги инициализации на ней не выполнялись
Map<String, Boolean> infobasesInitialized = [:]

@Field
// Ресурсы внешней блокировки баз сборки. Аргументы шагов в options вычисляются в начале сборки,
// поэтому список создается заранее, а заполняется в pre-stage
List<Map<String, String>> infobaseLockResources = []

@Field
// Ресурсы внутренних блокировок тестовых стадий, заполняются в pre-stage
Map<String, List<Map<String, String>>> stageLockResources = [bdd: [], syntaxCheck: [], smoke: [], yaxunit: []]

// Успешна ли инициализация базы, на которой идет тестовая стадия
boolean infobaseInitialized(String stageName) {
    return infobasesInitialized[InfobaseConnection.key(config.baseName(stageName))] != false
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
                        applyDebugOverridesIfNeeded()
                        config = jobConfiguration() as JobConfiguration
                        config.validateInitMethod()
                        config.validateStageBases()
                        agent1C = config.v8AgentLabel()
                        agentEdt = config.edtAgentLabel()
                        RepoUtils.computeRepoSlug(env.GIT_URL)
                        InfobaseLocks.fill(config, env.BUILD_TAG, infobaseLockResources, stageLockResources)
                    }
                }
            }

            stage('Подготовка и проверка качества') {
                // Базы сборки заняты от начала подготовки до окончания последнего теста
                options {
                    lock(extra: infobaseLockResources)
                }

                stages {
                    // Результат трансформации нужен подготовке всех баз сборки. С трансформацией в формат EDT
                    // она не пересекается: они выполняются для разных форматов исходников
                    stage('Трансформация из формата EDT') {
                        agent {
                            label agentEdt
                        }
                        when {
                            beforeAgent true
                            expression { config.stageFlags.needInfoBase() && (config.infoBaseFromFiles() || config.needLoadExtensions()) && config.sourceFormat == SourceFormat.EDT }
                        }
                        steps {
                            timeout(time: config.timeoutOptions.edtToDesignerFormatTransformation, unit: TimeUnit.MINUTES) {
                                edtToDesignerFormatTransformation config
                            }
                        }
                    }

                    stage('Подготовка') {
                        parallel {
                            stage('Подготовка 1С базы') {
                                // Если все тестовые стадии идут на своих базах, здесь только собираются расширения
                                when {
                                    beforeAgent true
                                    expression { config.stageFlags.needInfoBase() && (config.buildBaseNeeded() || config.needLoadExtensions()) }
                                }

                                stages {
                                    stage('Подготовка ИБ') {
                                        agent {
                                            label agent1C
                                        }

                                        stages {
                                            stage('Сборка расширений из исходников') {
                                                when {
                                                    expression { config.needLoadExtensions() }
                                                }
                                                steps {
                                                    restoreDebugOverridesIfNeeded()
                                                    timeout(time: config.timeoutOptions.getBinaries, unit: TimeUnit.MINUTES) {
                                                        createDir('build/out/cfe')
                                                        // Соберем или загрузим cfe из исходников и положим их в папку build/out/cfe
                                                        getExtensions config
                                                    }
                                                }
                                            }
                                            stage('Создание ИБ') {
                                                when {
                                                    expression { config.buildBaseNeeded() }
                                                }
                                                steps {
                                                    timeout(time: config.timeoutOptions.createInfoBase, unit: TimeUnit.MINUTES) {
                                                        createDir('build/out/')
                                                            createInfobase config
                                                    }
                                                }
                                            }

                                            stage('Загрузка конфигурации') {
                                                when {
                                                    beforeAgent true
                                                    expression { config.buildBaseNeeded() && config.needLoadConfiguration() }
                                                }
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

                                            stage('Загрузка расширений в конфигурацию') {
                                                when {
                                                    beforeAgent true
                                                    expression { config.buildBaseNeeded() && config.needLoadExtensions() }
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
                                                    expression { config.buildBaseNeeded() && config.stageFlags.initSteps }
                                                }
                                                steps {
                                                    timeout(time: config.timeoutOptions.initInfoBase, unit: TimeUnit.MINUTES) {
                                                        // Инициализация и первичная миграция
                                                        script {
                                                            infobasesInitialized[InfobaseConnection.key(config.baseName())] = initInfobase config
                                                        }
                                                    }
                                                }
                                            }


                                            // Локальную базу тестовые стадии получают из архива, внешнюю - по адресу:
                                            // ее архив нужен, только если включено сохранение базы
                                            stage('Архивация ИБ') {
                                                when {
                                                    beforeAgent true
                                                    expression { config.buildBaseNeeded() && (config.basePath == '' || config.initInfoBaseOptions.archiveInfobase?.savingEnabled()) }
                                                }
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

                            stage('Подготовка баз стадий') {
                                when {
                                    beforeAgent true
                                    expression { !config.stageBases().isEmpty() }
                                }
                                steps {
                                    script {
                                        // Число баз известно только во время сборки, поэтому ветки создаются здесь,
                                        // а не описываются в parallel. Каждая база готовится на своем агенте
                                        Map<String, Closure> branches = [:]
                                        for (String base : config.stageBases()) {
                                            String stageBase = base
                                            branches[stageBase] = {
                                                node(agent1C) {
                                                    infobasesInitialized[InfobaseConnection.key(stageBase)] =
                                                        prepareInfobase(config.withBasePath(stageBase))
                                                }
                                            }
                                        }
                                        parallel branches
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
                                    beforeOptions true
                                    expression { config.stageFlags.bdd && infobaseInitialized('bdd') }
                                }
                                // Тестовые стадии на одной базе идут по очереди
                                options {
                                    lock(extra: stageLockResources.bdd)
                                }
                                stages {
                                    stage('Распаковка ИБ') {
                                        steps {
                                            restoreDebugOverridesIfNeeded()
                                            unzipInfobase config.forStage('bdd')
                                        }
                                    }

                                    stage('Загрузка расширений в конфигурацию') {
                                        when {
                                            beforeAgent true
                                            expression { config.needLoadExtensions('bdd') }
                                        }
                                        steps {
                                            timeout(time: config.timeoutOptions.loadExtensions, unit: TimeUnit.MINUTES) {
                                                loadExtensions config.forStage('bdd'), 'bdd'
                                            }
                                        }
                                    }

                                    stage('Выполнение BDD сценариев') {
                                        steps {
                                            timeout(time: config.timeoutOptions.bdd, unit: TimeUnit.MINUTES) {
                                                bdd config.forStage('bdd')
                                            }
                                        }
                                    }

                                    stage('Архивация ИБ') {
                                        when {
                                            beforeAgent true
                                            expression { config.forStage('bdd').basePath == '' || config.bddOptions.archiveInfobase?.savingEnabled() }
                                        }
                                        steps {
                                            timeout(time: config.timeoutOptions.zipInfoBase, unit: TimeUnit.MINUTES) {
                                                printLocation()

                                                zipInfobase config.forStage('bdd'), 'bdd'
                                            }
                                        }
                                    }
                                }
                                // Служебные расширения стадии нужны только ей: удаляются и при падении стадии.
                                // post выполняется на агенте стадии под ее блокировкой базы
                                post {
                                    always {
                                        timeout(time: config.timeoutOptions.loadExtensions, unit: TimeUnit.MINUTES) {
                                            removeStageExtensions config.forStage('bdd'), 'bdd'
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
                                    beforeOptions true
                                    expression { config.stageFlags.syntaxCheck }
                                }
                                options {
                                    lock(extra: stageLockResources.syntaxCheck)
                                }
                                stages {
                                    stage('Распаковка ИБ') {
                                        steps {
                                            restoreDebugOverridesIfNeeded()
                                            unzipInfobase config.forStage('syntaxCheck')
                                        }
                                    }

                                    stage('Выполнение синтаксического контроля') {
                                        steps {
                                            timeout(time: config.timeoutOptions.syntaxCheck, unit: TimeUnit.MINUTES) {
                                                syntaxCheck config.forStage('syntaxCheck')
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
                                    beforeOptions true
                                    expression { config.stageFlags.smoke && infobaseInitialized('smoke') }
                                }
                                options {
                                    lock(extra: stageLockResources.smoke)
                                }
                                stages {
                                    stage('Распаковка ИБ') {
                                        steps {
                                            restoreDebugOverridesIfNeeded()
                                            unzipInfobase config.forStage('smoke')
                                        }
                                    }

                                    stage('Загрузка расширений в конфигурацию') {
                                        when {
                                            beforeAgent true
                                            expression { config.needLoadExtensions('smoke') }
                                        }
                                        steps {
                                            timeout(time: config.timeoutOptions.loadExtensions, unit: TimeUnit.MINUTES) {
                                                loadExtensions config.forStage('smoke'), 'smoke'
                                            }
                                        }
                                    }

                                    stage('Выполнение дымовых тестов') {
                                        steps {
                                            timeout(time: config.timeoutOptions.smoke, unit: TimeUnit.MINUTES) {
                                                smoke config.forStage('smoke')
                                            }
                                        }
                                    }
                                }
                                post {
                                    always {
                                        timeout(time: config.timeoutOptions.loadExtensions, unit: TimeUnit.MINUTES) {
                                            removeStageExtensions config.forStage('smoke'), 'smoke'
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
                                    beforeOptions true
                                    expression { config.stageFlags.yaxunit && infobaseInitialized('yaxunit') }
                                }
                                options {
                                    lock(extra: stageLockResources.yaxunit)
                                }
                                stages {
                                    stage('Распаковка ИБ') {
                                        steps {
                                            restoreDebugOverridesIfNeeded()
                                            unzipInfobase config.forStage('yaxunit')
                                        }
                                    }

                                    stage('Загрузка расширений в конфигурацию') {
                                        when {
                                            beforeAgent true
                                            expression { config.needLoadExtensions('yaxunit') }
                                        }
                                        steps {
                                            timeout(time: config.timeoutOptions.loadExtensions, unit: TimeUnit.MINUTES) {
                                                loadExtensions config.forStage('yaxunit'), 'yaxunit'
                                            }
                                        }
                                    }

                                    stage('Выполнение YAXUnit тестов') {
                                        steps {
                                            timeout(time: config.timeoutOptions.yaxunit, unit: TimeUnit.MINUTES) {
                                                yaxunit config.forStage('yaxunit')
                                            }
                                        }
                                    }
                                }
                                post {
                                    always {
                                        timeout(time: config.timeoutOptions.loadExtensions, unit: TimeUnit.MINUTES) {
                                            removeStageExtensions config.forStage('yaxunit'), 'yaxunit'
                                        }
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
                    expression { config.stageFlags.sonarqube }
                }
                steps {
                    restoreDebugOverridesIfNeeded()
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
