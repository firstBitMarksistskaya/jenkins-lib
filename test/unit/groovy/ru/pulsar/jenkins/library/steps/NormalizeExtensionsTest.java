package ru.pulsar.jenkins.library.steps;

import groovy.lang.Closure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.utils.InfobaseExtensions;
import ru.pulsar.jenkins.library.utils.TestUtils;
import ru.pulsar.jenkins.library.utils.VRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class NormalizeExtensionsTest {

    // файл списка расширений платформа пишет в UTF-8 с BOM
    private static final String BOM = String.valueOf((char) 0xFEFF);

    // mods загружается при подготовке, YAXUnit и Tests - на стадии yaxunit, BDD - на стадии bdd
    private static final String EXTENSIONS = """
        [
          { "name": "mods" },
          { "name": "YAXUnit", "stages": ["yaxunit"] },
          { "name": "Tests", "stages": ["yaxunit", "smoke"] },
          { "name": "BDD", "stages": ["bdd"] }
        ]
        """;

    // в базе остались расширения прошлых сборок и расширение, установленное администратором
    private static final String INFOBASE_EXTENSIONS = BOM + "mods\r\nyaxunit\r\nTests\r\nBDD\r\nAdminExtension\r\n";

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();

    @BeforeEach
    void setUp() {
        when(steps.withCredentials(anyList(), any(Closure.class))).thenAnswer(invocation -> {
            Closure<?> body = invocation.getArgument(1);
            return body.call();
        });
        doReturn(INFOBASE_EXTENSIONS).when(steps).readFile(InfobaseExtensions.LIST_FILE, "UTF-8");
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void runSkipsLocalBase() {
        // when
        // локальная база создается на каждой сборке
        List<String> commands = run("", "", EXTENSIONS, "initInfoBase");

        // then
        assertThat(commands).isEmpty();
    }

    @Test
    void runSkipsBaseWithoutStageExtensions() {
        // when
        List<String> commands = run("/F/var/1c/test_base", "", "[{ \"name\": \"mods\" }]", "initInfoBase");

        // then
        assertThat(commands).isEmpty();
    }

    @Test
    void runDeletesAllStageExtensionsInPreparation() {
        // when
        List<String> commands = run("/F/var/1c/test_base", "", EXTENSIONS, "initInfoBase");

        // then
        // mods загружается при подготовке, а расширение администратора не из конфигурации, их не трогаем
        assertThat(commands).containsExactly(
            dumpCommand(),
            deleteCommand("YAXUnit"),
            deleteCommand("Tests"),
            deleteCommand("BDD")
        );
    }

    @Test
    void runKeepsExtensionsOfCurrentStage() {
        // when
        List<String> commands = run("/F/var/1c/test_base", "", EXTENSIONS, "smoke");

        // then
        // Tests нужно и стадии smoke, его не удаляем
        assertThat(commands).containsExactly(
            dumpCommand(),
            deleteCommand("YAXUnit"),
            deleteCommand("BDD")
        );
    }

    @Test
    void runDeletesOnlyExtensionsFoundInInfobase() {
        // given
        doReturn(BOM + "mods\r\nBDD\r\n").when(steps).readFile(InfobaseExtensions.LIST_FILE, "UTF-8");

        // when
        List<String> commands = run("/F/var/1c/test_base", "", EXTENSIONS, "yaxunit");

        // then
        // для расширения, которого нет в базе, конфигуратор завершился бы с ошибкой
        assertThat(commands).containsExactly(dumpCommand(), deleteCommand("BDD"));
    }

    @Test
    void runDeletesExtensionsFromServerBaseWithExclusiveAccess() {
        // when
        List<String> commands = run("/Ssrv1c\\\\test_base", "srv1c:1545", EXTENSIONS, "bdd");

        // then
        assertThat(commands).hasSize(5);
        assertThat(commands.get(0)).contains("/DumpDBCfgList").doesNotContain("--uccode");
        assertThat(commands.get(1)).startsWith("vrunner session kill --db \"test_base\"");
        assertThat(commands.get(2)).contains("/DeleteCfg -Extension YAXUnit").contains(" --uccode ");
        assertThat(commands.get(3)).contains("/DeleteCfg -Extension Tests").contains(" --uccode ");
        assertThat(commands.get(4)).startsWith("vrunner session unlock --db \"test_base\"");
    }

    private List<String> run(String basePath, String rasAddress, String extensions, String stageName) {
        String config = """
            {
              "basePath": "%s",
              "rasAddress": "%s",
              "initInfobase": {
                "extensions": %s
              }
            }
            """.formatted(basePath, rasAddress, extensions);
        JobConfiguration jobConfiguration = ConfigurationReader.create(config);
        List<String> commands = new ArrayList<>();

        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            vrunner.when(() -> VRunner.exec(anyString())).thenAnswer(invocation -> {
                commands.add(invocation.getArgument(0));
                return 0;
            });

            new NormalizeExtensions(jobConfiguration, stageName).run();
        }

        return commands;
    }

    private static String dumpCommand() {
        return "vrunner designer --additional \"/DumpDBCfgList -AllExtensions /Out " + InfobaseExtensions.LIST_FILE + "\""
            + " --ibconnection \"/F/var/1c/test_base\"";
    }

    private static String deleteCommand(String name) {
        return "vrunner designer --additional \"/DeleteCfg -Extension " + name + "\" --ibconnection \"/F/var/1c/test_base\"";
    }
}
