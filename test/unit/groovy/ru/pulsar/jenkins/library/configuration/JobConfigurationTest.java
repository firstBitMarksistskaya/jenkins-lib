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

    @Test
    void stageWorksWithOwnBaseInCopyOfConfiguration() {
        // given
        JobConfiguration jobConfiguration = createStagesConfiguration("/Ssrv1c\\\\ssl", """
            "bdd": {},
            "yaxunit": { "basePath": "/Ssrv1c\\\\ssl2" }
            """);

        // then
        assertThat(jobConfiguration.baseName("yaxunit")).isEqualTo("/Ssrv1c\\ssl2");
        assertThat(jobConfiguration.baseName("bdd")).isEqualTo("/Ssrv1c\\ssl");
        // шаги стадии берут базу из basePath копии, сама конфигурация сборки не меняется
        JobConfiguration yaxunitConfiguration = jobConfiguration.forStage("yaxunit");
        assertThat(yaxunitConfiguration.getBasePath()).isEqualTo("/Ssrv1c\\ssl2");
        assertThat(yaxunitConfiguration.getYaxunitOptions()).isSameAs(jobConfiguration.getYaxunitOptions());
        assertThat(jobConfiguration.getBasePath()).isEqualTo("/Ssrv1c\\ssl");
        assertThat(jobConfiguration.forStage("bdd")).isSameAs(jobConfiguration);
    }

    @Test
    void stageBasesAreOwnBasesOfEnabledStagesExceptBuildBase() {
        // given
        // smoke - та же база, что у bdd, в другой записи, yaxunit - база сборки, база выключенной стадии не нужна
        JobConfiguration jobConfiguration = createStagesConfiguration("/Ssrv1c\\\\ssl", """
            "bdd": { "basePath": "/Ssrv1c\\\\ssl2" },
            "smoke": { "basePath": "/SSRV1C:1541\\\\SSL2" },
            "yaxunit": { "basePath": "/SSrv1C\\\\SSL" },
            "syntaxCheck": { "basePath": "/Ssrv1c\\\\ssl3" }
            """, "syntaxCheck");

        // then
        assertThat(jobConfiguration.enabledTestStages()).containsExactly("bdd", "smoke", "yaxunit");
        assertThat(jobConfiguration.stageBases()).containsExactly("/Ssrv1c\\ssl2");
        assertThat(jobConfiguration.buildBaseNeeded()).isTrue();
    }

    @Test
    void buildBaseIsNotNeededWhenAllStagesHaveOwnBases() {
        // given
        String config = """
            {
              "initInfobase": { "initMethod": "notInit" },
              "stages": { "bdd": true, "yaxunit": true },
              "bdd": { "basePath": "/Ssrv1c\\\\ssl2" },
              "yaxunit": { "basePath": "/Ssrv1c\\\\ssl3" }
            }
            """;
        JobConfiguration jobConfiguration = ConfigurationReader.create(config);

        // when
        jobConfiguration.validateInitMethod();

        // then
        // локальная база сборки не создается, поэтому notInit без шаблона допустим
        assertThat(jobConfiguration.buildBaseNeeded()).isFalse();
        assertThat(jobConfiguration.stageBases()).containsExactly("/Ssrv1c\\ssl2", "/Ssrv1c\\ssl3");
        verify(steps, never()).error(anyString());
    }

    @Test
    void validateStageBasesFailsForRelativeFileBase() {
        // given
        JobConfiguration jobConfiguration = createStagesConfiguration("", """
            "bdd": { "basePath": "/F./build/bdd" }
            """);

        // when
        jobConfiguration.validateStageBases();

        // then
        verify(steps).error(contains("/F./build/bdd"));
    }

    @Test
    void validateStageBasesPassesForServerAndAbsoluteFileBases() {
        // given
        JobConfiguration jobConfiguration = createStagesConfiguration("", """
            "bdd": { "basePath": "/Ssrv1c\\\\ssl2" },
            "yaxunit": { "basePath": "/F/var/1c/yaxunit" }
            """);

        // when
        jobConfiguration.validateStageBases();

        // then
        verify(steps, never()).error(anyString());
    }

    // Конфигурация с включенными тестовыми стадиями, кроме перечисленных в disabledStages
    private static JobConfiguration createStagesConfiguration(String basePath, String stageOptions, String... disabledStages) {
        List<String> disabled = List.of(disabledStages);
        String stages = String.join(", ", List.of("bdd", "syntaxCheck", "smoke", "yaxunit").stream()
            .map(stage -> "\"%s\": %s".formatted(stage, !disabled.contains(stage)))
            .toList());
        String config = """
            {
              "basePath": "%s",
              "stages": { %s },
              %s
            }
            """.formatted(basePath, stages, stageOptions);

        return ConfigurationReader.create(config);
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
