package ru.pulsar.jenkins.library.steps;

import groovy.lang.Closure;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.InitInfoBaseOptions;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.utils.TestUtils;
import ru.pulsar.jenkins.library.utils.VRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoadExtensionsTest {

    private static final String EXTENSION = """
        {
          "name": "ext",
          "initMethod": "fromSource",
          "path": "src/cfe/ext"%s
        }
        """;

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();

    @BeforeEach
    void setUp() {
        when(steps.withEnv(anyList(), any(Closure.class))).thenAnswer(invocation -> {
            Closure<?> body = invocation.getArgument(1);
            return body.call();
        });
        when(steps.withCredentials(anyList(), any(Closure.class))).thenAnswer(invocation -> {
            Closure<?> body = invocation.getArgument(1);
            return body.call();
        });
        when(steps.fileExists("./tools/vrunner.json")).thenReturn(true);
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void runYaxunit() throws IOException {
        // given
        // файл содержит 4 расширения для разных этапов
        String config = IOUtils.resourceToString(
                "jobConfiguration.json",
                StandardCharsets.UTF_8,
                this.getClass().getClassLoader()
        );
        JobConfiguration jobConfiguration = ConfigurationReader.create(config);

        try (MockedStatic<VRunner> vrunner = mockVRunner(new ArrayList<>())) {
            // when
            LoadExtensions loadExtensionsInit = new LoadExtensions(jobConfiguration, "initInfoBase");
            loadExtensionsInit.run();

            // then
            InitInfoBaseOptions.Extension[] extensions = loadExtensionsInit.getExtensionsFiltered();
            assertThat(extensions.length).isEqualTo(2);
            assertThat(extensions[0].getName()).isEqualTo("mods");
            assertThat(extensions[1].getName()).isEqualTo("mods2");

            // when
            LoadExtensions loadExtensionsWithStage = new LoadExtensions(jobConfiguration, "yaxunit");
            loadExtensionsWithStage.run();

            // then
            extensions = loadExtensionsWithStage.getExtensionsFiltered();
            assertThat(extensions.length).isEqualTo(1);
            assertThat(extensions[0].getName()).isEqualTo("YAXUnit");
        }
        verify(steps, never()).error(anyString());
    }

    @Test
    void runLoadsExtensionIntoLocalBase() {
        // when
        List<String> commands = run(createConfiguration("", ""), "initInfoBase");

        // then
        // в новую пустую базу файл настроек этапа инициализации не передается: пользователей в ней нет.
        // Список расширений базы после подключения не читается, как и раньше
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0))
                .contains("--ibconnection \"/F./build/ib\"")
                .doesNotContain("--settings");
    }

    @Test
    void runLoadsExtensionIntoExternalBase() {
        // when
        List<String> commands = run(createConfiguration("/Ssrv1c\\\\test_base", ""), "initInfoBase");

        // then
        // в существующую базу файл настроек этапа инициализации передается
        assertThat(commands.get(0))
                .contains("--ibconnection \"/Ssrv1c\\test_base\"")
                .endsWith(" --settings ./tools/vrunner.json");
    }

    @Test
    void runUsesSmokeTestSettingsForSmokeStage() {
        // given
        when(steps.fileExists("./tools/vrunner-smoke.json")).thenReturn(true);
        String config = """
            {
              "initInfobase": {
                "extensions": [%s]
              },
              "smoke": {
                "vrunnerSettings": "./tools/vrunner-smoke.json"
              }
            }
            """.formatted(EXTENSION.formatted(", \"stages\": [\"smoke\"]"));

        // when
        List<String> commands = run(ConfigurationReader.create(config), "smoke");

        // then
        assertThat(commands.get(0)).endsWith(" --settings ./tools/vrunner-smoke.json");
    }

    @Test
    void runLoadsExtensionsIntoServerBaseWithExclusiveAccess() {
        // when
        List<String> commands = run(createConfiguration("/Ssrv1c\\\\test_base", "srv1c:1545"), "initInfoBase");

        // then
        // подключение идет под кодом разрешения
        assertThat(commands).hasSize(3);
        assertThat(commands.get(0)).startsWith("vrunner session kill --db \"test_base\"");
        assertThat(commands.get(1)).contains(" run --command ").contains(" --uccode ");
        assertThat(commands.get(2)).startsWith("vrunner session unlock --db \"test_base\"");
    }

    @Test
    void runDoesNothingWithoutExtensionsForStage() {
        // when
        List<String> commands = run(createConfiguration("/Ssrv1c\\\\test_base", "srv1c:1545"), "bdd");

        // then
        assertThat(commands).isEmpty();
    }

    private List<String> run(JobConfiguration jobConfiguration, String stageName) {
        List<String> commands = new ArrayList<>();

        try (MockedStatic<VRunner> vrunner = mockVRunner(commands)) {
            new LoadExtensions(jobConfiguration, stageName).run();
        }

        return commands;
    }

    private static MockedStatic<VRunner> mockVRunner(List<String> commands) {
        MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class);
        vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
        vrunner.when(() -> VRunner.exec(anyString())).thenAnswer(invocation -> {
            commands.add(invocation.getArgument(0));
            return 0;
        });
        return vrunner;
    }

    private static JobConfiguration createConfiguration(String basePath, String rasAddress) {
        String config = """
            {
              "basePath": "%s",
              "rasAddress": "%s",
              "initInfobase": {
                "extensions": [%s]
              }
            }
            """.formatted(basePath, rasAddress, EXTENSION.formatted(""));
        return ConfigurationReader.create(config);
    }
}
