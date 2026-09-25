package ru.pulsar.jenkins.library.steps;

import groovy.lang.Closure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.GlobalCoverageOptions;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.configuration.StepCoverageOptions;
import ru.pulsar.jenkins.library.utils.EnvUtils;
import ru.pulsar.jenkins.library.utils.TestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WithCoverageTest {

    private IStepExecutor steps;
    private JobConfiguration config;
    private StepCoverageOptions options;
    private Coverable stage;
    private EnvUtils env;

    @BeforeEach
    void setUp() {
        steps = mock(IStepExecutor.class);
        env = new EnvUtils();
        env.WORKSPACE = new java.io.File("build/test-workspace").getAbsolutePath();
        env.STAGE_NAME = "BDD scenarios";
        when(steps.env()).thenReturn(env);
        when(steps.isUnix()).thenReturn(false);
        when(steps.lock(anyString(), any(Closure.class))).thenAnswer(invocation ->
                ((Closure<?>) invocation.getArgument(1)).call());
        when(steps.bat(anyString(), eq(false), eq(true), eq("UTF-8")))
                .thenReturn("1001");
        when(steps.bat(anyString(), eq(true), eq(false), eq("UTF-8")))
                .thenReturn(0);

        GlobalCoverageOptions global = new GlobalCoverageOptions();
        global.setDbgsPath("C:\\Program Files\\1cv8\\dbgs.exe");
        global.setCoverage41CPath("C:\\tools\\Coverage\\Coverage41C.bat");
        config = new JobConfiguration();
        config.setSrcDir("src/cf");
        config.setCoverageOptions(global);
        options = new StepCoverageOptions();
        options.setCoverage(true);
        options.setDbgsPort(1550);
        stage = mock(Coverable.class);
        when(stage.getStageSlug()).thenReturn("bdd");
        when(stage.getCoveragePidsPath()).thenReturn("build/bdd-pids");
        when(stage.getCoverageStashName()).thenReturn("coverage-bdd");
        when(stage.getCoverageStashPath()).thenReturn("build/out/bdd");
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void startsWindowsDbgsWithEncodedPowerShellAndReturnsExactPid() {
        IStepExecutor steps = mock(IStepExecutor.class);
        when(steps.isUnix()).thenReturn(false);
        when(steps.bat(anyString(), eq(false), eq(true), eq("UTF-8")))
                .thenReturn(" 12048\r\n");
        String executable = "C:\\Program Files\\100%!\\1Cv8\\$bin\\dbgs.exe";
        String stdout = ".\\build\\BDD's 100%! $out.log";
        String stderr = ".\\build\\BDD's 100%! $err.log";

        String pid = WithCoverage.startDbgs(steps, executable, 1550, stdout, stderr);

        assertThat(pid).isEqualTo("12048");
        ArgumentCaptor<String> command = ArgumentCaptor.forClass(String.class);
        verify(steps).bat(command.capture(), eq(false), eq(true), eq("UTF-8"));
        assertThat(command.getValue())
                .doesNotContain(executable)
                .doesNotContain(stdout)
                .doesNotContain(stderr);
        String encoded = command.getValue().substring(
                command.getValue().indexOf("-EncodedCommand ") + "-EncodedCommand ".length()
        ).trim();
        String script = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_16LE);
        assertThat(script)
                .contains("Start-Process")
                .contains("-PassThru")
                .contains("$p.Id")
                .contains("--addr=127.0.0.1")
                .contains("--port=1550")
                .contains("BDD''s 100%! $out.log")
                .contains("BDD''s 100%! $err.log");
    }

    @Test
    void rejectsInvalidPidOutput() {
        for (String output : Arrays.asList(
                null, "", "0", "-1", "12 13", "12\n13", "12 & whoami", "dbgs.exe 12")) {
            IStepExecutor steps = mock(IStepExecutor.class);
            when(steps.isUnix()).thenReturn(false);
            when(steps.bat(anyString(), eq(false), eq(true), eq("UTF-8")))
                    .thenReturn(output);

            assertThatThrownBy(() -> WithCoverage.startDbgs(
                    steps, "C:\\1cv8\\dbgs.exe", 1550, "out.log", "err.log"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PID dbgs");
        }
    }

    @Test
    void startsLinuxDbgsAndReturnsExactPid() {
        IStepExecutor steps = mock(IStepExecutor.class);
        when(steps.isUnix()).thenReturn(true);
        when(steps.sh(anyString(), eq(false), eq(true), eq("UTF-8")))
                .thenReturn("2317\n");

        String pid = WithCoverage.startDbgs(
                steps,
                "/opt/1cv8 dir/dbgs",
                1551,
                "./build/YAXUnit's output.log",
                "./build/YAXUnit's error.log"
        );

        assertThat(pid).isEqualTo("2317");
        ArgumentCaptor<String> command = ArgumentCaptor.forClass(String.class);
        verify(steps).sh(command.capture(), eq(false), eq(true), eq("UTF-8"));
        assertThat(command.getValue()).isEqualTo(
                "'/opt/1cv8 dir/dbgs' --addr=127.0.0.1 --port=1551 "
                        + "> './build/YAXUnit'\"'\"'s output.log' 2>&1 "
                        + "& printf '%s\\n' \"$!\""
        );
    }

    @Test
    void returnsMissingWindowsProcessStatusDuringCleanup() {
        IStepExecutor steps = mock(IStepExecutor.class);
        when(steps.isUnix()).thenReturn(false);
        when(steps.bat(anyString(), eq(true), eq(false), eq("UTF-8")))
                .thenReturn(128);

        int status = WithCoverage.stopDbgs(steps, "12048");

        assertThat(status).isEqualTo(128);
        ArgumentCaptor<String> command = ArgumentCaptor.forClass(String.class);
        verify(steps).bat(command.capture(), eq(true), eq(false), eq("UTF-8"));
        assertThat(command.getValue())
                .contains("taskkill /PID 12048 /F")
                .doesNotContain("12049");
    }

    @Test
    void stopsOnlyExactLinuxPidDuringCleanup() {
        IStepExecutor steps = mock(IStepExecutor.class);
        when(steps.isUnix()).thenReturn(true);
        when(steps.sh(anyString(), eq(true), eq(false), eq("UTF-8")))
                .thenReturn(0);

        int status = WithCoverage.stopDbgs(steps, "2317");

        assertThat(status).isZero();
        verify(steps).sh(
                eq("kill 2317 >/dev/null 2>&1"),
                eq(true), eq(false), eq("UTF-8")
        );
    }

    @Test
    void rejectsShellTextBeforeCleanupCommand() {
        IStepExecutor steps = mock(IStepExecutor.class);

        assertThatThrownBy(() -> WithCoverage.stopDbgs(steps, "12 & whoami"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PID dbgs");
        verify(steps, never()).bat(anyString(), eq(true), eq(false), eq("UTF-8"));
        verify(steps, never()).sh(anyString(), eq(true), eq(false), eq("UTF-8"));
    }

    @Test
    void clearsStalePidAndOwnsNewPidBeforeCoverageCheck() {
        new WithCoverage(config, stage, options, successfulBody()).run();

        InOrder order = inOrder(steps);
        order.verify(steps).writeFile("build/bdd-pids", "", "UTF-8");
        order.verify(steps).bat(anyString(), eq(false), eq(true), eq("UTF-8"));
        order.verify(steps).writeFile("build/bdd-pids", "1001", "UTF-8");
        order.verify(steps).start(eq("C:\\tools\\Coverage\\Coverage41C.bat"), anyString());
        verify(steps, never()).readFile("build/bdd-pids");
        ArgumentCaptor<String> cleanup = ArgumentCaptor.forClass(String.class);
        verify(steps).bat(cleanup.capture(), eq(true), eq(false), eq("UTF-8"));
        assertThat(cleanup.getValue()).contains("/PID 1001").doesNotContain("9999");
    }

    @Test
    void clearFailurePreventsDbgsLaunchAndCleanup() {
        doThrow(new RuntimeException("PID clear failed"))
                .when(steps).writeFile("build/bdd-pids", "", "UTF-8");

        assertThatThrownBy(() -> new WithCoverage(config, stage, options, successfulBody()).run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("PID clear failed");

        verify(steps, never()).bat(anyString(), eq(false), eq(true), eq("UTF-8"));
        verify(steps, never()).bat(anyString(), eq(true), eq(false), eq("UTF-8"));
    }

    @Test
    void diagnosticWriteFailureStillCleansOwnedPid() {
        doThrow(new RuntimeException("PID write failed"))
                .when(steps).writeFile("build/bdd-pids", "1001", "UTF-8");

        assertThatThrownBy(() -> new WithCoverage(config, stage, options, successfulBody()).run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("PID write failed");

        ArgumentCaptor<String> cleanup = ArgumentCaptor.forClass(String.class);
        verify(steps).bat(cleanup.capture(), eq(true), eq(false), eq("UTF-8"));
        assertThat(cleanup.getValue()).contains("/PID 1001");
    }

    @Test
    void startupFailureSkipsBodyAndCleanup() {
        doThrow(new RuntimeException("startup failed"))
                .when(steps).bat(anyString(), eq(false), eq(true), eq("UTF-8"));
        Closure<?> body = mock(Closure.class);

        assertThatThrownBy(() -> new WithCoverage(config, stage, options, body).run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("startup failed");

        verify(body, never()).call();
        verify(steps, never()).bat(anyString(), eq(true), eq(false), eq("UTF-8"));
    }

    @Test
    void bodyFailureRemainsPrimaryWhenCleanupThrows() {
        doThrow(new RuntimeException("cleanup failed"))
                .when(steps).bat(anyString(), eq(true), eq(false), eq("UTF-8"));

        Closure<?> body = failingBody("body failed");
        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() ->
                new WithCoverage(config, stage, options, body).run());

        assertThat(failure).hasMessage("body failed");
        assertThat(failure.getSuppressed()).hasSize(1);
        assertThat(failure.getSuppressed()[0]).hasMessage("cleanup failed");
    }

    @Test
    void bodyFailureRemainsPrimaryWhenCleanupReturnsNonzeroStatus() {
        when(steps.bat(anyString(), eq(true), eq(false), eq("UTF-8")))
                .thenReturn(128);

        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() ->
                new WithCoverage(config, stage, options, failingBody("body failed")).run());

        assertThat(failure).hasMessage("body failed");
        assertThat(failure.getSuppressed()).isEmpty();
    }

    @Test
    void cleanupFailurePropagatesWhenItIsOnlyFailure() {
        doThrow(new RuntimeException("cleanup failed"))
                .when(steps).bat(anyString(), eq(true), eq(false), eq("UTF-8"));

        assertThatThrownBy(() -> new WithCoverage(config, stage, options, successfulBody()).run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("cleanup failed");
    }

    @Test
    void twoParallelRunsCleanOnlyTheirOwnedDbgs() throws Exception {
        ParallelCoverageHarness harness = new ParallelCoverageHarness();
        TestUtils.setupMockedContext(harness.steps());
        ParallelCoverageHarness.Scenario bdd =
                new ParallelCoverageHarness.Scenario("BDD scenarios", 1550, "1001");
        ParallelCoverageHarness.Scenario yaxunit =
                new ParallelCoverageHarness.Scenario("YAXUnit", 1551, "1002");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> bddRun = pool.submit(() -> runParallelScenario(
                    harness, bdd, failingBody("BDD body failed")));
            Future<?> yaxunitRun = pool.submit(() -> runParallelScenario(
                    harness, yaxunit, successfulBody()));

            Throwable bddFailure = null;
            try {
                bddRun.get(20, TimeUnit.SECONDS);
            } catch (ExecutionException failure) {
                bddFailure = failure.getCause();
            }
            yaxunitRun.get(20, TimeUnit.SECONDS);

            assertThat(bddFailure).hasMessage("BDD body failed");
            assertThat(bdd.locks).containsExactly("built-in_1550");
            assertThat(yaxunit.locks).containsExactly("built-in_1551");
            assertThat(bdd.cleanupCommands).hasSize(1);
            assertThat(yaxunit.cleanupCommands).hasSize(1);
            assertThat(bdd.cleanupCommands.get(0))
                    .contains("/PID 1001").doesNotContain("1002");
            assertThat(yaxunit.cleanupCommands.get(0))
                    .contains("/PID 1002").doesNotContain("1001");
        } finally {
            pool.shutdownNow();
        }
    }

    private void runParallelScenario(ParallelCoverageHarness harness,
                                     ParallelCoverageHarness.Scenario scenario,
                                     Closure<?> body) {
        harness.bind(scenario);
        try {
            JobConfiguration scenarioConfig = new JobConfiguration();
            scenarioConfig.setSrcDir(config.getSrcDir());
            scenarioConfig.setCoverageOptions(config.getCoverageOptions());
            StepCoverageOptions scenarioOptions = new StepCoverageOptions();
            scenarioOptions.setCoverage(true);
            scenarioOptions.setDbgsPort(scenario.port);
            Coverable scenarioStage = mock(Coverable.class);
            when(scenarioStage.getStageSlug()).thenReturn(scenario.name);
            when(scenarioStage.getCoveragePidsPath()).thenReturn("build/" + scenario.name + "-pids");
            when(scenarioStage.getCoverageStashName()).thenReturn("coverage-" + scenario.name);
            when(scenarioStage.getCoverageStashPath()).thenReturn("build/out/" + scenario.name);
            new WithCoverage(scenarioConfig, scenarioStage, scenarioOptions, body).run();
        } finally {
            harness.unbind();
        }
    }

    private Closure<?> successfulBody() {
        return new Closure<Object>(this) {
            public Object doCall() {
                return null;
            }
        };
    }

    private Closure<?> failingBody(String message) {
        return new Closure<Object>(this) {
            public Object doCall() {
                throw new RuntimeException(message);
            }
        };
    }

}
