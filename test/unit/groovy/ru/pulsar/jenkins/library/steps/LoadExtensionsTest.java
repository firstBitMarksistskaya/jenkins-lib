package ru.pulsar.jenkins.library.steps;

import groovy.lang.Closure;
import hudson.FilePath;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.InitInfoBaseOptions;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.utils.FileUtils;
import ru.pulsar.jenkins.library.utils.TestUtils;
import ru.pulsar.jenkins.library.utils.VRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class LoadExtensionsTest {

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext();
    }

    @Test
    void runYaxunit() throws IOException {

        try (MockedStatic<FileUtils> fu = Mockito.mockStatic(FileUtils.class)) {
            fu.when(() -> FileUtils.getFilePath(anyString()))
                    .thenReturn(new FilePath(new File("/")));

            // given
            // файл содержит 4 расширения для разных этапов
            String config = IOUtils.resourceToString(
                    "jobConfiguration.json",
                    StandardCharsets.UTF_8,
                    this.getClass().getClassLoader()
            );
            JobConfiguration jobConfiguration = ConfigurationReader.create(config);

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
    }

    @Test
    void runLoadsExtensionIntoLocalBase() {
        // when
        List<String> commands = runLoadCommands("");

        // then
        // в новую пустую базу файл настроек этапа инициализации не передается: пользователей в ней нет
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0))
                .contains("--ibconnection \"/F./build/ib\"")
                .doesNotContain("--settings");
    }

    @Test
    void runLoadsExtensionIntoExternalBase() {
        // when
        List<String> commands = runLoadCommands("/Ssrv1c\\\\test_base");

        // then
        // в существующую базу файл настроек этапа инициализации передается
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0))
                .contains("--ibconnection \"/Ssrv1c\\test_base\"")
                .endsWith(" --settings ./tools/vrunner.json");
    }

    private List<String> runLoadCommands(String basePath) {
        IStepExecutor steps = TestUtils.getMockedStepExecutor();
        when(steps.withEnv(anyList(), any(Closure.class))).thenAnswer(invocation -> {
            Closure<?> body = invocation.getArgument(1);
            return body.call();
        });
        when(steps.fileExists("./tools/vrunner.json")).thenReturn(true);
        TestUtils.setupMockedContext(steps);

        String config = """
            {
              "basePath": "%s",
              "initInfobase": {
                "extensions": [
                  {
                    "name": "ext",
                    "initMethod": "fromSource",
                    "path": "src/cfe/ext"
                  }
                ]
              }
            }
            """.formatted(basePath);
        JobConfiguration jobConfiguration = ConfigurationReader.create(config);
        List<String> commands = new ArrayList<>();

        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            vrunner.when(() -> VRunner.exec(anyString())).thenAnswer(invocation -> {
                commands.add(invocation.getArgument(0));
                return 0;
            });

            new LoadExtensions(jobConfiguration, "initInfoBase").run();
        }

        return commands;
    }
}
