package ru.pulsar.jenkins.library.utils

import com.cloudbees.groovy.cps.NonCPS
import hudson.AbortException
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry

import java.nio.charset.StandardCharsets

/**
 * Монопольный доступ к серверной базе на время операции: перед ней сеансы базы завершаются и начало новых
 * запрещается с кодом разрешения, сама операция получает тот же код, после нее запрет снимается, в том числе
 * при ошибке. Сеансами управляет кластер через RAS, поэтому для файловой базы и для серверной базы без адреса
 * RAS обертка ничего не делает: операция пройдет, только если в базе нет других сеансов.
 */
class ExclusiveAccess implements Serializable {

    /**
     * Выполняет тело с монопольным доступом к базе из конфигурации.
     * Тело получает параметр кода разрешения для команд vrunner, без управления сеансами - пустую строку.
     */
    static def run(JobConfiguration config, Closure body) {
        return run(config, 1, body)
    }

    /**
     * То же, но при ошибке команды завершение сеансов и тело повторяются раз в минуту, всего не больше attempts
     * попыток. Запрет начала сеансов между попытками не снимается, сборка получает ошибку последней попытки.
     */
    static def run(JobConfiguration config, int attempts, Closure body) {
        String connection = config.baseName()
        if (!InfobaseConnection.isServer(connection)) {
            return body.call('')
        }
        if (!config.rasAddress) {
            Logger.println("Адрес RAS (rasAddress) не указан, сеансы базы $connection не завершаются: " +
                "операция пройдет, только если в базе нет других сеансов")
            return body.call('')
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        String vrunnerPath = VRunner.getVRunnerPath()
        String sessionParameters = sessionParameters(config, connection, steps)
        String uccodeParameter = " --uccode ${permissionCode(steps)}"
        String killCommand = "$vrunnerPath session kill$sessionParameters$uccodeParameter"
        String unlockCommand = "$vrunnerPath session unlock$sessionParameters"

        return withClusterAdmin(config, steps) {
            def result
            int attempt = 1
            try {
                while (true) {
                    try {
                        VRunner.exec(killCommand)
                        result = body.call(uccodeParameter)
                        break
                    } catch (AbortException e) {
                        // Повторяется только ненулевой код возврата команды, прерывание по таймауту проходит дальше
                        if (attempt >= attempts) {
                            throw e
                        }
                        Logger.println("Монопольная операция с базой $connection не удалась " +
                            "(попытка $attempt из $attempts), повтор через минуту")
                        steps.sleep(60)
                        attempt++
                    }
                }
            } catch (Exception e) {
                // Запрет снимается и при ошибке, но сборка получает исходную ошибку операции
                try {
                    VRunner.exec(unlockCommand)
                } catch (Exception unlockException) {
                    Logger.println("Не удалось снять запрет начала сеансов базы $connection: ${unlockException.message}")
                }
                throw e
            }
            VRunner.exec(unlockCommand)
            return result
        }
    }

    // Код разрешения на сборку: одинаковый во всех стадиях сборки и разный у разных сборок
    private static String permissionCode(IStepExecutor steps) {
        String buildTag = steps.env().BUILD_TAG
        return UUID.nameUUIDFromBytes(buildTag.getBytes(StandardCharsets.UTF_8)).toString()
    }

    private static String sessionParameters(JobConfiguration config, String connection, IStepExecutor steps) {
        String parameters = " --db \"${InfobaseConnection.serverBaseName(connection)}\" --ras ${config.rasAddress}"
        parameters += clusterParameter(config, connection, steps)

        // Логин и пароль администратора базы, если в ней есть пользователи
        String vrunnerSettings = config.initInfoBaseOptions.vrunnerSettings
        if (steps.fileExists(vrunnerSettings)) {
            parameters += " --settings $vrunnerSettings"
        }

        return parameters
    }

    // Кластер с портом из строки подключения. Без порта vanessa-runner берет первый кластер RAS.
    private static String clusterParameter(JobConfiguration config, String connection, IStepExecutor steps) {
        String port = InfobaseConnection.serverPort(connection)
        if (!port) {
            return ''
        }

        String racOutput = steps.cmd("\"${racPath(config, steps)}\" cluster list ${config.rasAddress}", false, true)
        Map<String, String> clusters = clustersByPort(racOutput)
        String clusterId = clusters[port]
        if (!clusterId) {
            steps.error("На сервере администрирования ${config.rasAddress} нет кластера с портом $port " +
                "из строки подключения $connection. Найдены кластеры с портами: ${clusters.keySet().join(', ')}")
        }

        return " --cluster $clusterId"
    }

    // rac той же версии платформы, что и у vanessa-runner, находится через v8find, как dbgs для замеров покрытия
    private static String racPath(JobConfiguration config, IStepExecutor steps) {
        long timestamp = System.currentTimeMillis()
        String racFindScriptPath = "build/tmp/rac_${timestamp}.os"
        String racPathResult = "build/tmp/racPath_${timestamp}"

        steps.writeFile(racFindScriptPath, steps.libraryResource('rac.os'), 'UTF-8')
        steps.cmd("oscript $racFindScriptPath ${config.v8version} $racPathResult")

        String racPath = steps.readFile(racPathResult).trim()
        if (!racPath) {
            steps.error("Не найдена утилита rac платформы ${config.v8version} на агенте")
        }

        return racPath
    }

    // Кластеры из вывода rac cluster list: порт кластера -> идентификатор кластера
    @NonCPS
    static Map<String, String> clustersByPort(String racOutput) {
        Map<String, String> clusters = new LinkedHashMap<>()
        String clusterId = null
        racOutput.readLines().each { String line ->
            def matcher = line =~ /^\s*(cluster|port)\s*:\s*(\S+)\s*$/
            if (!matcher.matches()) {
                return
            }
            if (matcher.group(1) == 'cluster') {
                clusterId = matcher.group(2)
            } else if (clusterId != null) {
                clusters.put(matcher.group(2), clusterId)
                clusterId = null
            }
        }
        return clusters
    }

    private static def withClusterAdmin(JobConfiguration config, IStepExecutor steps, Closure body) {
        String credentialsId = CredentialSelection.resolve(steps, config.secrets.clusterAdmin, 'CLUSTER_ADMIN')
        if (!credentialsId) {
            return body.call()
        }

        // vanessa-runner берет логин и пароль администратора кластера из этих переменных окружения
        return steps.withCredentials([
            steps.usernamePassword(credentialsId, 'RUNNER_CLUSTERADMIN_USER', 'RUNNER_CLUSTERADMIN_PWD')
        ]) {
            body.call()
        }
    }
}
