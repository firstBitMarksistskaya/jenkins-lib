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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class RemoveStageExtensionsTest {

    // файл списка расширений платформа пишет в UTF-8 с BOM
    private static final String BOM = String.valueOf((char) 0xFEFF);

    // mods - расширение пайплайна, остальные - служебные расширения стадий
    private static final String EXTENSIONS = """
        [
          { "name": "mods" },
          { "name": "YAXUnit", "stages": ["yaxunit"] },
          { "name": "Tests", "stages": ["yaxunit", "smoke"] },
          { "name": "BDD", "stages": ["bdd"] },
          { "name": "Init", "stages": ["initInfoBase", "yaxunit"] }
        ]
        """;

    // в базе все расширения из конфигурации и расширение, установленное администратором
    private static final String INFOBASE_EXTENSIONS = BOM + "mods\r\nyaxunit\r\nTests\r\nBDD\r\nInit\r\nAdminExtension\r\n";

    private static final String FILE_BASE = "/F/var/1c/test_base";

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
        // тестовая стадия работает со своей копией локальной базы из архива
        List<String> commands = run("", "", EXTENSIONS, "yaxunit");

        // then
        assertThat(commands).isEmpty();
    }

    @Test
    void runSkipsStageWithoutOwnExtensions() {
        // when
        List<String> commands = run(FILE_BASE, "", EXTENSIONS, "syntaxCheck");

        // then
        // конфигуратор не запускается
        assertThat(commands).isEmpty();
    }

    @Test
    void runDeletesOnlyOwnStageExtensions() {
        // when
        List<String> commands = run(FILE_BASE, "", EXTENSIONS, "yaxunit");

        // then
        // расширения других стадий, расширения пайплайна, в том числе Init с initInfoBase,
        // и расширение администратора не трогаем
        assertThat(commands).containsExactly(
            dumpCommand(),
            deleteCommand("YAXUnit"),
            deleteCommand("Tests")
        );
    }

    @Test
    void runDeletesExtensionOfSeveralStagesAfterEachOfThem() {
        // when
        List<String> commands = run(FILE_BASE, "", EXTENSIONS, "smoke");

        // then
        assertThat(commands).containsExactly(dumpCommand(), deleteCommand("Tests"));
    }

    @Test
    void runDeletesNothingWhenExtensionWasNotLoaded() {
        // given
        // стадия упала до подключения своего расширения
        doReturn(BOM + "mods\r\n").when(steps).readFile(InfobaseExtensions.LIST_FILE, "UTF-8");

        // when
        List<String> commands = run(FILE_BASE, "", EXTENSIONS, "bdd");

        // then
        // для расширения, которого нет в базе, конфигуратор завершился бы с ошибкой
        assertThat(commands).containsExactly(dumpCommand());
    }

    @Test
    void runDeletesExtensionsFromServerBaseWithExclusiveAccess() {
        // when
        List<String> commands = run("/Ssrv1c\\\\test_base", "srv1c:1545", EXTENSIONS, "bdd");

        // then
        // сеансы завершаются до первого обращения конфигуратора к базе
        assertThat(commands).hasSize(4);
        assertThat(commands.get(0)).startsWith("vrunner session kill --db \"test_base\"");
        assertThat(commands.get(1)).contains("/DumpDBCfgList").contains(" --uccode ");
        assertThat(commands.get(2)).contains("/DeleteCfg -Extension BDD").contains(" --uccode ");
        assertThat(commands.get(3)).startsWith("vrunner session unlock --db \"test_base\"");
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
            // список расширений базы читается с кодом возврата
            vrunner.when(() -> VRunner.exec(anyString(), anyBoolean())).thenAnswer(invocation -> {
                commands.add(invocation.getArgument(0));
                return 0;
            });

            new RemoveStageExtensions(jobConfiguration, stageName).run();
        }

        return commands;
    }

    private static String dumpCommand() {
        return "vrunner designer --additional \"/DumpDBCfgList -AllExtensions /Out " + InfobaseExtensions.LIST_FILE + "\""
            + " --ibconnection \"" + FILE_BASE + "\"";
    }

    private static String deleteCommand(String name) {
        return "vrunner designer --additional \"/DeleteCfg -Extension " + name + "\" --ibconnection \"" + FILE_BASE + "\"";
    }
}
