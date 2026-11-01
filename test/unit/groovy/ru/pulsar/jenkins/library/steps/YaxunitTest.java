package ru.pulsar.jenkins.library.steps;

import groovy.lang.Closure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.utils.TestUtils;
import ru.pulsar.jenkins.library.utils.VRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class YaxunitTest {

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();

    @BeforeEach
    void setUp() {
        when(steps.withEnv(anyList(), any(Closure.class))).thenAnswer(invocation -> {
            Closure<?> body = invocation.getArgument(1);
            return body.call();
        });
        when(steps.withCoverage(any(), any(), any(), any(Closure.class))).thenAnswer(invocation -> {
            Closure<?> body = invocation.getArgument(3);
            return body.call();
        });
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void runUsesLocalBase() {
        // when
        List<String> commands = runTestsCommands("");

        // then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0)).contains("--ibconnection \"/F./build/ib\"");
    }

    @Test
    void runUsesExternalBase() {
        // when
        List<String> commands = runTestsCommands("/Ssrv1c\\\\test_base");

        // then
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0)).contains("--ibconnection \"/Ssrv1c\\test_base\"");
    }

    private List<String> runTestsCommands(String basePath) {
        String config = """
            {
              "basePath": "%s",
              "stages": {
                "yaxunit": true
              }
            }
            """.formatted(basePath);
        JobConfiguration jobConfiguration = ConfigurationReader.create(config);
        List<String> commands = new ArrayList<>();

        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            vrunner.when(() -> VRunner.exec(anyString(), eq(true))).thenAnswer(invocation -> {
                commands.add(invocation.getArgument(0));
                return 0;
            });

            new Yaxunit(jobConfiguration).run();
        }

        return commands;
    }
}
