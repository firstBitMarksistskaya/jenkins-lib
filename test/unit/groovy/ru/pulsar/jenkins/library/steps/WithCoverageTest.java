package ru.pulsar.jenkins.library.steps;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.pulsar.jenkins.library.IStepExecutor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WithCoverageTest {

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

}
