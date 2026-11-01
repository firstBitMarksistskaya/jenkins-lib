package ru.pulsar.jenkins.library.steps;

import groovy.lang.Closure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.utils.TestUtils;
import ru.pulsar.jenkins.library.utils.VRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class InitFromStorageTest {

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();

    @BeforeEach
    void setUp() {
        when(steps.fileExists("./tools/vrunner.json")).thenReturn(true);
        when(steps.withCredentials(anyList(), any(Closure.class))).thenAnswer(invocation -> {
            Closure<?> body = invocation.getArgument(1);
            return body.call();
        });
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void runPassesInitSettingsForExistingBase() {
        // when
        List<String> commands = runCommands("/F/var/1c/test_base", "");

        // then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0))
            .startsWith("vrunner update-dev --storage")
            .contains("--ibconnection \"/F/var/1c/test_base\"")
            .endsWith(" --settings ./tools/vrunner.json");
    }

    @Test
    void runLoadsConfigurationIntoServerBaseWithExclusiveAccess() {
        // when
        List<String> commands = runCommands("/Ssrv1c\\\\test_base", "");

        // then
        assertThat(commands).hasSize(3);
        assertThat(commands.get(0)).startsWith("vrunner session kill --db \"test_base\"");
        assertThat(commands.get(1))
            .startsWith("vrunner update-dev --storage")
            .contains("--ibconnection \"/Ssrv1c\\test_base\" --settings ./tools/vrunner.json --uccode ");
        assertThat(commands.get(2)).startsWith("vrunner session unlock --db \"test_base\"");
    }

    @Test
    void runDoesNotPassInitSettingsForNewLocalBase() {
        // when
        List<String> commands = runCommands("", "");

        // then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0))
            .contains("--ibconnection \"/F./build/ib\"")
            .doesNotContain("--settings");
    }

    private List<String> runCommands(String basePath, String templateDBPath) {
        String config = """
            {
              "basePath": "%s",
              "rasAddress": "srv1c:1545",
              "initInfobase": {
                "initMethod": "fromStorage",
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

            new InitFromStorage(jobConfiguration).run();
        }

        return commands;
    }
}
