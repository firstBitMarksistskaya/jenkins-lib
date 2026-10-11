package ru.pulsar.jenkins.library.utils

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry

import java.nio.charset.StandardCharsets

import static ru.pulsar.jenkins.library.configuration.Secrets.UNKNOWN_ID

/**
 * Монопольный доступ к серверной базе на время операции: перед ней сеансы базы завершаются и начало новых
 * запрещается с кодом разрешения, сама операция получает тот же код, после нее запрет снимается, в том числе
 * при ошибке. Сеансами управляет кластер через RAS, поэтому для файловой базы обертка ничего не делает.
 */
class ExclusiveAccess implements Serializable {

    /**
     * Выполняет тело с монопольным доступом к базе из конфигурации.
     * Тело получает параметр кода разрешения для команд vrunner, для файловой базы - пустую строку.
     */
    static def run(JobConfiguration config, Closure body) {
        String connection = config.baseName()
        if (!InfobaseConnection.isServer(connection)) {
            return body.call('')
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        String uccodeParameter = " --uccode ${permissionCode(steps)}"
        String killCommand = sessionCommand('kill', config, connection, steps) + uccodeParameter
        String unlockCommand = sessionCommand('unlock', config, connection, steps)

        return withClusterAdmin(config, steps) {
            def result
            try {
                VRunner.exec(killCommand)
                result = body.call(uccodeParameter)
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

    private static String sessionCommand(String action, JobConfiguration config, String connection, IStepExecutor steps) {
        String command = "${VRunner.getVRunnerPath()} session $action --db \"${InfobaseConnection.serverBaseName(connection)}\""
        if (config.rasAddress) {
            command += " --ras ${config.rasAddress}"
        }

        // Логин и пароль администратора базы, если в ней есть пользователи
        String vrunnerSettings = config.initInfoBaseOptions.vrunnerSettings
        if (steps.fileExists(vrunnerSettings)) {
            command += " --settings $vrunnerSettings"
        }

        return command
    }

    private static def withClusterAdmin(JobConfiguration config, IStepExecutor steps, Closure body) {
        String credentialsId = config.secrets.clusterAdmin
        if (!credentialsId || credentialsId == UNKNOWN_ID) {
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
