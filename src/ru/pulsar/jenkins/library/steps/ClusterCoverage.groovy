package ru.pulsar.jenkins.library.steps

import com.cloudbees.groovy.cps.NonCPS
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.CredentialSelection
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.InfobaseConnection
import ru.pulsar.jenkins.library.utils.Logger

/**
 * Замеры покрытия на серверной базе. Серверный код выполняется в кластере, и dbgs на агенте его не видит.
 * Его видит только сервер отладки, который поднимает агент сервера 1С с ключами -debug -http, поэтому
 * Coverage41C подключается к нему по адресу и имени базы в кластере, а dbgs на агенте не запускается.
 * Блокировка порта сервера отладки на агенте не нужна: стадии на одной базе идут по очереди
 * под внутренней блокировкой базы.
 */
class ClusterCoverage implements Serializable {

    // Coverage41C читает пароль сервера отладки из переменной окружения с этим именем
    private static final String PASSWORD_VARIABLE = 'COVERAGE41C_DEBUG_SERVER_PASSWORD'

    // Порт сервера отладки агента сервера 1С по умолчанию (-debugServerPort)
    private static final String DEFAULT_DEBUG_SERVER_PORT = '1550'

    static def run(JobConfiguration config, Coverable stage, Closure body) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String debugServer = config.coverageOptions.clusterDebugServer
        if (!debugServer) {
            steps.error("Для замеров покрытия на серверной базе ${config.baseName()} укажите адрес сервера отладки " +
                "кластера (coverage -> clusterDebugServer). dbgs на агенте Jenkins не видит серверный код, " +
                "который выполняется в кластере, и покрытие получилось бы без него.")
        }

        String coverage41CPath = config.coverageOptions.coverage41CPath
        // start, check и stop находят процесс замеров по одинаковым -i и -u
        String connection = " -i ${InfobaseConnection.serverBaseName(config.baseName())} -u ${debugServerUrl(debugServer)}"

        startCoverage(steps, config, stage, connection)

        def result
        try {
            result = body()
        } catch (Exception e) {
            // Coverage41C, оставшийся подключенным к серверу отладки, не даст подключиться следующему замеру
            try {
                steps.cmd("$coverage41CPath stop$connection")
            } catch (Exception stopException) {
                Logger.println("Не удалось остановить Coverage41C: ${stopException.message}")
            }
            throw e
        }

        steps.cmd("$coverage41CPath stop$connection")
        steps.stash(stage.getCoverageStashName(), stage.getCoverageStashPath(), true)

        return result
    }

    private static void startCoverage(IStepExecutor steps, JobConfiguration config, Coverable stage, String connection) {
        def env = steps.env()
        def workspaceDir = FileUtils.getFilePath("$env.WORKSPACE")
        String coverage41CPath = config.coverageOptions.coverage41CPath

        String startParameters = "start$connection -P $workspaceDir -s ${config.srcDir} -o ${stage.getCoverageStashPath()}"
        Logger.println("Замеры покрытия ${stage.getStageSlug()} на сервере отладки кластера:$connection")

        // Пароль принимает только start, check и stop этот ключ не принимают
        String passwordCredentials = CredentialSelection.resolve(steps, config.secrets.debugServerPassword, 'DEBUG_SERVER_PASSWORD')
        if (passwordCredentials) {
            steps.withCredentials([steps.string(passwordCredentials, PASSWORD_VARIABLE)]) {
                steps.start(coverage41CPath, "$startParameters -p:env $PASSWORD_VARIABLE")
            }
        } else {
            steps.start(coverage41CPath, startParameters)
        }

        sleep(1000)
        steps.cmd("$coverage41CPath check$connection")
    }

    // Адрес сервера отладки для Coverage41C: http://хост:порт. Порт по умолчанию тот же, что у -debugServerPort
    @NonCPS
    static String debugServerUrl(String debugServer) {
        String address = debugServer.trim()
        String scheme = 'http://'
        for (String prefix : ['http://', 'https://']) {
            if (address.startsWith(prefix)) {
                scheme = prefix
                address = address.substring(prefix.length())
            }
        }

        int pathStart = address.indexOf('/')
        String host = pathStart >= 0 ? address.substring(0, pathStart) : address
        String path = pathStart >= 0 ? address.substring(pathStart) : ''
        if (!host.contains(':')) {
            host += ":$DEFAULT_DEBUG_SERVER_PORT"
        }
        return "$scheme$host$path".toString()
    }
}
