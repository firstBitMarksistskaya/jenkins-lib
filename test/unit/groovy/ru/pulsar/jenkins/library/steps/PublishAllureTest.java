package ru.pulsar.jenkins.library.steps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.utils.TestUtils;

import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PublishAllureTest {

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void runUnstashesInitResultsOfBuildBaseAndEachStageBase() {
        // given
        // свои базы стадий готовятся параллельно и сохраняют отчеты инициализации под своими именами
        JobConfiguration config = ConfigurationReader.create("""
            {
              "basePath": "/Ssrv1c\\\\test_base",
              "stages": { "initSteps": true, "bdd": true, "yaxunit": true },
              "bdd": { "basePath": "/Ssrv1c\\\\test_base_bdd" },
              "yaxunit": { "basePath": "/Ssrv1c\\\\test_base_yaxunit" }
            }
            """);

        // when
        new PublishAllure(config).run();

        // then
        verify(steps).unstash("init-allure");
        verify(steps).unstash("init-allure-1");
        verify(steps).unstash("init-allure-2");
        verify(steps, times(3)).unstash(startsWith("init-allure"));
    }

    @Test
    void runUnstashesOnlyBuildBaseInitResultsWithoutStageBases() {
        // given
        JobConfiguration config = ConfigurationReader.create("""
            {
              "stages": { "initSteps": true, "bdd": true }
            }
            """);

        // when
        new PublishAllure(config).run();

        // then
        verify(steps).unstash("init-allure");
        verify(steps, times(1)).unstash(startsWith("init-allure"));
    }
}
