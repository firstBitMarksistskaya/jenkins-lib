package ru.pulsar.jenkins.library.utils;

import groovy.lang.Closure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExclusiveAccessTest {

    // код разрешения выводится из BUILD_TAG сборки, в тестовом окружении это jenkins-project-1
    private static final String UCCODE = UUID.nameUUIDFromBytes("jenkins-project-1".getBytes(StandardCharsets.UTF_8)).toString();

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();
    private final List<String> commands = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(steps.withCredentials(anyList(), any(Closure.class))).thenAnswer(invocation -> {
            Closure<?> body = invocation.getArgument(1);
            return body.call();
        });
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void runPassesEmptyCodeForFileBases() {
        for (String basePath : new String[]{"", "/F/var/1c/test_base"}) {
            // given
            List<String> uccodes = new ArrayList<>();

            // when
            run(createConfiguration(basePath, "", "UNKNOWN_ID"), uccode -> uccodes.add(uccode));

            // then
            assertThat(uccodes).as(basePath).containsExactly("");
        }
        assertThat(commands).isEmpty();
    }

    @Test
    void runDoesWithoutSessionManagementOnServerBaseWithoutRasAddress() {
        // given
        List<String> uccodes = new ArrayList<>();

        // when
        run(createConfiguration("/Ssrv1c\\\\test_base", "", "cluster-admin"), uccode -> uccodes.add(uccode));

        // then
        // без адреса RAS сеансы не завершаются: операция пройдет, только если в базе нет других сеансов
        assertThat(uccodes).containsExactly("");
        assertThat(commands).isEmpty();
        verify(steps, never()).withCredentials(anyList(), any(Closure.class));
    }

    @Test
    void runKillsSessionsBeforeOperationAndUnlocksAfterItOnServerBase() {
        // given
        when(steps.fileExists("./tools/vrunner.json")).thenReturn(true);

        // when
        run(createConfiguration("/Ssrv1c\\\\test_base", "srv1c:1545", "UNKNOWN_ID"),
            uccode -> commands.add("operation" + uccode));

        // then
        assertThat(commands).containsExactly(
            "vrunner session kill --db \"test_base\" --ras srv1c:1545 --settings ./tools/vrunner.json --uccode " + UCCODE,
            "operation --uccode " + UCCODE,
            "vrunner session unlock --db \"test_base\" --ras srv1c:1545 --settings ./tools/vrunner.json"
        );
        verify(steps, never()).withCredentials(anyList(), any(Closure.class));
    }

    @Test
    void runUnlocksAndRethrowsWhenOperationFails() {
        // given
        RuntimeException failure = new RuntimeException("restore failed");

        // when
        Throwable thrown = catchThrowable(() -> run(createConfiguration("/Ssrv1c\\\\test_base", "srv1c:1545", "UNKNOWN_ID"),
            uccode -> {
                throw failure;
            }));

        // then
        assertThat(thrown).isSameAs(failure);
        assertThat(commands).containsExactly(
            "vrunner session kill --db \"test_base\" --ras srv1c:1545 --uccode " + UCCODE,
            "vrunner session unlock --db \"test_base\" --ras srv1c:1545"
        );
    }

    @Test
    void runPassesClusterAdminCredentialsToSessionCommands() {
        // when
        run(createConfiguration("/Ssrv1c\\\\test_base", "srv1c:1545", "cluster-admin"), uccode -> null);

        // then
        verify(steps).usernamePassword("cluster-admin", "RUNNER_CLUSTERADMIN_USER", "RUNNER_CLUSTERADMIN_PWD");
        verify(steps).withCredentials(anyList(), any(Closure.class));
        assertThat(commands).hasSize(2);
    }

    private void run(JobConfiguration jobConfiguration, Function<String, Object> operation) {
        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            vrunner.when(() -> VRunner.exec(anyString())).thenAnswer(invocation -> {
                commands.add(invocation.getArgument(0));
                return 0;
            });

            ExclusiveAccess.run(jobConfiguration, new Closure<Object>(null) {
                @SuppressWarnings("unused")
                public Object doCall(Object uccode) {
                    return operation.apply((String) uccode);
                }
            });
        }
    }

    private static JobConfiguration createConfiguration(String basePath, String rasAddress, String clusterAdmin) {
        String config = """
            {
              "basePath": "%s",
              "rasAddress": "%s",
              "secrets": {
                "clusterAdmin": "%s"
              }
            }
            """.formatted(basePath, rasAddress, clusterAdmin);

        return ConfigurationReader.create(config);
    }
}
