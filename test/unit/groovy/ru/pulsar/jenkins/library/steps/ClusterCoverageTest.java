package ru.pulsar.jenkins.library.steps;

import groovy.lang.Closure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.configuration.StepCoverageOptions;
import ru.pulsar.jenkins.library.utils.TestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClusterCoverageTest {

    private static final String CONNECTION = " -i test_base -u http://srv1c:1550";

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();
    private final List<String> events = new ArrayList<>();

    private final Coverable stage = new Coverable() {
        @Override
        public String getStageSlug() {
            return "bdd";
        }

        @Override
        public String getCoverageStashPath() {
            return "build/out/bdd-coverage.xml";
        }

        @Override
        public String getCoverageStashName() {
            return "bdd-coverage";
        }

        @Override
        public String getCoveragePidsPath() {
            return "build/bdd-pids";
        }
    };

    @BeforeEach
    void setUp() {
        when(steps.withCredentials(anyList(), any(Closure.class))).thenAnswer(invocation -> {
            Closure<?> body = invocation.getArgument(1);
            return body.call();
        });
        doAnswer(invocation -> {
            events.add("start " + invocation.getArgument(1));
            return null;
        }).when(steps).start(anyString(), anyString());
        doAnswer(invocation -> {
            events.add("cmd " + invocation.getArgument(0));
            return 0;
        }).when(steps).cmd(anyString());
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void runMeasuresOnClusterDebugServerWithoutDbgsAndPortLock() {
        // when
        run(createConfiguration("srv1c:1550", "UNKNOWN_ID"), () -> events.add("body"));

        // then
        assertThat(events).containsExactly(
            "start start" + CONNECTION + " -P ws -s src/cf -o build/out/bdd-coverage.xml",
            "cmd Coverage41C check" + CONNECTION,
            "body",
            "cmd Coverage41C stop" + CONNECTION
        );
        // dbgs на агенте не запускается, порт сервера отладки агента не блокируется
        verify(steps, never()).lock(anyString(), any(Closure.class));
        verify(steps, never()).bat(anyString(), anyBoolean(), anyBoolean(), anyString());
        verify(steps, never()).sh(anyString(), anyBoolean(), anyBoolean(), anyString());
        verify(steps).stash("bdd-coverage", "build/out/bdd-coverage.xml", true);
    }

    @Test
    void runPassesDebugServerPasswordOnlyToStart() {
        // when
        run(createConfiguration("srv1c:1550", "debug-server-password"), () -> events.add("body"));

        // then
        verify(steps).string("debug-server-password", "COVERAGE41C_DEBUG_SERVER_PASSWORD");
        assertThat(events.get(0)).endsWith(" -p:env COVERAGE41C_DEBUG_SERVER_PASSWORD");
        assertThat(events.subList(1, events.size())).allSatisfy(event -> assertThat(event).doesNotContain("-p"));
    }

    @Test
    void runFailsWithoutClusterDebugServer() {
        // given
        doThrow(new RuntimeException("no debug server")).when(steps).error(anyString());

        // when
        // без сервера отладки кластера покрытие получилось бы без серверного кода
        Throwable thrown = catchThrowable(() -> run(createConfiguration("", "UNKNOWN_ID"), () -> events.add("body")));

        // then
        assertThat(thrown).hasMessage("no debug server");
        verify(steps).error(contains("clusterDebugServer"));
        assertThat(events).isEmpty();
    }

    @Test
    void runStopsCoverage41CWhenTestsFail() {
        // given
        RuntimeException failure = new RuntimeException("tests failed");

        // when
        Throwable thrown = catchThrowable(() -> run(createConfiguration("srv1c:1550", "UNKNOWN_ID"), () -> {
            throw failure;
        }));

        // then
        // иначе Coverage41C остался бы подключенным к серверу отладки и не дал бы подключиться следующему замеру
        assertThat(thrown).isSameAs(failure);
        assertThat(events).last().isEqualTo("cmd Coverage41C stop" + CONNECTION);
        verify(steps, never()).stash(anyString(), anyString(), anyBoolean());
    }

    @Test
    void debugServerUrlKeepsExplicitScheme() {
        assertThat(ClusterCoverage.debugServerUrl("srv1c:1550")).isEqualTo("http://srv1c:1550");
        assertThat(ClusterCoverage.debugServerUrl(" https://srv1c:1550 ")).isEqualTo("https://srv1c:1550");
    }

    @Test
    void debugServerUrlUsesDefaultPortWhenOmitted() {
        // порт указывается, только если сервер отладки запущен не на стандартном 1550
        assertThat(ClusterCoverage.debugServerUrl("srv1c")).isEqualTo("http://srv1c:1550");
        assertThat(ClusterCoverage.debugServerUrl("srv1c:1551")).isEqualTo("http://srv1c:1551");
        assertThat(ClusterCoverage.debugServerUrl("https://srv1c")).isEqualTo("https://srv1c:1550");
        assertThat(ClusterCoverage.debugServerUrl("http://srv1c/")).isEqualTo("http://srv1c:1550/");
    }


    @Test
    void unsetIdUsesAvailableStandardCredential() {
        JobConfiguration config = createConfiguration("srv1c", "");
        when(steps.credentialExists("DEBUG_SERVER_PASSWORD")).thenReturn(true);
        run(config, () -> events.add("body"));
        verify(steps).string("DEBUG_SERVER_PASSWORD", "COVERAGE41C_DEBUG_SERVER_PASSWORD");
    }

    @Test
    void bindingFailureNeverExecutesProtectedCommandOrBody() {
        for (String id : new String[]{"explicit-missing", "explicit-wrong-type", ""}) {
            JobConfiguration config = createConfiguration("srv1c", "");
            config.getSecrets().setDebugServerPassword(id);
            when(steps.credentialExists("DEBUG_SERVER_PASSWORD")).thenReturn(true);
            RuntimeException failure = new RuntimeException("binding failed");
            doThrow(failure).when(steps).withCredentials(anyList(), any(Closure.class));
            assertThat(catchThrowable(() -> run(config, () -> events.add("body")))).isSameAs(failure);
            assertThat(events).isEmpty();
        }
    }

    private void run(JobConfiguration jobConfiguration, Runnable body) {
        StepCoverageOptions options = new StepCoverageOptions();
        options.setCoverage(true);

        new WithCoverage(jobConfiguration, stage, options, new Closure<Object>(null) {
            @SuppressWarnings("unused")
            public Object doCall() {
                body.run();
                return null;
            }
        }).run();
    }

    private static JobConfiguration createConfiguration(String clusterDebugServer, String password) {
        String config = """
            {
              "basePath": "/Ssrv1c\\\\test_base",
              "coverage": {
                "clusterDebugServer": "%s"
              },
              "secrets": {
                "debugServerPassword": "%s"
              }
            }
            """.formatted(clusterDebugServer, password);

        return ConfigurationReader.create(config);
    }
}
