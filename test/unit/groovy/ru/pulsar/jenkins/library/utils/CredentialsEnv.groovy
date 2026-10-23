package ru.pulsar.jenkins.library.utils

/** Окружение сборки с переменными, которые привязывают учетные данные пользователя ИБ */
class CredentialsEnv extends EnvUtils {

    public String RUNNER_DBUSER
    public String RUNNER_DBPWD
    public String WORKSPACE_TMP
}
