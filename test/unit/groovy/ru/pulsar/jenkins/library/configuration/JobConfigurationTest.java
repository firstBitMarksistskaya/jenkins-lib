package ru.pulsar.jenkins.library.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.utils.TestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JobConfigurationTest {

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void needLoadConfigurationIsFalseForNotInit() {
        // given
        JobConfiguration jobConfiguration = createConfiguration("notInit", "template.dt", "");

        // then
        assertThat(jobConfiguration.needLoadConfiguration()).isFalse();
    }

    @Test
    void needLoadConfigurationIsTrueForOtherInitMethods() {
        for (String initMethod : new String[]{"fromStorage", "fromSource", "defaultBranchFromStorage"}) {
            // given
            JobConfiguration jobConfiguration = createConfiguration(initMethod, "", "");

            // then
            assertThat(jobConfiguration.needLoadConfiguration()).as(initMethod).isTrue();
        }
    }

    @Test
    void validateInitMethodFailsForNotInitWithoutTemplateAndBasePath() {
        // given
        JobConfiguration jobConfiguration = createConfiguration("notInit", "", "");

        // when
        jobConfiguration.validateInitMethod();

        // then
        verify(steps).error(contains("notInit"));
    }

    @Test
    void validateInitMethodPassesForNotInitWithTemplate() {
        // given
        JobConfiguration jobConfiguration = createConfiguration("notInit", "template.dt", "");

        // when
        jobConfiguration.validateInitMethod();

        // then
        verify(steps, never()).error(anyString());
    }

    @Test
    void validateInitMethodPassesForNotInitWithBasePath() {
        // given
        JobConfiguration jobConfiguration = createConfiguration("notInit", "", "/F/var/1c/test-base");

        // when
        jobConfiguration.validateInitMethod();

        // then
        verify(steps, never()).error(anyString());
    }

    @Test
    void validateInitMethodPassesForOtherInitMethodsWithoutTemplateAndBasePath() {
        for (String initMethod : new String[]{"fromStorage", "fromSource", "defaultBranchFromStorage"}) {
            // given
            JobConfiguration jobConfiguration = createConfiguration(initMethod, "", "");

            // when
            jobConfiguration.validateInitMethod();
        }

        // then
        verify(steps, never()).error(anyString());
    }

    @Test
    void stageExtensionsAreExtensionsWithStagesWithoutInitInfoBase() {
        // given
        JobConfiguration jobConfiguration = ConfigurationReader.create("""
            {
              "initInfobase": {
                "extensions": [
                  { "name": "byDefault" },
                  { "name": "allStages", "stages": [] },
                  { "name": "initAndBdd", "stages": ["initInfoBase", "bdd"] },
                  { "name": "bdd", "stages": ["bdd"] },
                  { "name": "smokeAndYaxunit", "stages": ["smoke", "yaxunit"] }
                ]
              }
            }
            """);

        // then
        // расширения без стадий и с initInfoBase загружаются при подготовке и остаются в базе
        List<?> stageExtensions = jobConfiguration.stageExtensions();
        assertThat(stageExtensions)
            .extracting(extension -> ((InitInfoBaseOptions.Extension) extension).getName())
            .containsExactly("bdd", "smokeAndYaxunit");
    }

    private static JobConfiguration createConfiguration(String initMethod, String templateDBPath, String basePath) {
        String config = """
            {
              "basePath": "%s",
              "initInfobase": {
                "initMethod": "%s",
                "templateDBPath": "%s"
              }
            }
            """.formatted(basePath, initMethod, templateDBPath);

        return ConfigurationReader.create(config);
    }
}
