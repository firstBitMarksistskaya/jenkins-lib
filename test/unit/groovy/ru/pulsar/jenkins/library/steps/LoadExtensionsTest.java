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
import ru.pulsar.jenkins.library.utils.InfobaseExtensions;
import ru.pulsar.jenkins.library.utils.InfobaseCredentialsStub;
import ru.pulsar.jenkins.library.utils.TestUtils;
import ru.pulsar.jenkins.library.utils.VRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoadExtensionsTest {

    // файл списка расширений платформа пишет в UTF-8 с BOM
    private static final String BOM = String.valueOf((char) 0xFEFF);

    private static final String EXTENSION = """
        {
          "name": "ext",
          "initMethod": "fromSource",
          "path": "src/cfe/ext"%s
        }
        """;

    // ext - расширение пайплайна, Smoke и BDD - служебные расширения стадий
    private static final String EXTENSIONS_WITH_STAGE_ONES = """
        { "name": "ext" },
        { "name": "Smoke", "stages": ["yaxunit"] },
        { "name": "BDD", "stages": ["bdd", "smoke"] }
        """;

    private static final String SERVER_BASE = "/Ssrv1c\\\\test_base";

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
        // по умолчанию в базе нет расширений
        infobaseExtensions(BOM);
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
        List<String> commands = run(createConfiguration("", "", EXTENSION.formatted("")), "initInfoBase");

        // then
        // в новую пустую базу файл настроек этапа инициализации не передается: пользователей в ней нет.
        // Расширений в новой базе нет, поэтому список расширений базы не читается
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0))
                .contains(" run --command ")
                .contains("--ibconnection \"/F./build/ib\"")
                .doesNotContain("--settings");
    }

    @Test
    void runInstallsMissingExtensionIntoExternalBase() {
        // when
        List<String> commands = run(createConfiguration(SERVER_BASE, "", EXTENSION.formatted("")), "initInfoBase");

        // then
        // расширения в базе еще нет: его ставит обработка подключения
        assertThat(commands).hasSize(2);
        assertThat(commands.get(0)).contains("/DumpDBCfgList -AllExtensions");
        assertThat(commands.get(1))
                .contains(" run --command ")
                .contains("--ibconnection \"/Ssrv1c\\test_base\"")
                .endsWith(" --settings ./tools/vrunner.json");
    }

    @Test
    void runUpdatesExistingExtensionOfExternalBase() {
        // given
        // имена расширений регистр не различают
        infobaseExtensions(BOM + "EXT\r\n");

        // when
        List<String> commands = run(createConfiguration(SERVER_BASE, "", EXTENSION.formatted("")), "initInfoBase");

        // then
        // расширение уже есть: обновляется конфигуратором, данные расширения остаются
        assertThat(commands).hasSize(2);
        assertThat(commands.get(1))
                .startsWith("vrunner loadext --file \"")
                .contains("ext.cfe\" --extension \"ext\" --updatedb --ibconnection \"/Ssrv1c\\test_base\"")
                .endsWith(" --settings ./tools/vrunner.json");
    }

    @Test
    void runDeletesLeftoverStageExtensionsInPreparation() {
        // given
        // Smoke остался от прерванной сборки, AdminExtension установлен администратором
        infobaseExtensions(BOM + "ext\r\nsmoke\r\nAdminExtension\r\n");

        // when
        List<String> commands = run(createConfiguration(SERVER_BASE, "", EXTENSIONS_WITH_STAGE_ONES), "initInfoBase");

        // then
        // BDD в базе нет, а расширение администратора не из конфигурации: их не трогаем
        assertThat(commands).hasSize(3);
        assertThat(commands.get(0)).contains("/DumpDBCfgList -AllExtensions");
        assertThat(commands.get(1)).contains("/DeleteCfg -Extension Smoke");
        assertThat(commands.get(2)).startsWith("vrunner loadext").contains("--extension \"ext\"");
    }

    @Test
    void runDeletesLeftoversWithoutPipelineExtensions() {
        // given
        infobaseExtensions(BOM + "Smoke\r\n");

        // when
        List<String> commands = run(
            createConfiguration(SERVER_BASE, "", "{ \"name\": \"Smoke\", \"stages\": [\"yaxunit\"] }"),
            "initInfoBase"
        );

        // then
        assertThat(commands).hasSize(2);
        assertThat(commands.get(1)).contains("/DeleteCfg -Extension Smoke");
    }

    @Test
    void runOnlyReadsInfobaseWhenNothingLeftInPreparation() {
        // when
        List<String> commands = run(
            createConfiguration(SERVER_BASE, "", "{ \"name\": \"Smoke\", \"stages\": [\"yaxunit\"] }"),
            "initInfoBase"
        );

        // then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0)).contains("/DumpDBCfgList -AllExtensions");
    }

    @Test
    void runInstallsStageExtensionWithoutReadingInfobase() {
        // given
        infobaseExtensions(BOM + "Smoke\r\n");

        // when
        List<String> commands = run(createConfiguration(SERVER_BASE, "", EXTENSIONS_WITH_STAGE_ONES), "yaxunit");

        // then
        // служебное расширение удалено после прошлой стадии или при подготовке, его ставит обработка подключения
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0)).contains(" run --command ").contains("Smoke.cfe");
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
        // given
        infobaseExtensions(BOM + "ext\r\nSmoke\r\n");

        // when
        List<String> commands = run(createConfiguration(SERVER_BASE, "srv1c:1545", EXTENSIONS_WITH_STAGE_ONES), "initInfoBase");

        // then
        // сеансы завершаются до первого обращения конфигуратора к базе: список расширений,
        // удаление и обновление идут под кодом разрешения
        assertThat(commands).hasSize(5);
        assertThat(commands.get(0)).startsWith("vrunner session kill --db \"test_base\"");
        assertThat(commands.get(1)).contains("/DumpDBCfgList").contains(" --uccode ");
        assertThat(commands.get(2)).contains("/DeleteCfg -Extension Smoke").contains(" --uccode ");
        assertThat(commands.get(3)).startsWith("vrunner loadext").contains(" --uccode ");
        assertThat(commands.get(4)).startsWith("vrunner session unlock --db \"test_base\"");
    }

    @Test
    void runReadsServerBaseUnderExclusiveAccessWhenNothingLeft() {
        // when
        List<String> commands = run(
            createConfiguration(SERVER_BASE, "srv1c:1545", "{ \"name\": \"Smoke\", \"stages\": [\"yaxunit\"] }"),
            "initInfoBase"
        );

        // then
        // удалять нечего, но список уже прочитан под запретом сеансов: запрет снимается
        assertThat(commands).hasSize(3);
        assertThat(commands.get(0)).startsWith("vrunner session kill");
        assertThat(commands.get(1)).contains("/DumpDBCfgList").contains(" --uccode ");
        assertThat(commands.get(2)).startsWith("vrunner session unlock");
    }

    @Test
    void runDoesNothingWithoutExtensionsForStage() {
        // when
        List<String> commands = run(createConfiguration(SERVER_BASE, "srv1c:1545", EXTENSION.formatted("")), "bdd");

        // then
        assertThat(commands).isEmpty();
    }

    @Test
    void runLoadsExtensionIntoNewLocalBaseWithoutInfobaseCredentials() {
        // given
        InfobaseCredentialsStub.bind(steps);

        // when
        List<String> commands = run(createConfiguration("", "", EXTENSION.formatted("")), "initInfoBase");

        // then
        verify(steps, never()).credentialExists(anyString());
        assertThat(commands).hasSize(1);
    }

    @Test
    void runLoadsStageExtensionUnderInfobaseCredentials() {
        // given
        InfobaseCredentialsStub.bind(steps);

        // when
        List<String> commands = run(createConfiguration("", "", EXTENSION.formatted(", \"stages\": [\"bdd\"]")), "bdd");

        // then
        verify(steps).usernamePassword(endsWith("_INFOBASE_USER"), eq("RUNNER_DBUSER"), eq("RUNNER_DBPWD"));
        assertThat(commands).hasSize(1);
    }

    private List<String> run(JobConfiguration jobConfiguration, String stageName) {
        List<String> commands = new ArrayList<>();

        try (MockedStatic<VRunner> vrunner = mockVRunner(commands)) {
            new LoadExtensions(jobConfiguration, stageName).run();
        }

        return commands;
    }

    private void infobaseExtensions(String dump) {
        doReturn(dump).when(steps).readFile(InfobaseExtensions.LIST_FILE, "UTF-8");
    }

    private static MockedStatic<VRunner> mockVRunner(List<String> commands) {
        MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class);
        vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
        vrunner.when(() -> VRunner.exec(anyString())).thenAnswer(invocation -> {
            commands.add(invocation.getArgument(0));
            return 0;
        });
        // список расширений базы читается с кодом возврата
        vrunner.when(() -> VRunner.exec(anyString(), anyBoolean())).thenAnswer(invocation -> {
            commands.add(invocation.getArgument(0));
            return 0;
        });
        return vrunner;
    }

    private static JobConfiguration createConfiguration(String basePath, String rasAddress, String extensions) {
        String config = """
            {
              "basePath": "%s",
              "rasAddress": "%s",
              "initInfobase": {
                "extensions": [%s]
              }
            }
            """.formatted(basePath, rasAddress, extensions);
        return ConfigurationReader.create(config);
    }
}
