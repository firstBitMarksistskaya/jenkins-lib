package ru.pulsar.jenkins.library.steps

import groovy.json.JsonSlurper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.ConfigurationReader
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.utils.CredentialsEnv
import ru.pulsar.jenkins.library.utils.InfobaseCredentialsStub
import ru.pulsar.jenkins.library.utils.TestUtils
import ru.pulsar.jenkins.library.utils.VRunner

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyBoolean
import static org.mockito.ArgumentMatchers.anyList
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.endsWith
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class BddTest {

    private static final String STATUS_FILE = 'build/out/bdd-exit-code.log'

    @TempDir
    Path workspace

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor()
    private final List<String> commands = []
    private final List<String> testClientParameters = []

    @BeforeEach
    void setUp() {
        when(steps.withEnv(anyList(), any(Closure))).thenAnswer(callBody(1))
        when(steps.withCoverage(any(), any(), any(), any(Closure))).thenAnswer(callBody(3))
        TestUtils.setupMockedContext(steps)
    }

    @Test
    void stepWithoutStatusFileGetsRunnerExitCode() {
        // when
        // VA не записала файл статуса: 1С не запустилась, например под неверным пользователем
        run(1)

        // then
        verify(steps).unstable(anyString())
        verify(steps, never()).error(anyString())
    }

    @Test
    void vanessaStepGetsTestClientPasswordUnderInfobaseCredentials() {
        // given
        CredentialsEnv env = InfobaseCredentialsStub.bind(steps)
        env.WORKSPACE = workspace.toString()
        env.WORKSPACE_TMP = workspace.resolve('ws@tmp').toString()
        write('tools/vrunner.json', '{ "vanessa": { "--vanessasettings": "./tools/VBParams.json" } }')
        write('tools/VBParams.json', '{}')

        // when
        run(0)

        // then
        assertThat(commands).hasSize(1)
        assertThat(commands[0])
            .startsWith('vrunner vanessa --settings ./tools/vrunner.json --ibconnection "/F./build/ib" --vanessasettings "')
            .contains(env.WORKSPACE_TMP)
        assertThat(testClientParameters).containsExactly('/P"fixture-secret"')
        verify(steps).usernamePassword(endsWith('_INFOBASE_USER'), eq('RUNNER_DBUSER'), eq('RUNNER_DBPWD'))
    }

    private void run(int exitStatus) {
        JobConfiguration config = ConfigurationReader.create('{ "stages": { "bdd": true } }')
        MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner)
        try {
            vrunner.when({ VRunner.getVRunnerPath() } as MockedStatic.Verification).thenReturn('vrunner')
            vrunner.when({ VRunner.exec(anyString(), anyBoolean()) } as MockedStatic.Verification).thenAnswer({ invocation ->
                String command = invocation.getArgument(0)
                commands << command
                def copy = command =~ / --vanessasettings "([^"]+)"$/
                if (copy.find()) {
                    Map settings = new JsonSlurper().parse(new File(copy.group(1)), 'UTF-8') as Map
                    testClientParameters << (settings['ДополнительныеПараметрыЗапускаКлиентаТестирования'] as String)
                }
                return exitStatus
            } as Answer)
            // файла статуса нет, используется переданный код
            vrunner.when({ VRunner.readExitStatusFromFile(eq(STATUS_FILE), any()) } as MockedStatic.Verification)
                .thenAnswer({ invocation -> invocation.getArgument(1) } as Answer)

            new Bdd(config).run()

            vrunner.verify({ VRunner.readExitStatusFromFile(STATUS_FILE, exitStatus) } as MockedStatic.Verification)
        } finally {
            vrunner.close()
        }
    }

    private void write(String path, String content) {
        Path file = workspace.resolve(path)
        Files.createDirectories(file.parent)
        Files.write(file, content.getBytes(StandardCharsets.UTF_8))
    }

    private static Answer callBody(int index) {
        return { invocation -> (invocation.getArgument(index) as Closure).call() } as Answer
    }
}
