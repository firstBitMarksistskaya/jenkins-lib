package ru.pulsar.jenkins.library.steps

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.stubbing.Answer
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.utils.CredentialsEnv
import ru.pulsar.jenkins.library.utils.TestUtils

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.catchThrowable
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.when

/** Тест-клиент дымовых тестов под учетными данными ИБ из Jenkins */
class SmokeTestClientTest {

    @TempDir
    Path workspace

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor()
    private final CredentialsEnv env = new CredentialsEnv()

    @BeforeEach
    void setUp() {
        env.RUNNER_DBUSER = 'test-user'
        env.RUNNER_DBPWD = 'fixture-secret'
        when(steps.env()).thenReturn(env)
        when(steps.error(anyString())).thenAnswer({ invocation ->
            throw new IllegalStateException(invocation.getArgument(0) as String)
        } as Answer)
        TestUtils.setupMockedContext(steps)
    }

    @Test
    void testClientWithUserAndPasswordFails() {
        // given
        String settings = write('{ "xunit": { "--testclient": "test-user:from-file:1538" } }')

        // when
        Throwable thrown = catchThrowable { SmokeTest.checkTestClient(steps, settings) }

        // then
        // иначе тест-клиент вошел бы в базу с паролем из файла настроек
        assertThat(thrown).hasMessageContaining('::').hasMessageNotContaining('from-file')
    }

    @Test
    void testClientFromDefaultSectionIsChecked() {
        // given
        String settings = write('{ "default": { "--testclient": "test-user::1538" } }')

        // when
        Throwable thrown = catchThrowable { SmokeTest.checkTestClient(steps, settings) }

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException)
    }

    @Test
    void testClientWithManagerCredentialsIsStarted() {
        // given
        // блок команды важнее блока default, как в vanessa-runner
        String settings = write('{ "default": { "--testclient": "test-user:from-file:1" }, "xunit": { "--testclient": "::1538" } }')

        // then
        assertThat(SmokeTest.checkTestClient(steps, settings)).isTrue()
    }

    @Test
    void smokeWithoutTestClientIsNotChecked() {
        assertThat(SmokeTest.checkTestClient(steps, workspace.resolve('absent.json').toString())).isFalse()
        assertThat(SmokeTest.checkTestClient(steps, write('{ "xunit": { "testsPath": "tests" } }'))).isFalse()
    }

    @Test
    void separatorsOfAddParametersInPasswordFail() {
        // given
        String settings = write('{ "xunit": { "--testclient": "::1538" } }')
        env.RUNNER_DBPWD = 'fixture:secret'

        // when
        Throwable thrown = catchThrowable { SmokeTest.checkTestClient(steps, settings) }

        // then
        // ADD делит параметры тест-клиента пользователь:пароль:порт по двоеточию
        assertThat(thrown).hasMessageContaining('пользователь:пароль:порт').hasMessageNotContaining('fixture')
    }

    @Test
    void shellCharactersInPasswordFailOnLinux() {
        // given
        String settings = write('{ "xunit": { "--testclient": "::1538" } }')
        when(steps.isUnix()).thenReturn(true)
        env.RUNNER_DBPWD = 'fixture`secret'

        // when
        Throwable thrown = catchThrowable { SmokeTest.checkTestClient(steps, settings) }

        // then
        assertThat(thrown).hasMessageContaining('командную оболочку').hasMessageNotContaining('fixture')
    }

    private String write(String content) {
        Path file = Files.createTempFile(workspace, 'vrunner', '.json')
        Files.write(file, content.getBytes(StandardCharsets.UTF_8))
        return file.toString()
    }
}
