package ru.pulsar.jenkins.library.steps;

import groovy.lang.Closure;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.utils.EnvUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ParallelCoverageHarness {

    static final class Scenario {
        final String name;
        final int port;
        final String pid;
        final List<String> locks = new CopyOnWriteArrayList<>();
        final List<String> cleanupCommands = new CopyOnWriteArrayList<>();

        Scenario(String name, int port, String pid) {
            this.name = name;
            this.port = port;
            this.pid = pid;
        }
    }

    private final ThreadLocal<Scenario> current = new ThreadLocal<>();
    private final CyclicBarrier started = new CyclicBarrier(2);
    private final IStepExecutor steps = mock(IStepExecutor.class);

    ParallelCoverageHarness() {
        when(steps.isUnix()).thenReturn(false);
        when(steps.env()).thenAnswer(invocation -> {
            Scenario scenario = scenario();
            EnvUtils env = new EnvUtils();
            env.NODE_NAME = "built-in";
            env.WORKSPACE = new File("build/parallel-workspace").getAbsolutePath();
            env.STAGE_NAME = scenario.name;
            return env;
        });
        when(steps.lock(anyString(), any(Closure.class))).thenAnswer(invocation -> {
            Scenario scenario = scenario();
            scenario.locks.add(invocation.getArgument(0));
            return ((Closure<?>) invocation.getArgument(1)).call();
        });
        when(steps.bat(anyString(), any(Boolean.class), any(Boolean.class), anyString()))
                .thenAnswer(invocation -> {
                    Scenario scenario = scenario();
                    String command = invocation.getArgument(0);
                    boolean returnStatus = invocation.getArgument(1);
                    boolean returnStdout = invocation.getArgument(2);
                    if (returnStdout) {
                        started.await(10, TimeUnit.SECONDS);
                        return scenario.pid;
                    }
                    if (returnStatus) {
                        scenario.cleanupCommands.add(command);
                        return 0;
                    }
                    return null;
                });
    }

    IStepExecutor steps() {
        return steps;
    }

    void bind(Scenario scenario) {
        current.set(scenario);
    }

    void unbind() {
        current.remove();
    }

    private Scenario scenario() {
        Scenario scenario = current.get();
        if (scenario == null) {
            throw new IllegalStateException("Parallel scenario is not bound to this thread");
        }
        return scenario;
    }
}
