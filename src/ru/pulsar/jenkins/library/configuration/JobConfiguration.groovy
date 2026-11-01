package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.InfobaseConnection

@JsonIgnoreProperties(ignoreUnknown = true)
class JobConfiguration implements Serializable, Cloneable {
    @JsonPropertyDescription("Версия платформы 1С:Предприятие в формате 8.3.хх.хххх.")
    String v8version

    @JsonPropertyDescription("Версия модуля 1C:Enterprise Development Tools формате xxxx.x.x:x86_64")
    String edtVersion

    @JsonPropertyDescription("Путь к корневому каталогу с исходниками конфигурации, в случае хранения исходников в формате EDT, необходимо указать путь к проекту")
    String srcDir

    @JsonPropertyDescription("Строка подключения к информационной базе в формате `--ibconnection` (например, `/Sсервер\\\\база`, `/Sсервер:порт\\\\база` или `/Fабсолютный_путь`). Если не указано, используется файловая база `./build/ib`, создаваемая на каждой сборке")
    String basePath

    @JsonPropertyDescription("""Адрес сервера администрирования RAS кластера серверной базы в формате хост:порт.
    Через него на время операций, которым нужен монопольный доступ, завершаются сеансы базы и запрещается начало новых.
    Если не указан, сеансы не завершаются: такие операции пройдут, только если в базе нет других сеансов.
    Если RAS обслуживает несколько кластеров, укажите порт кластера в basePath, иначе используется первый кластер.
    """)
    String rasAddress

    @JsonPropertyDescription("Формат исходников конфигурации")
    @JsonProperty(defaultValue = "designer")
    SourceFormat sourceFormat

    @JsonProperty("stages")
    @JsonPropertyDescription("Включение этапов сборок")
    StageFlags stageFlags

    @JsonProperty("timeout")
    @JsonPropertyDescription("Настройка таймаутов для шагов")
    TimeoutOptions timeoutOptions

    @JsonPropertyDescription("Имя ветки по умолчанию. Значение по умолчанию - main.")
    String defaultBranch

    @JsonPropertyDescription("Идентификаторы сохраненных секретов")
    Secrets secrets

    @JsonProperty("initInfobase")
    @JsonPropertyDescription("Настройки шага инициализации ИБ")
    InitInfoBaseOptions initInfoBaseOptions

    @JsonProperty("bdd")
    @JsonPropertyDescription("Настройки шага запуска BDD сценариев")
    BddOptions bddOptions

    @JsonProperty("sonarqube")
    @JsonPropertyDescription("Настройки анализа SonarQube")
    SonarQubeOptions sonarQubeOptions

    @JsonProperty("syntaxCheck")
    @JsonPropertyDescription("Настройки синтаксического контроля")
    SyntaxCheckOptions syntaxCheckOptions

    @JsonProperty("smoke")
    @JsonPropertyDescription("Настройки дымового тестирования")
    SmokeTestOptions smokeTestOptions

    @JsonProperty("coverage")
    @JsonPropertyDescription("Настройки замеров покрытия")
    GlobalCoverageOptions coverageOptions

    @JsonProperty("yaxunit")
    @JsonPropertyDescription("Настройки YAXUnit")
    YaxunitOptions yaxunitOptions

    @JsonProperty("resultsTransform")
    @JsonPropertyDescription("Настройки трансформации результатов анализа")
    ResultsTransformOptions resultsTransformOptions

    @JsonProperty("notifications")
    @JsonPropertyDescription("Настройки рассылки результатов сборки")
    NotificationsOptions notificationsOptions

    @JsonProperty("logosConfig")
    @JsonPropertyDescription("Конфигурация библиотеки logos. Применяется перед запуском каждой стадии сборки")
    String logosConfig

    @Override
    @NonCPS
    String toString() {
        return "JobConfiguration{" +
            "v8version='" + v8version + '\'' +
            ", edtVersion='" + edtVersion + '\'' +
            ", srcDir='" + srcDir + '\'' +
            ", basePath='" + basePath + '\'' +
            ", rasAddress='" + rasAddress + '\'' +
            ", sourceFormat=" + sourceFormat +
            ", stageFlags=" + stageFlags +
            ", timeoutOptions=" + timeoutOptions +
            ", defaultBranch='" + defaultBranch + '\'' +
            ", secrets=" + secrets +
            ", initInfoBaseOptions=" + initInfoBaseOptions +
            ", bddOptions=" + bddOptions +
            ", sonarQubeOptions=" + sonarQubeOptions +
            ", syntaxCheckOptions=" + syntaxCheckOptions +
            ", smokeTestOptions=" + smokeTestOptions +
            ", coverageOptions=" + coverageOptions +
            ", yaxunitOptions=" + yaxunitOptions +
            ", resultsTransformOptions=" + resultsTransformOptions +
            ", notificationOptions=" + notificationsOptions +
            ", logosConfig='" + logosConfig + '\'' +
            '}'
    }

    boolean infoBaseFromFiles() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env()
        String branchName = env.BRANCH_NAME
        def initMethod = initInfoBaseOptions.initMethod

