package ru.pulsar.jenkins.library.utils

import ru.pulsar.jenkins.library.IStepExecutor

import static ru.pulsar.jenkins.library.configuration.Secrets.UNKNOWN_ID

/** Выбирает идентификатор учетных данных. Ошибки привязки и выполнения команды передаются вызывающему коду без повторного выбора. */
class CredentialSelection {

    static String resolve(IStepExecutor steps, String configuredId, String defaultId) {
        if (configuredId && configuredId != UNKNOWN_ID) {
            Logger.println("Credentials: explicit ID $configuredId")
            return configuredId
        }
        if (steps.credentialExists(defaultId)) {
            Logger.println("Credentials: standard ID $defaultId")
            return defaultId
        }
        Logger.println("Credentials: without credentials ($defaultId not available)")
        return null
    }
}
