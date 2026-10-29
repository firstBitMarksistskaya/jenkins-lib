package ru.pulsar.jenkins.library.utils;

import groovy.lang.Closure;
import hudson.AbortException;
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
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExclusiveAccessTest {

    // код разрешения выводится из BUILD_TAG сборки, в тестовом окружении это jenkins-project-1
    private static final String UCCODE = UUID.nameUUIDFromBytes("jenkins-project-1".getBytes(StandardCharsets.UTF_8)).toString();

    private static final String RAC_PATH = "C:\\Program Files\\1cv8\\8.3.27.2214\\bin\\rac.exe";

    // вывод rac cluster list в шаге bat: сначала эхо команд, затем кластеры, разделенные пустой строкой
    private static final String RAC_CLUSTER_LIST = """
        C:\\ws>chcp 65001  1>nul\r
        \r
        C:\\ws>"C:\\Program Files\\1cv8\\8.3.27.2214\\bin\\rac.exe" cluster list srv1c:1545\r
        cluster                       : 0ee2d8a2-1111-4b6f-a0a8-7f1e2c3d4e5f\r
        host                          : srv1c\r
        port                          : 1541\r
        name                          : "Local cluster"\r
        expiration-timeout            : 60\r
        \r
        cluster                       : 9a8b7c6d-2222-4e1f-b2c3-d4e5f6a7b8c9\r
        host                          : srv1c\r
        port                          : 1641\r
        name                          : "Test cluster"\r
        expiration-timeout            : 60\r
        """;

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();
    private final List<String> commands = new ArrayList<>();
    private int failingKills;

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
        // без порта в строке подключения кластер не ищется: vanessa-runner берет первый кластер RAS
        verify(steps, never()).cmd(anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    void runSelectsClusterByPortFromConnection() {
        // given
        doReturn(RAC_PATH + "\n").when(steps).readFile(startsWith("build/tmp/racPath_"));
        doReturn(RAC_CLUSTER_LIST).when(steps).cmd(contains(" cluster list "), eq(false), eq(true));

        // when
        run(createConfiguration("/Ssrv1c:1641\\\\test_base", "srv1c:1545", "UNKNOWN_ID"), uccode -> null);

        // then
        verify(steps).cmd(startsWith("oscript build/tmp/rac_"));
        verify(steps).cmd("\"" + RAC_PATH + "\" cluster list srv1c:1545", false, true);
        assertThat(commands).containsExactly(
            "vrunner session kill --db \"test_base\" --ras srv1c:1545 --cluster 9a8b7c6d-2222-4e1f-b2c3-d4e5f6a7b8c9 --uccode " + UCCODE,
            "vrunner session unlock --db \"test_base\" --ras srv1c:1545 --cluster 9a8b7c6d-2222-4e1f-b2c3-d4e5f6a7b8c9"
        );
    }

    @Test
    void runFailsBeforeSessionCommandsWhenClusterWithPortIsNotFound() {
        // given
        doReturn(RAC_PATH).when(steps).readFile(startsWith("build/tmp/racPath_"));
        doReturn(RAC_CLUSTER_LIST).when(steps).cmd(contains(" cluster list "), eq(false), eq(true));
        doThrow(new RuntimeException("cluster not found")).when(steps).error(anyString());

        // when
        Throwable thrown = catchThrowable(() ->
            run(createConfiguration("/Ssrv1c:1741\\\\test_base", "srv1c:1545", "UNKNOWN_ID"), uccode -> null));

        // then
        assertThat(thrown).hasMessage("cluster not found");
        verify(steps).error(contains("1741"));
        verify(steps).error(contains("1541, 1641"));
        assertThat(commands).isEmpty();
    }

    @Test
    void clustersByPortParsesRacClusterList() {
        assertThat(ExclusiveAccess.clustersByPort(RAC_CLUSTER_LIST)).containsExactly(
            entry("1541", "0ee2d8a2-1111-4b6f-a0a8-7f1e2c3d4e5f"),
            entry("1641", "9a8b7c6d-2222-4e1f-b2c3-d4e5f6a7b8c9")
        );
        assertThat(ExclusiveAccess.clustersByPort("")).isEmpty();
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
    void runRepeatsKillAndOperationEveryMinuteWithinAttempts() {
        // given
        failingKills = 1;
        int[] failingOperations = {1};

        // when
        run(createConfiguration("/Ssrv1c\\\\test_base", "srv1c:1545", "UNKNOWN_ID"), 5, uccode -> {
            commands.add("operation" + uccode);
            if (failingOperations[0]-- > 0) {
                sneakyThrow(new AbortException("dump failed"));
            }
            return null;
        });

        // then
        // запрет снимается один раз, после успешной попытки
        String kill = "vrunner session kill --db \"test_base\" --ras srv1c:1545 --uccode " + UCCODE;
        String operation = "operation --uccode " + UCCODE;
        assertThat(commands).containsExactly(kill, kill, operation, kill, operation,
            "vrunner session unlock --db \"test_base\" --ras srv1c:1545");
        verify(steps, times(2)).sleep(60);
    }

    @Test
    void runFailsAfterLastAttempt() {
        // given
        failingKills = 3;

        // when
        Throwable thrown = catchThrowable(() ->
            run(createConfiguration("/Ssrv1c\\\\test_base", "srv1c:1545", "UNKNOWN_ID"), 3, uccode -> {
                commands.add("operation" + uccode);
                return null;
            }));

        // then
        // после последней попытки сборка получает ее ошибку, запрет снимается один раз
        String kill = "vrunner session kill --db \"test_base\" --ras srv1c:1545 --uccode " + UCCODE;
        assertThat(thrown).isInstanceOf(AbortException.class);
        assertThat(commands).containsExactly(kill, kill, kill,
            "vrunner session unlock --db \"test_base\" --ras srv1c:1545");
        verify(steps, times(2)).sleep(60);
    }

    @Test
    void runDoesNotRepeatCommandFailureByDefault() {
        // given
        failingKills = 1;

        // when
        Throwable thrown = catchThrowable(() ->
            run(createConfiguration("/Ssrv1c\\\\test_base", "srv1c:1545", "UNKNOWN_ID"), uccode -> null));

        // then
        assertThat(thrown).isInstanceOf(AbortException.class);
        assertThat(commands).containsExactly(
            "vrunner session kill --db \"test_base\" --ras srv1c:1545 --uccode " + UCCODE,
            "vrunner session unlock --db \"test_base\" --ras srv1c:1545"
        );
        verify(steps, never()).sleep(anyInt());
    }

    @Test
    void runDoesNotRepeatOtherErrors() {
        // given
        RuntimeException failure = new RuntimeException("restore failed");

        // when
        Throwable thrown = catchThrowable(() ->
            run(createConfiguration("/Ssrv1c\\\\test_base", "srv1c:1545", "UNKNOWN_ID"), 5, uccode -> {
                throw failure;
            }));

        // then
        assertThat(thrown).isSameAs(failure);
        verify(steps, never()).sleep(anyInt());
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


    @Test
    void unsetIdUsesAvailableStandardCredential() {
        JobConfiguration config = createConfiguration("/Ssrv1c\\\\test_base", "srv1c:1545", "");
        when(steps.credentialExists("CLUSTER_ADMIN")).thenReturn(true);
        run(config, uccode -> null);
        verify(steps).usernamePassword("CLUSTER_ADMIN", "RUNNER_CLUSTERADMIN_USER", "RUNNER_CLUSTERADMIN_PWD");
    }

    @Test
    void bindingFailureNeverExecutesProtectedCommandOrBody() {
        for (String id : new String[]{"explicit-missing", "explicit-wrong-type", ""}) {
            JobConfiguration config = createConfiguration("/Ssrv1c\\\\test_base", "srv1c:1545", "");
            config.getSecrets().setClusterAdmin(id);
            when(steps.credentialExists("CLUSTER_ADMIN")).thenReturn(true);
            RuntimeException failure = new RuntimeException("binding failed");
            doThrow(failure).when(steps).withCredentials(anyList(), any(Closure.class));
            assertThat(catchThrowable(() -> run(config, uccode -> null))).isSameAs(failure);
            assertThat(commands).isEmpty();
        }
    }

    private void run(JobConfiguration jobConfiguration, Function<String, Object> operation) {
        run(jobConfiguration, 1, operation);
    }

    // AbortException проверяемое, а операция в тестах - Function
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable exception) throws T {
        throw (T) exception;
    }

    private void run(JobConfiguration jobConfiguration, int attempts, Function<String, Object> operation) {
        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            vrunner.when(() -> VRunner.exec(anyString())).thenAnswer(invocation -> {
                String command = invocation.getArgument(0);
                commands.add(command);
                if (command.contains(" session kill ") && failingKills-- > 0) {
                    throw new AbortException("kill failed");
                }
                return 0;
            });

            ExclusiveAccess.run(jobConfiguration, attempts, new Closure<Object>(null) {
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
