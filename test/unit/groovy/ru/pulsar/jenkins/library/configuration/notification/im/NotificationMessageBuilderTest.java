package ru.pulsar.jenkins.library.configuration.notification.im;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageBuilderTest {

  @Test
  @DisplayName("BDD / YAXUnit / Дымовые тесты — нода PARALLEL, имя логического шага")
  void parallelBranchesAreReported() {
    assertThat(report("PARALLEL", "UNSTABLE", false, "STAGE", "SUCCESS", true)).isTrue();
    assertThat(report("PARALLEL", "UNSTABLE", false, "STAGE", "UNSTABLE", true)).isTrue();
  }

  @Test
  @DisplayName("Трансформация в формат EDT — PARALLEL без вложенных stages")
  void leafParallelBranchIsReported() {
    assertThat(report("PARALLEL", "FAILURE", false, "STAGE", "UNSTABLE", true)).isTrue();
  }

  @Test
  @DisplayName("Выполнение BDD/YAXUnit — вложенные sequential под PARALLEL")
  void nestedExecutionStagesAreDropped() {
    assertThat(report("STAGE", "UNSTABLE", false, "PARALLEL", "UNSTABLE", false)).isFalse();
    assertThat(report("STAGE", "UNSTABLE", false, "STAGE", "UNSTABLE", false)).isFalse();
  }

  @Test
  @DisplayName("Выполнение BDD: предыдущая стадия SUCCESS, но предок — ветка PARALLEL")
  void nestedStageAfterSuccessfulSiblingIsDropped() {
    assertThat(report("STAGE", "UNSTABLE", false, "STAGE", "SUCCESS", false, true)).isFalse();
  }

  @Test
  @DisplayName("контейнеры Подготовка / Проверка качества не репортим")
  void outerParallelContainersAreDropped() {
    assertThat(report("STAGE", "UNSTABLE", true, null, null, false)).isFalse();
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
    assertThat(report("PARALLEL", "SUCCESS", false, "STAGE", "SUCCESS", true)).isFalse();
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
    return report(type, result, isContainer, parentType, parentResult, parentIsContainer, false);
  }

  private static boolean report(
      String type,
      String result,
      boolean isContainer,
      String parentType,
      String parentResult,
      boolean parentIsContainer,
      boolean underParallelBranch
  ) {
    return NotificationMessageBuilder.shouldReportStage(
        type,
        result,
        isContainer,
        parentType,
        parentResult,
        parentIsContainer,
        underParallelBranch
    );
  }
}
