package ru.pulsar.jenkins.library.steps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.utils.InfobaseCredentialsStub;
import ru.pulsar.jenkins.library.utils.TestUtils;
import ru.pulsar.jenkins.library.utils.VRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitFromFilesTest {

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();

    @BeforeEach
    void setUp() {
        when(steps.fileExists("./tools/vrunner.json")).thenReturn(true);
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void runPassesInitSettingsForExistingBase() {
        // when
        List<String> commands = runCommands("/F/var/1c/test_base", "");

        // then
        assertThat(commands).containsExactly(
            "vrunner update-dev --src src/cf --ibconnection \"/F/var/1c/test_base\" --settings ./tools/vrunner.json"
        );
    }

    @Test
    void runLoadsConfigurationIntoServerBaseWithExclusiveAccess() {
        // when
        List<String> commands = runCommands("/Ssrv1c\\\\test_base", "");

        // then
        assertThat(commands).hasSize(3);
        assertThat(commands.get(0)).startsWith("vrunner session kill --db \"test_base\"");
        assertThat(commands.get(1)).startsWith(
            "vrunner update-dev --src src/cf --ibconnection \"/Ssrv1c\\test_base\" --settings ./tools/vrunner.json --uccode "
        );
        assertThat(commands.get(2)).startsWith("vrunner session unlock --db \"test_base\"");
    }

    @Test
    void runPassesInitSettingsForTemplate() {
        // when
        List<String> commands = runCommands("", "template.dt");

        // then
        assertThat(commands).containsExactly(
            "vrunner update-dev --src src/cf --ibconnection \"/F./build/ib\" --settings ./tools/vrunner.json"
        );
    }

    @Test
    void runDoesNotPassInitSettingsForNewLocalBase() {
        // when
        List<String> commands = runCommands("", "");

        // then
        assertThat(commands).containsExactly("vrunner update-dev --src src/cf --ibconnection \"/F./build/ib\"");
    }

    @Test
    void runLoadsConfigurationIntoTemplateBaseUnderInfobaseCredentials() {
        // given
        InfobaseCredentialsStub.bind(steps);

        // when
        List<String> commands = runCommands("", "template.dt");

        // then
        verify(steps).usernamePassword(endsWith("_INFOBASE_USER"), eq("RUNNER_DBUSER"), eq("RUNNER_DBPWD"));
        assertThat(commands).hasSize(1);
    }

    @Test
    void runLoadsConfigurationIntoNewLocalBaseWithoutInfobaseCredentials() {
        // given
        InfobaseCredentialsStub.bind(steps);

        // when
        List<String> commands = runCommands("", "");

        // then
        verify(steps, never()).credentialExists(anyString());
        assertThat(commands).containsExactly("vrunner update-dev --src src/cf --ibconnection \"/F./build/ib\"");
    }

    private List<String> runCommands(String basePath, String templateDBPath) {
        String config = """
            {
              "basePath": "%s",
              "rasAddress": "srv1c:1545",
              "initInfobase": {
                "initMethod": "fromSource",
                "templateDBPath": "%s"
              }
            }
            """.formatted(basePath, templateDBPath);
        JobConfiguration jobConfiguration = ConfigurationReader.create(config);
        List<String> commands = new ArrayList<>();

        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            vrunner.when(() -> VRunner.exec(anyString())).thenAnswer(invocation -> {
                commands.add(invocation.getArgument(0));
                return 0;
            });

            new InitFromFiles(jobConfiguration).run();
        }

        return commands;
    }
}
