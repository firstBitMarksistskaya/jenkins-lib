package ru.pulsar.jenkins.library.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InfobaseLocksTest {

    private static final String BUILD_TAG = "jenkins-project-1";

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();
    private final List<Map<String, String>> buildResources = new ArrayList<>();
    private final Map<String, List<Map<String, String>>> stageResources = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext(steps);
        stageResources.put("bdd", new ArrayList<>());
        stageResources.put("syntaxCheck", new ArrayList<>());
    }

    @Test
    void fillLocksExternalBaseForBuildAndQueuesStagesOnIt() {
        // when
        fill("/SSrv1C\\\\Test_Base");

        // then
        assertThat(buildResources).containsExactly(Map.of("resource", "/Ssrv1c:1541\\test_base"));
        // имя внутренней блокировки отличается от внешней и одинаково у всех стадий на этой базе
        assertThat(stageResources.get("bdd")).containsExactly(Map.of("resource", "/Ssrv1c:1541\\test_base_" + BUILD_TAG));
        assertThat(stageResources.get("syntaxCheck")).containsExactly(Map.of("resource", "/Ssrv1c:1541\\test_base_" + BUILD_TAG));
        verify(steps).echo(contains("/Ssrv1c:1541\\test_base"));
    }

    @Test
    void fillUsesNamesNobodyElseRequestsForLocalBase() {
        // when
        fill("");

        // then
        // имена есть, но никто больше их не запросит: тестовые стадии на локальной базе идут параллельно
        assertThat(buildResources).containsExactly(Map.of("resource", BUILD_TAG));
        assertThat(stageResources.get("bdd")).containsExactly(Map.of("resource", BUILD_TAG + "_bdd"));
        assertThat(stageResources.get("syntaxCheck")).containsExactly(Map.of("resource", BUILD_TAG + "_syntaxCheck"));
        verify(steps, never()).echo(anyString());
    }

    @Test
    void fillDoesNotLockRelativeFileBase() {
        // when
        fill("/F./bases/test");

        // then
        assertThat(buildResources).containsExactly(Map.of("resource", BUILD_TAG));
        assertThat(stageResources.get("bdd")).containsExactly(Map.of("resource", BUILD_TAG + "_bdd"));
    }

    @Test
    void fillLocksAllBasesOfBuildAtOnce() {
        // given
        stageResources.put("yaxunit", new ArrayList<>());

        // when
        fillWithStages("/Ssrv1c\\\\ssl", """
            "stages": { "bdd": true, "yaxunit": true },
            "yaxunit": { "basePath": "/Ssrv1c\\\\ssl2" }
            """);

        // then
        // внешняя блокировка берет обе базы одним шагом, стадии на разных базах не ждут друг друга
        assertThat(buildResources).containsExactly(
            Map.of("resource", "/Ssrv1c:1541\\ssl"),
            Map.of("resource", "/Ssrv1c:1541\\ssl2"));
        assertThat(stageResources.get("bdd")).containsExactly(Map.of("resource", "/Ssrv1c:1541\\ssl_" + BUILD_TAG));
        assertThat(stageResources.get("yaxunit")).containsExactly(Map.of("resource", "/Ssrv1c:1541\\ssl2_" + BUILD_TAG));
    }

    @Test
    void fillDoesNotLockBuildBaseWithoutStagesOnIt() {
        // when
        fillWithStages("/Ssrv1c\\\\ssl", """
            "stages": { "bdd": true },
            "bdd": { "basePath": "/Ssrv1c\\\\ssl2" }
            """);

        // then
        // базу сборки не используют тесты этой сборки, ее может занять другая сборка
        assertThat(buildResources).containsExactly(Map.of("resource", "/Ssrv1c:1541\\ssl2"));
        assertThat(stageResources.get("bdd")).containsExactly(Map.of("resource", "/Ssrv1c:1541\\ssl2_" + BUILD_TAG));
    }

    @Test
    void fillQueuesStagesOnOwnBaseWhenBuildBaseIsLocal() {
        // when
        fillWithStages("", """
            "stages": { "bdd": true, "syntaxCheck": true },
            "bdd": { "basePath": "/Ssrv1c\\\\ssl2" }
            """);

        // then
        // локальная база сборки не блокируется, своя база стадии - блокируется
        assertThat(buildResources).containsExactly(Map.of("resource", "/Ssrv1c:1541\\ssl2"));
        assertThat(stageResources.get("bdd")).containsExactly(Map.of("resource", "/Ssrv1c:1541\\ssl2_" + BUILD_TAG));
        assertThat(stageResources.get("syntaxCheck")).containsExactly(Map.of("resource", BUILD_TAG + "_syntaxCheck"));
    }

    private void fill(String basePath) {
        fillWithStages(basePath, "");
    }

    private void fillWithStages(String basePath, String stageOptions) {
        String config = """
            {
              %s
              "basePath": "%s"
            }
            """.formatted(stageOptions.isBlank() ? "" : stageOptions.strip() + ",", basePath);
        JobConfiguration jobConfiguration = ConfigurationReader.create(config);

        InfobaseLocks.fill(jobConfiguration, BUILD_TAG, buildResources, stageResources);
    }
}
