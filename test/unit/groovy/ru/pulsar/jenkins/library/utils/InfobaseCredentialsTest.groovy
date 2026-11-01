package ru.pulsar.jenkins.library.utils

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.stubbing.Answer
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.ConfigurationReader
import ru.pulsar.jenkins.library.configuration.JobConfiguration

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.catchThrowable
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyList
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class InfobaseCredentialsTest {

    private static final String STANDARD_ID = 'org_repo_INFOBASE_USER'

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor()
    private final CredentialsEnv env = new CredentialsEnv()
    private final Object binding = new Object()
    private final List<List<String>> environments = []
    private final List<Object> bodyArguments = []

    @BeforeEach
    void setUp() {
        RepoUtils.computeRepoSlug('https://github.com/org/repo.git')
        env.RUNNER_DBUSER = 'Администратор'
        env.RUNNER_DBPWD = 'fixture-secret'
        when(steps.env()).thenReturn(env)
        when(steps.usernamePassword(anyString(), anyString(), anyString())).thenReturn(binding)
        when(steps.withCredentials(anyList(), any(Closure))).thenAnswer({ invocation ->
            (invocation.getArgument(1) as Closure).call()
        } as Answer)
        when(steps.withEnv(anyList(), any(Closure))).thenAnswer({ invocation ->
            environments << invocation.getArgument(0)
            (invocation.getArgument(1) as Closure).call()
        } as Answer)
        when(steps.error(anyString())).thenAnswer({ invocation ->
            throw new IllegalStateException(invocation.getArgument(0) as String)
        } as Answer)
        TestUtils.setupMockedContext(steps)
    }

    @AfterEach
    void tearDown() {
        RepoUtils.computeRepoSlug('')
    }

    @Test
    void standardCredentialsAreBoundToRunnerVariables() {
        // given
        when(steps.credentialExists(STANDARD_ID)).thenReturn(true)

        // when
        def result = run(configuration(''), true)

        // then
        assertThat(result).isEqualTo('done')
        assertThat(bodyArguments).containsExactly(true)
        verify(steps).usernamePassword(STANDARD_ID, 'RUNNER_DBUSER', 'RUNNER_DBPWD')
        verify(steps).withCredentials(eq([binding]), any(Closure))
        assertThat(environments).containsExactly([])
    }

    @Test
    void explicitIdIsBoundWithoutLookup() {
        // when
        run(configuration('"secrets": {"infobaseUser": "test-admin"}'), true)

        // then
        // явный ID не заменяется стандартным: его отсутствие или неверный тип - ошибка привязки
        verify(steps, never()).credentialExists(anyString())
        verify(steps).usernamePassword('test-admin', 'RUNNER_DBUSER', 'RUNNER_DBPWD')
        assertThat(bodyArguments).containsExactly(true)
    }

    @Test
    void settingsFilesAreUsedWithoutStandardCredentials() {
        // when
        run(configuration(''), true)

        // then
        verify(steps).credentialExists(STANDARD_ID)
        verify(steps, never()).withCredentials(anyList(), any(Closure))
        assertThat(bodyArguments).containsExactly(false)
    }

    @Test
    void baseWithoutUsersIsEnteredWithoutCredentials() {
        // given
        when(steps.credentialExists(STANDARD_ID)).thenReturn(true)

        // when
        run(configuration(''), false)

        // then
        verify(steps, never()).credentialExists(anyString())
        verify(steps, never()).withCredentials(anyList(), any(Closure))
        assertThat(bodyArguments).containsExactly(false)
    }

    @Test
    void emptyPasswordIsPassedExplicitly() {
        // given
        // переменную с пустым значением Jenkins не передает
        when(steps.credentialExists(STANDARD_ID)).thenReturn(true)
        env.RUNNER_DBPWD = null

        // when
        run(configuration(''), true)

        // then
        assertThat(environments).containsExactly(['RUNNER_DBPWD=""'])
        assertThat(bodyArguments).containsExactly(true)
    }

    @Test
    void quoteInPasswordFailsWithoutPrintingIt() {
        // given
        when(steps.credentialExists(STANDARD_ID)).thenReturn(true)
        env.RUNNER_DBPWD = 'fixture"secret'
        JobConfiguration config = configuration('')

        // when
        Throwable thrown = catchThrowable { run(config, true) }

        // then
        assertThat(thrown)
            .hasMessageContaining(STANDARD_ID)
            .hasMessageContaining('двойную кавычку')
            .hasMessageNotContaining('fixture')
        assertThat(bodyArguments).isEmpty()
    }

    @Test
    void emptyUserFails() {
        // given
        when(steps.credentialExists(STANDARD_ID)).thenReturn(true)
        env.RUNNER_DBUSER = ' '
        JobConfiguration config = configuration('')

        // when
        Throwable thrown = catchThrowable { run(config, true) }

        // then
        assertThat(thrown).hasMessageContaining('не указано имя пользователя')
        assertThat(bodyArguments).isEmpty()
    }

    @Test
    void baseHasUsersAfterTemplateAndInExternalBase() {
        JobConfiguration newBase = configuration('')
        JobConfiguration template = configuration('"initInfobase": {"templateDBPath": "template.dt"}')
        JobConfiguration external = configuration('"basePath": "/Ssrv1c\\\\test_base"')

        assertThat(InfobaseCredentials.preparedBaseHasUsers(newBase)).isFalse()
        assertThat(InfobaseCredentials.preparedBaseHasUsers(template)).isTrue()
        assertThat(InfobaseCredentials.preparedBaseHasUsers(external)).isTrue()

        // тестовые стадии работают с подготовленной базой
        assertThat(InfobaseCredentials.stageBaseHasUsers(newBase, 'initInfoBase')).isFalse()
        assertThat(InfobaseCredentials.stageBaseHasUsers(newBase, 'bdd')).isTrue()
        assertThat(InfobaseCredentials.stageBaseHasUsers(template, 'initInfoBase')).isTrue()
    }

    private def run(JobConfiguration config, boolean baseHasUsers) {
        return InfobaseCredentials.withCredentials(config, baseHasUsers) { credentialsBound ->
            bodyArguments << credentialsBound
            return 'done'
        }
    }

    private static JobConfiguration configuration(String properties) {
        return ConfigurationReader.create("{$properties}".toString())
    }
}
