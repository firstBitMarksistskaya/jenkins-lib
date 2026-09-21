package ru.pulsar.jenkins.library.configuration.notification.im;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageBuilderTest {

  @Test
  @DisplayName("BDD сценарии / YAXUnit тесты — ветки parallel, одна строка на шаг")
  void parallelBranchesAreReported() {
    assertThat(report("STAGE", "UNSTABLE", false, "STAGE", "SUCCESS", true)).isTrue();
    assertThat(report("STAGE", "UNSTABLE", false, "STAGE", "UNSTABLE", true)).isTrue();
  }

  @Test
  @DisplayName("Выполнение BDD/YAXUnit — вложенные sequential, из сообщения PR-376")
  void nestedExecutionStagesAreDropped() {
    assertThat(report("STAGE", "UNSTABLE", false, "STAGE", "UNSTABLE", false)).isFalse();
  }

  @Test
  @DisplayName("контейнеры Подготовка / Проверка качества с parallel-ребёнком не репортим")
  void containersWithParallelChildAreDropped() {
    assertThat(report("STAGE", "UNSTABLE", true, null, null, false)).isFalse();
    assertThat(report("PARALLEL", "UNSTABLE", false, "STAGE", "UNSTABLE", true)).isFalse();
  }

  @Test
  @DisplayName("Распаковка ИБ внутри логического шага не репортим")
  void nestedUnpackStageIsDropped() {
    assertThat(report("STAGE", "FAILURE", false, "STAGE", "FAILURE", false)).isFalse();
  }

  @Test
  @DisplayName("SonarQube и pre-stage без контейнера-родителя репортим")
  void topLevelStagesAreReported() {
    assertThat(report("STAGE", "FAILURE", false, null, null, false)).isTrue();
    assertThat(report("STAGE", "UNSTABLE", false, "STEP", "SUCCESS", false)).isTrue();
  }

  @Test
  @DisplayName("SUCCESS и NOT_BUILT не репортим")
  void successfulAndSkippedAreDropped() {
    assertThat(report("STAGE", "SUCCESS", false, "STAGE", "SUCCESS", true)).isFalse();
    assertThat(report("STAGE", "NOT_BUILT", false, "STAGE", "SUCCESS", true)).isFalse();
  }

  private static boolean report(
      String type,
      String result,
      boolean isContainer,
      String parentType,
      String parentResult,
      boolean parentIsContainer
  ) {
    return NotificationMessageBuilder.shouldReportStage(
        type,
        result,
        isContainer,
        parentType,
        parentResult,
        parentIsContainer
    );
  }
}
