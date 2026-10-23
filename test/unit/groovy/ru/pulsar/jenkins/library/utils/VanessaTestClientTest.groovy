package ru.pulsar.jenkins.library.utils

import groovy.json.JsonSlurper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.stubbing.Answer
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.ConfigurationReader
import ru.pulsar.jenkins.library.configuration.JobConfiguration

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.catchThrowable
import static org.assertj.core.api.Assertions.entry
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class VanessaTestClientTest {

    private static final String PASSWORD = 'fixture-secret'
    private static final String COMMAND = 'vrunner vanessa --settings ./tools/vrunner.json --ibconnection "/F./build/ib"'
    private static final Pattern COPY = ~/ --vanessasettings "([^"]+)"$/
    private static final String ADDITIONAL_PARAMETERS = 'ДополнительныеПараметрыЗапускаКлиентаТестирования'

    @TempDir
    Path workspace

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor()
    private final CredentialsEnv env = new CredentialsEnv()
    private final List<String> commands = []
    private final List<Map> copies = []

    @BeforeEach
    void setUp() {
        env.WORKSPACE = workspace.toString()
        env.WORKSPACE_TMP = workspace.resolve('ws@tmp').toString()
        env.RUNNER_DBUSER = 'Администратор'
        env.RUNNER_DBPWD = PASSWORD
        when(steps.env()).thenReturn(env)
        when(steps.error(anyString())).thenAnswer({ invocation ->
            throw new IllegalStateException(invocation.getArgument(0) as String)
        } as Answer)
        TestUtils.setupMockedContext(steps)
    }

    @Test
    void passwordIsAddedToCopyOfVanessaSettings() {
        // given
        write('tools/vrunner.json', '''
            {
              "default": { "--db-user": "Администратор", "--db-pwd": "from-file" },
              "vanessa": { "--vanessasettings": "./tools/VBParams.json" }
            }
            ''')
        String original = '''
            {
              "КаталогФич": "$workspaceRoot/features",
              "ДополнительныеПараметрыЗапускаКлиентаТестирования": "/DisableStartupMessages"
            }
            '''
        write('tools/VBParams.json', original)

        // when
        run(configuration(''), COMMAND, true)

        // then
        assertThat(commands).hasSize(1)
        assertThat(commands[0]).startsWith("$COMMAND --vanessasettings \"$env.WORKSPACE_TMP".toString())
        assertThat(copies[0])
            .contains(entry('КаталогФич', '$workspaceRoot/features'))
            .contains(entry(ADDITIONAL_PARAMETERS, "/DisableStartupMessages /P\"$PASSWORD\"".toString()))
            .contains(entry('МаскироватьПарольВЛоге', true))
        // копия удаляется после команды, исходные настройки не меняются
        assertThat(new File(copyPath(commands[0]))).doesNotExist()
        assertThat(read('tools/VBParams.json')).isEqualTo(original)
        // VA пишет строку запуска клиента тестирования в журнал регистрации локальной базы
        verify(steps).deleteDir('build/ib/1Cv8Log')
    }

    @Test
    void settingsFromCommandAreReplacedAndEnglishNamesKept() {
        // given
        write('va.json', '{ "additionalparametersforlaunchingthetestingclient": "", "maskpwdinlog": false }')
        String command = "vrunner vanessa --vanessasettings \"${workspace.resolve('va.json')}\" --ibconnection \"/F./build/ib\""

        // when
        run(configuration(''), command, true)

        // then
        assertThat(commands[0]).containsOnlyOnce('--vanessasettings').doesNotContain('va.json')
        assertThat(copies[0]).containsOnly(
            entry('additionalparametersforlaunchingthetestingclient', "/P\"$PASSWORD\"".toString()),
            entry('maskpwdinlog', true)
        )
    }

    @Test
    void defaultRunnerSectionAndWorkspaceRootAreResolved() {
        // given
        write('tools/vrunner.json', '{ "default": { "--vanessasettings": "$workspaceRoot/tools/VBParams.json" } }')
        write('tools/VBParams.json', '{}')

        // when
        run(configuration(''), COMMAND, true)

        // then
        assertThat(copies[0]).contains(entry(ADDITIONAL_PARAMETERS, "/P\"$PASSWORD\"".toString()))
    }

    @Test
    void eventLogOfExternalBaseIsKept() {
        // given
        writeDefaultSettings()

        // when
        run(configuration('"basePath": "/Ssrv1c\\\\test_base"'), COMMAND, true)

        // then
        assertThat(copies).hasSize(1)
        verify(steps, never()).deleteDir(anyString())
    }

    @Test
    void passwordInVanessaSettingsFailsWithoutPrintingSecrets() {
        // given
        write('tools/vrunner.json', '{ "vanessa": { "--vanessasettings": "./tools/VBParams.json" } }')
        write('tools/VBParams.json', '''
            { "ДанныеКлиентовТестирования": [ { "Имя": "Этот клиент", "ДопПараметры": "/N\\"Администратор\\" /P\\"from-file\\"" } ] }
            ''')
        JobConfiguration config = configuration('')

        // when
        Throwable thrown = catchThrowable { run(config, COMMAND, true) }

        // then
        assertThat(thrown).hasMessageContaining('(/P)').hasMessageNotContaining(PASSWORD).hasMessageNotContaining('from-file')
        assertThat(commands).isEmpty()
        assertThat(workspace.resolve('ws@tmp/infobase-credentials')).doesNotExist()
    }

    @Test
    void proxyKeyIsNotPassword() {
        // given
        write('tools/vrunner.json', '{ "vanessa": { "--vanessasettings": "./tools/VBParams.json" } }')
        write('tools/VBParams.json', '{ "ДополнительныеПараметрыЗапускаКлиентаТестирования": "/Proxy -PSrv proxy" }')

        // when
        run(configuration(''), COMMAND, true)

        // then
        assertThat(copies[0]).contains(entry(ADDITIONAL_PARAMETERS, "/Proxy -PSrv proxy /P\"$PASSWORD\"".toString()))
    }

    @Test
    void shellCharactersInPasswordFailOnLinux() {
        // given
        writeDefaultSettings()
        when(steps.isUnix()).thenReturn(true)
        env.RUNNER_DBPWD = 'fixture$secret'
        JobConfiguration config = configuration('')

        // when
        Throwable thrown = catchThrowable { run(config, COMMAND, true) }

        // then
        assertThat(thrown).hasMessageContaining('командную оболочку').hasMessageNotContaining('fixture')
        assertThat(commands).isEmpty()
    }

    @Test
    void commandIsUnchangedWithoutPasswordOrVanessa() {
        // given
        writeDefaultSettings()
        JobConfiguration config = configuration('')
        String xunit = 'vrunner xunit --settings ./tools/vrunner.json'

        // when
        run(config, COMMAND, false)
        run(config, xunit, true)
        env.RUNNER_DBPWD = '""'
        run(config, COMMAND, true)

        // then
        // без пароля VA сама запускает клиент тестирования с пользователем менеджера
        assertThat(commands).containsExactly(COMMAND, xunit, COMMAND)
        assertThat(workspace.resolve('ws@tmp')).doesNotExist()
        verify(steps, never()).deleteDir(anyString())
    }

    @Test
    void commandIsUnchangedWithoutVanessaSettings() {
        // when
        run(configuration(''), COMMAND, true)

        // then
        assertThat(commands).containsExactly(COMMAND)
        assertThat(copies).isEmpty()
    }

    private void run(JobConfiguration config, String command, boolean credentialsBound) {
        VanessaTestClient.withPassword(config, command, credentialsBound) { String effectiveCommand ->
            commands << effectiveCommand
            if (COPY.matcher(effectiveCommand).find()) {
                copies << (new JsonSlurper().parse(new File(copyPath(effectiveCommand)), 'UTF-8') as Map)
            }
            return 0
        }
    }

    private void writeDefaultSettings() {
        write('tools/vrunner.json', '{ "vanessa": { "--vanessasettings": "./tools/VBParams.json" } }')
        write('tools/VBParams.json', '{}')
    }

    private static String copyPath(String command) {
        Matcher matcher = COPY.matcher(command)
        assertThat(matcher.find()).isTrue()
        return matcher.group(1)
    }

    private void write(String path, String content) {
        Path file = workspace.resolve(path)
        Files.createDirectories(file.parent)
        Files.write(file, content.getBytes(StandardCharsets.UTF_8))
    }

    private String read(String path) {
        return new String(Files.readAllBytes(workspace.resolve(path)), StandardCharsets.UTF_8)
    }

    private static JobConfiguration configuration(String properties) {
        return ConfigurationReader.create("{$properties}".toString())
    }
}
