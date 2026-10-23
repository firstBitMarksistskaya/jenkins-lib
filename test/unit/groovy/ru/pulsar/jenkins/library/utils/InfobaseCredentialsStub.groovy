package ru.pulsar.jenkins.library.utils

import org.mockito.stubbing.Answer
import ru.pulsar.jenkins.library.IStepExecutor

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyList
import static org.mockito.ArgumentMatchers.endsWith
import static org.mockito.Mockito.when

/** Стандартные учетные данные пользователя ИБ найдены, привязка и окружение выполняют тело команд */
class InfobaseCredentialsStub {

    static CredentialsEnv bind(IStepExecutor steps) {
        CredentialsEnv env = new CredentialsEnv()
        env.RUNNER_DBUSER = 'test-user'
        env.RUNNER_DBPWD = 'fixture-secret'
        when(steps.env()).thenReturn(env)
        when(steps.credentialExists(endsWith(InfobaseCredentials.STANDARD_ID_SUFFIX))).thenReturn(true)
        when(steps.withCredentials(anyList(), any(Closure))).thenAnswer({ invocation ->
            (invocation.getArgument(1) as Closure).call()
        } as Answer)
        when(steps.withEnv(anyList(), any(Closure))).thenAnswer({ invocation ->
            (invocation.getArgument(1) as Closure).call()
        } as Answer)
        return env
    }
}
