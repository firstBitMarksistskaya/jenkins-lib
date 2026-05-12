package ru.pulsar.jenkins.library.steps;

import groovy.lang.Closure;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.utils.BspDetector;
import ru.pulsar.jenkins.library.utils.TestUtils;
import ru.pulsar.jenkins.library.utils.VRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitInfoBaseTest {

    private IStepExecutor steps;

    @BeforeEach
    void setUp() {
        steps = TestUtils.getMockedStepExecutor();

        when(steps.withEnv(anyList(), any(Closure.class)))
                .thenAnswer(invocation -> {
                    Closure<?> body = invocation.getArgument(1);
                    return body.call();
                });

        when(steps.catchError(any(Closure.class)))
                .thenAnswer(invocation -> {
                    Closure<?> body = invocation.getArgument(0);
                    return body.call();
                });

        TestUtils.setupMockedContext(steps);
    }

    @Test
    void runUsesMigrationStatusFileForBspConfiguration() throws IOException {

        // given
        String config = IOUtils.resourceToString(
                "initInfoBaseRunMigration.json",
                StandardCharsets.UTF_8,
                this.getClass().getClassLoader()
        );
        JobConfiguration jobConfiguration = ConfigurationReader.create(config);
        String modulePath = "src/cf/CommonModules/"
                + BspDetector.DEFAULT_INFO_BASE_UPDATE_MODULE_NAME
                + "/Ext/Module.bsl";
        when(steps.fileExists(modulePath)).thenReturn(true);
        List<String> commands = new ArrayList<>();

        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            vrunner.when(() -> VRunner.exec(anyString(), eq(true))).thenAnswer(invocation -> {
                commands.add(invocation.getArgument(0));
                return 0;
            });
            vrunner.when(() -> VRunner.readExitStatusFromFile("build/migration-exit-status.log", 0)).thenReturn(0);

            // when
            new InitInfoBase(jobConfiguration).run();

            // then
            assertThat(commands.get(0)).contains("--exitCodePath \"build/migration-exit-status.log\"");
            vrunner.verify(() -> VRunner.readExitStatusFromFile("build/migration-exit-status.log", 0));
            verify(steps, never()).error(anyString());
        }
    }

    @Test
    void runDoesNotUseMigrationStatusFileForNonBspConfiguration() throws IOException {

        // given
        String config = IOUtils.resourceToString(
                "initInfoBaseRunMigration.json",
                StandardCharsets.UTF_8,
                this.getClass().getClassLoader()
        );
        JobConfiguration jobConfiguration = ConfigurationReader.create(config);
        when(steps.fileExists(anyString())).thenReturn(false);
        List<String> commands = new ArrayList<>();

        try (MockedStatic<VRunner> vrunner = Mockito.mockStatic(VRunner.class)) {
            vrunner.when(VRunner::getVRunnerPath).thenReturn("vrunner");
            vrunner.when(() -> VRunner.exec(anyString(), eq(true))).thenAnswer(invocation -> {
                commands.add(invocation.getArgument(0));
                return 0;
            });

            // when
            new InitInfoBase(jobConfiguration).run();

            // then
            assertThat(commands.get(0)).doesNotContain("--exitCodePath");
            vrunner.verify(() -> VRunner.readExitStatusFromFile(anyString(), any(Integer.class)), never());
            verify(steps, never()).error(anyString());
        }
    }
}
