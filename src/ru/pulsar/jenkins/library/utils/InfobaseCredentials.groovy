package ru.pulsar.jenkins.library.utils

import com.cloudbees.groovy.cps.NonCPS
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry

/**
 * Пользователь информационной базы из учетных данных Jenkins вида "username with password" для команд vanessa-runner.
 * vanessa-runner берет пользователя и пароль из переменных окружения RUNNER_DBUSER и RUNNER_DBPWD раньше, чем из
 * файла настроек --settings, поэтому под учетными данными пароль из файла настроек не используется, в том числе
 * при ошибке входа. Если учетные данные не заданы и стандартных нет, команды, как и раньше, берут пользователя
 * и пароль из файлов настроек.
 */
class InfobaseCredentials implements Serializable {

    static final String USER_VARIABLE = 'RUNNER_DBUSER'
    static final String PASSWORD_VARIABLE = 'RUNNER_DBPWD'
    static final String STANDARD_ID_SUFFIX = '_INFOBASE_USER'

    private static final String LOCAL_EVENT_LOG_DIR = 'build/ib/1Cv8Log'

    /**
     * Есть ли пользователи в базе при ее подготовке: в базе из шаблона и во внешней базе есть, в новой пустой
     * базе их нет, и вход под пользователем в нее не проходит
     */
    static boolean preparedBaseHasUsers(JobConfiguration config) {
        return config.templateDBLoaded() || config.basePath
    }

    /** Есть ли пользователи в базе стадии: тестовые стадии работают с подготовленной базой */
    static boolean stageBaseHasUsers(JobConfiguration config, String stageName) {
        return stageName != 'initInfoBase' || preparedBaseHasUsers(config)
    }

    /** То же, что withCredentials(config, true, body): для базы, в которой есть пользователи */
    static def withCredentials(JobConfiguration config, Closure body) {
        return withCredentials(config, true, body)
    }

    /**
     * Выполняет тело под учетными данными пользователя ИБ: явными из secrets.infobaseUser или стандартными
     * {slug}_INFOBASE_USER. Тело получает признак, что учетные данные привязаны. В базу без пользователей
     * команды входят без них.
     */
    static def withCredentials(JobConfiguration config, boolean baseHasUsers, Closure body) {
        if (!baseHasUsers) {
            return body.call(false)
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        Logger.println("Пользователь информационной базы для vanessa-runner")
        String credentialsId = CredentialSelection.resolve(steps, config.secrets.infobaseUser, standardId())
        if (!credentialsId) {
            return body.call(false)
        }

        return steps.withCredentials([steps.usernamePassword(credentialsId, USER_VARIABLE, PASSWORD_VARIABLE)]) {
            String problem = credentialsProblem(steps.env())
            if (problem) {
                steps.error("Учетные данные пользователя информационной базы $credentialsId: $problem")
            }

            // Переменную с пустым значением Jenkins не передает, и vanessa-runner взял бы пароль из файла
            // настроек. Значение "" он принимает как явно пустой пароль
            List<String> emptyPassword = isPasswordEmpty(steps.env()) ? ["$PASSWORD_VARIABLE=\"\"".toString()] : []
            return steps.withEnv(emptyPassword) {
                body.call(true)
            }
        }
    }

    /**
     * Причина, по которой пароль нельзя передать клиенту тестирования, или null. Клиенты тестирования VA и ADD
     * запускаются командой ОС с паролем в кавычках, на Linux - через командную оболочку
     */
    @NonCPS
    static String testClientPasswordProblem(def env, boolean unix) {
        String password = envValue(env, PASSWORD_VARIABLE) ?: ''
        if (unix && password =~ /[$`\\]/) {
            return 'пароль пользователя ИБ не должен содержать символы $, ` и \\: ' +
                'на Linux клиент тестирования запускается через командную оболочку'
        }
        return null
    }

    /**
     * VA и ADD записывают строку запуска или параметры подключения клиента тестирования вместе с паролем
     * в журнал регистрации базы. Журнал локальной базы сборки удаляется: в архив базы он не входит, но остается
     * в рабочем каталоге. Журнал внешней базы не удаляется
     */
    static void clearTestClientEventLog(JobConfiguration config) {
        if (config.basePath) {
            return
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        try {
            steps.deleteDir(LOCAL_EVENT_LOG_DIR)
        } catch (Exception e) {
            Logger.println("Не удалось удалить журнал регистрации локальной базы $LOCAL_EVENT_LOG_DIR: ${e.message}")
        }
    }

    static String standardId() {
        return (RepoUtils.getRepoSlug() ?: '') + STANDARD_ID_SUFFIX
    }

    @NonCPS
    static boolean isPasswordEmpty(def env) {
        String password = envValue(env, PASSWORD_VARIABLE)
        return password == null || password.isEmpty() || password == '""'
    }

    // Значение переменной окружения или null, если ее нет
    @NonCPS
    static String envValue(def env, String name) {
        try {
            return env."$name"
        } catch (MissingPropertyException ignored) {
            return null
        }
    }

    // Причина, по которой учетные данные нельзя передать vanessa-runner, или null. Значения в текст не попадают
    @NonCPS
    static String credentialsProblem(def env) {
        String user = envValue(env, USER_VARIABLE) ?: ''
        String password = envValue(env, PASSWORD_VARIABLE) ?: ''
        if (!user.trim()) {
            return 'не указано имя пользователя'
        }
        if (user.contains('"') || password.contains('"')) {
            return 'имя пользователя и пароль не должны содержать двойную кавычку: vanessa-runner передает их платформе в кавычках'
        }
        if ((user + password) =~ /[\r\n]/) {
            return 'имя пользователя и пароль не должны содержать перевод строки'
        }
        return null
    }
}