        return (initMethod == InitInfoBaseMethod.FROM_SOURCE) ||
            (initMethod == InitInfoBaseMethod.DEFAULT_BRANCH_FROM_STORAGE && branchName != defaultBranch)
    }

    boolean needLoadExtensions(String stageName = "") {
        if (stageName.isEmpty()) {
            return initInfoBaseOptions.extensions.length != 0
        } else {
            return initInfoBaseOptions.extensions.any { extension ->
                extension.stages.contains(stageName)
            }
        }
    }

    // Расширения отдельных стадий: подключаются только на своих стадиях, а не при инициализации базы.
    // Тип элементов не указан: Jenkins компилирует JobConfiguration раньше InitInfoBaseOptions
    // и не находит вложенный класс Extension
    List stageExtensions() {
        return initInfoBaseOptions.extensions.findAll { extension ->
            extension.stages.length != 0 && !extension.stages.contains('initInfoBase')
        } as List
    }

    // При способе инициализации notInit конфигурация не загружается: она уже есть в базе из шаблона или в существующей базе
    boolean needLoadConfiguration() {
        return initInfoBaseOptions.initMethod != InitInfoBaseMethod.NOT_INIT
    }

    void validateInitMethod() {
        // Если все тестовые стадии идут на своих базах, локальная база сборки не создается
        if (needLoadConfiguration() || templateDBLoaded() || basePath || !buildBaseNeeded()) {
            return
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        steps.error("Способ инициализации notInit (initInfobase -> initMethod) не загружает конфигурацию в базу, " +
            "а шаблон базы (initInfobase -> templateDBPath) и существующая база (basePath) не указаны. " +
            "В пустой базе, созданной пайплайном, конфигурации нет. " +
            "Укажите templateDBPath или basePath либо выберите другой способ инициализации.")
    }

    boolean templateDBLoaded() {
        return initInfoBaseOptions.templateDBPath != null && !initInfoBaseOptions.templateDBPath.isEmpty()
    }

    String v8AgentLabel() {
        return v8version
    }

    String edtAgentLabel() {
        String edtVersionForRing = "edt"
        if (edtVersion != '') {
            edtVersionForRing += "@" + edtVersion
        }
        return edtVersionForRing
    }

    @NonCPS
    String baseName(){
        String base = basePath
        if (basePath == '') {
            base = "/F./build/ib"
        }

        return base
    }

    // Строка подключения к базе тестовой стадии: своя база стадии, если она задана, иначе база сборки
    @NonCPS
    String baseName(String stageName) {
        String stageBase = stageBasePath(stageName)
        return stageBase ?: baseName()
    }

    // Своя база тестовой стадии из ее настроек или пустая строка
    @NonCPS
    String stageBasePath(String stageName) {
        String stageBase
        switch (stageName) {
            case 'bdd':
                stageBase = bddOptions?.basePath
                break
            case 'syntaxCheck':
                stageBase = syntaxCheckOptions?.basePath
                break
            case 'smoke':
                stageBase = smokeTestOptions?.basePath
                break
            case 'yaxunit':
                stageBase = yaxunitOptions?.basePath
                break
            default:
                stageBase = null
        }
        return stageBase?.trim() ?: ''
    }

    /**
     * Настройки, с которыми шаги тестовой стадии работают с ее базой: копия конфигурации, в которой basePath -
     * своя база стадии. Шаги берут базу из basePath и не знают, на какой базе идет стадия. Если своей базы
     * у стадии нет, возвращается эта же конфигурация.
     */
    @NonCPS
    JobConfiguration forStage(String stageName) {
        String stageBase = stageBasePath(stageName)
        return stageBase ? withBasePath(stageBase) : this
    }

    // Копия конфигурации для работы с другой базой. Вложенные настройки общие: шаги их не меняют
    @NonCPS
    JobConfiguration withBasePath(String base) {
        JobConfiguration copy = (JobConfiguration) clone()
        copy.basePath = base
        return copy
    }

    // Включенные тестовые стадии в порядке пайплайна
    @NonCPS
    List<String> enabledTestStages() {
        List<String> stages = []
        if (stageFlags.bdd) {
            stages.add('bdd')
        }
        if (stageFlags.syntaxCheck) {
            stages.add('syntaxCheck')
        }
        if (stageFlags.smoke) {
            stages.add('smoke')
        }
        if (stageFlags.yaxunit) {
            stages.add('yaxunit')
        }
        return stages
    }

    // База сборки нужна, если на ней идет хотя бы одна включенная тестовая стадия.
    // Без тестовых стадий базу сборки готовят шаги инициализации
    @NonCPS
    boolean buildBaseNeeded() {
        List<String> stages = enabledTestStages()
        String buildBase = InfobaseConnection.key(baseName())
        return stages.isEmpty() || stages.any { InfobaseConnection.key(baseName(it)) == buildBase }
    }

    // Свои базы включенных тестовых стадий, кроме базы сборки, по одной на базу: каждую готовит своя ветка
    @NonCPS
    List<String> stageBases() {
        String buildBase = InfobaseConnection.key(baseName())
        List<String> keys = [buildBase]
        List<String> bases = []
        for (String stageName : enabledTestStages()) {
            String stageBase = stageBasePath(stageName)
            if (!stageBase) {
                continue
            }
            String key = InfobaseConnection.key(stageBase)
            if (!keys.contains(key)) {
                keys.add(key)
                bases.add(stageBase)
            }
        }
        return bases
    }

    // База стадии готовится отдельно от локальной базы сборки на другом агенте, поэтому путь файловой базы
    // должен быть абсолютным: относительный путь на разных агентах означает разные базы
    void validateStageBases() {
        for (String stageName : enabledTestStages()) {
            String stageBase = stageBasePath(stageName)
            if (stageBase && !InfobaseConnection.isServer(stageBase) && InfobaseConnection.lockResource(stageBase) == null) {
                IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
                steps.error("База стадии $stageName ($stageName -> basePath) $stageBase задана относительным путем. " +
                    "Базу стадии пайплайн готовит и тестирует на разных агентах: укажите серверную базу " +
                    "или абсолютный путь к файловой базе, доступный всем агентам.")
            }
        }
    }
}