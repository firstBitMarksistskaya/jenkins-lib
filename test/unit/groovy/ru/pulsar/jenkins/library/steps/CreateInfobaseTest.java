package ru.pulsar.jenkins.library.steps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.utils.FileUtils;
import ru.pulsar.jenkins.library.utils.InfobaseCredentialsStub;
import ru.pulsar.jenkins.library.utils.TestUtils;
import ru.pulsar.jenkins.library.utils.VRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateInfobaseTest {

    private static final String SERVER_BASE = "/Ssrv1c\\test_base";
    private static final String FILE_BASE = "/F/var/1c/test_base";

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();
    private final List<String> commands = new ArrayList<>();
    private final List<String> loadedFiles = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void runCreatesLocalBaseWithoutTemplate() {
        // when
        run(createConfiguration("", ""));

        // then
        verify(steps).installLocalDependencies();
        assertThat(commands).containsExactly("vrunner init-dev  --ibconnection \"/F./build/ib\"");
    }

    @Test
    void runUsesExistingBaseWithoutTemplate() {
        // when
        run(createConfiguration(SERVER_BASE, ""));

        // then
        // локальные зависимости нужны дальнейшим командам подготовки
        verify(steps).installLocalDependencies();
        assertThat(commands).isEmpty();
        assertThat(loadedFiles).isEmpty();
    }

    @Test
    void runCreatesLocalBaseAndRestoresDtTemplate() {
        // when
        run(createConfiguration("", "template.dt"));

        // then
        assertThat(loadedFiles).containsExactly("template.dt -> ws/build/tmp/dump.dt");
        assertThat(commands).containsExactly(
            "vrunner init-dev  --ibconnection \"/F./build/ib\"",
            "vrunner restore --ibconnection \"/F./build/ib\" build/tmp/dump.dt"
        );
    }

    @Test
    void runRestoresDtTemplateIntoExternalBaseWithoutCreation() {
        // given
        when(steps.fileExists("./tools/vrunner.json")).thenReturn(true);

        // when
        run(createConfiguration(FILE_BASE, "template.dt"));

        // then
        assertThat(commands).containsExactly(
            "vrunner restore --ibconnection \"/F/var/1c/test_base\" build/tmp/dump.dt --settings ./tools/vrunner.json"
        );
    }

    @Test
    void runRestoresDtTemplateIntoServerBaseWithExclusiveAccess() {
        // given
        when(steps.fileExists("./tools/vrunner.json")).thenReturn(true);

        // when
        run(createConfiguration(SERVER_BASE, "template.dt"));

        // then
        assertThat(commands).hasSize(3);
        assertThat(commands.get(0)).startsWith(
            "vrunner session kill --db \"test_base\" --ras srv1c:1545 --settings ./tools/vrunner.json --uccode "
        );
        assertThat(commands.get(1)).startsWith(
            "vrunner restore --ibconnection \"/Ssrv1c\\test_base\" build/tmp/dump.dt --settings ./tools/vrunner.json --uccode "
        );
        assertThat(commands.get(2)).isEqualTo(
            "vrunner session unlock --db \"test_base\" --ras srv1c:1545 --settings ./tools/vrunner.json"
        );
    }

    @Test
    void runCopies1CDTemplateIntoLocalBase() {
        // when
        run(createConfiguration("", "template.1CD"));

        // then
        assertThat(loadedFiles).containsExactly("template.1CD -> ws/build/ib/1Cv8.1CD");
        assertThat(commands).isEmpty();
    }

    @Test
    void runCopies1CDTemplateIntoExternalFileBaseDir() {
        // when
        run(createConfiguration(FILE_BASE, "template.1CD"));

        // then
        assertThat(loadedFiles).containsExactly("template.1CD -> /var/1c/test_base/1Cv8.1CD");
        assertThat(commands).isEmpty();
    }

    @Test
    void runResolvesRelativeExternalFileBaseDirFromWorkspace() {
        // when
        run(createConfiguration("/F./bases/test", "template.1CD"));

        // then
        assertThat(loadedFiles).containsExactly("template.1CD -> ws/./bases/test/1Cv8.1CD");
    }

    @Test
    void runFailsOn1CDTemplateForServerBase() {
        // when
        run(createConfiguration(SERVER_BASE, "template.1CD"));

        // then
        verify(steps).error(contains(".dt"));
        assertThat(loadedFiles).isEmpty();
        assertThat(commands).isEmpty();
    }

    @Test
    void runRestoresDtIntoNewLocalBaseWithoutInfobaseCredentials() {
        // given
        InfobaseCredentialsStub.bind(steps);

        // when
        run(createConfiguration("", "template.dt"));

        // then
        // в новой пустой базе пользователей нет, учетные данные действуют после загрузки шаблона
        verify(steps, never()).credentialExists(anyString());
        assertThat(commands).containsExactly(
            "vrunner init-dev  --ibconnection \"/F./build/ib\"",
            "vrunner restore --ibconnection \"/F./build/ib\" build/tmp/dump.dt"
        );
    }

    @Test
    void runRestoresDtIntoExternalBaseUnderInfobaseCredentials() {
        // given
        InfobaseCredentialsStub.bind(steps);

        // when
        run(createConfiguration(FILE_BASE, "template.dt"));

        // then
        verify(steps).usernamePassword(endsWith("_INFOBASE_USER"), eq("RUNNER_DBUSER"), eq("RUNNER_DBPWD"));
        assertThat(commands).containsExactly(
            "vrunner restore --ibconnection \"/F/var/1c/test_base\" build/tmp/dump.dt"
        );
    }

    private void run(JobConfiguration jobConfiguration) {
        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class);
             MockedStatic<FileUtils> fileUtils = Mockito.mockStatic(FileUtils.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            vrunner.when(() -> VRunner.exec(anyString())).thenAnswer(invocation -> {
                commands.add(invocation.getArgument(0));
                return 0;
            });
            fileUtils.when(() -> FileUtils.getAbsolutePath(anyString(), any())).thenCallRealMethod();
            fileUtils.when(() -> FileUtils.loadFile(anyString(), any(), anyString())).thenAnswer(invocation -> {
                loadedFiles.add(invocation.getArgument(0) + " -> " + invocation.getArgument(2));
                return null;
            });

            new CreateInfobase(jobConfiguration).run();
        }
    }

    private static JobConfiguration createConfiguration(String basePath, String templateDBPath) {
        String config = """
            {
              "basePath": "%s",
              "rasAddress": "srv1c:1545",
              "initInfobase": {
                "templateDBPath": "%s"
              }
            }
            """.formatted(basePath.replace("\\", "\\\\"), templateDBPath);

        return ConfigurationReader.create(config);
    }
}
