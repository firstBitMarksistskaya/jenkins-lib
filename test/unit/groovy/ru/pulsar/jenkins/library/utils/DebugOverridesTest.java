package ru.pulsar.jenkins.library.utils;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebugOverridesTest {

  @Test
  void resolveProfileKey_returnsPenultimateSegment() {
    assertThat(DebugOverrides.resolveProfileKey("CPC/ci_uh_MR/MR-1101"))
      .isEqualTo("ci_uh_MR");
  }

  @Test
  void resolveProfileKey_returnsNullForShortPath() {
    assertThat(DebugOverrides.resolveProfileKey("ci_uh_MR"))
      .isNull();
  }

  @Test
  void resolveProfileKey_returnsNullForNull() {
    assertThat(DebugOverrides.resolveProfileKey(null))
      .isNull();
  }

  @Test
  void resolveProfileKey_returnsNullForEmptyString() {
    assertThat(DebugOverrides.resolveProfileKey(""))
      .isNull();
  }

  @Test
  void normalizeTarget_normalizesRelativePath() {
    assertThat(DebugOverrides.normalizeTarget(".\\tools\\vrunner.json"))
      .isEqualTo("tools/vrunner.json");
  }

  @Test
  void validateTargetPath_rejectsAbsolutePath() {
    assertThatThrownBy(() -> DebugOverrides.validateTargetPath("C:/temp/file.json"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validateTargetPath_rejectsTraversal() {
    assertThatThrownBy(() -> DebugOverrides.validateTargetPath("../tools/vrunner.json"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validateTargetPath_rejectsUncPath() {
    assertThatThrownBy(() -> DebugOverrides.validateTargetPath("//server/share/file.json"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UNC target paths are not allowed");
  }

  @Test
  void validateDebugProfile_requiresReplacementFields() {
    Map<String, Object> replacement = new LinkedHashMap<>();
    replacement.put("target", "tools/vrunner.json");

    Map<String, Object> profile = new LinkedHashMap<>();
    profile.put("enabled", true);
    profile.put("replacements", List.of(replacement));

    assertThatThrownBy(() -> DebugOverrides.validateDebugProfile(profile))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validateDebugProfile_acceptsValidProfile() {
    Map<String, Object> profile = new LinkedHashMap<>();
    profile.put("enabled", true);
    profile.put("replacements", List.of(replacement("debug-1", "tools/vrunner.json")));

    DebugOverrides.validateDebugProfile(profile);
  }

  @Test
  void buildConfigFileProviderEntries_buildsVariables() {
    Map<String, Object> first = replacement("debug-1", "jobConfiguration.json");
    Map<String, Object> second = replacement("debug-2", "tools/vrunner.json");

    List entries = DebugOverrides.buildConfigFileProviderEntries(List.of(first, second));
    Map firstEntry = (Map) entries.get(0);
    Map secondEntry = (Map) entries.get(1);

    assertThat(entries).hasSize(2);
    assertThat(firstEntry.get("fileId").toString()).isEqualTo("debug-1");
    assertThat(firstEntry.get("variable").toString()).isEqualTo("DEBUG_OVERRIDE_FILE_0");
    assertThat(secondEntry.get("fileId").toString()).isEqualTo("debug-2");
    assertThat(secondEntry.get("variable").toString()).isEqualTo("DEBUG_OVERRIDE_FILE_1");
  }

  @Test
  void collectDownstreamTargets_returnsOnlyConfiguredDownstreamFiles() {
    List<String> targets = DebugOverrides.collectDownstreamTargets(List.of(
      replacement("debug-1", "jobConfiguration.json"),
      replacement("debug-2", "./tools/vrunner.json"),
      replacement("debug-3", "sonar-project.properties")
    ));

    assertThat(targets).containsExactly("tools/vrunner.json", "sonar-project.properties");
  }

  @Test
  void collectDownstreamTargets_returnsEmptyListForEmptyReplacements() {
    assertThat(DebugOverrides.collectDownstreamTargets(List.of()))
      .isEmpty();
  }

  @Test
  void buildStashIncludes_joinsTargets() {
    assertThat(DebugOverrides.buildStashIncludes(List.of("tools/vrunner.json", "sonar-project.properties")))
      .isEqualTo("tools/vrunner.json,sonar-project.properties");
  }

  @Test
  void buildStashIncludes_rejectsUnsafeTargets() {
    assertThatThrownBy(() -> DebugOverrides.buildStashIncludes(List.of("tools/*.json")))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Downstream targets are not stash-safe");
  }

  @Test
  void shouldTreatConfigFileProviderErrorAsMissingPlugin_detectsMissingDslMethod() {
    Exception exception = new RuntimeException("No such DSL method 'configFileProvider' found among steps");

    assertThat(DebugOverrides.shouldTreatConfigFileProviderErrorAsMissingPlugin(exception))
      .isTrue();
  }

  @Test
  void shouldTreatConfigFileProviderErrorAsMissingControlFile_detectsMissingManagedFile() {
    Exception exception = new RuntimeException(
      "Managed file with id jenkins-debug-overrides-control not found"
    );

    assertThat(DebugOverrides.shouldTreatConfigFileProviderErrorAsMissingControlFile(exception))
      .isTrue();
  }

  @Test
  void shouldTreatConfigFileProviderErrorAsInvalidControlFile_detectsInvalidJsonException() {
    Exception exception = new RuntimeException(
      "Failed to parse jenkins-debug-overrides-control: Unexpected character at line 1"
    );
    exception.setStackTrace(new StackTraceElement[] {
      new StackTraceElement("applyDebugOverridesIfNeeded", "loadControlConfig", "applyDebugOverridesIfNeeded.groovy", 77)
    });

    assertThat(DebugOverrides.shouldTreatConfigFileProviderErrorAsInvalidControlFile(exception))
      .isTrue();
  }

  @Test
  void shouldTreatConfigFileProviderErrorAsInvalidControlFile_ignoresUnrelatedJsonErrors() {
    Exception exception = new RuntimeException("Unexpected character at line 1");

    assertThat(DebugOverrides.shouldTreatConfigFileProviderErrorAsInvalidControlFile(exception))
      .isFalse();
  }

  @Test
  void shouldTreatUnstashErrorAsMissingStash_detectsMissingStash() {
    Exception exception = new RuntimeException("No such saved stash 'debug-overrides-files'");

    assertThat(DebugOverrides.shouldTreatUnstashErrorAsMissingStash(exception))
      .isTrue();
  }

  @Test
  void shouldTreatUnstashErrorAsMissingStash_normalizesCurlyQuotes() {
    Exception exception = new RuntimeException("No such saved stash \u2018debug-overrides-files\u2019");

    assertThat(DebugOverrides.shouldTreatUnstashErrorAsMissingStash(exception))
      .isTrue();
  }

  private static Map<String, Object> replacement(String fileId, String target) {
    Map<String, Object> replacement = new LinkedHashMap<>();
    replacement.put("fileId", fileId);
    replacement.put("target", target);
    return replacement;
  }
}
