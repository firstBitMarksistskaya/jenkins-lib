package ru.pulsar.jenkins.library.configuration;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.utils.TestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationReaderTest {

  @BeforeEach
  void setUp() {
    TestUtils.setupMockedContext();
  }

  @Test
  void testCreateJobConfigurationObject() throws IOException {
    // given
    String config = IOUtils.resourceToString(
      "jobConfiguration.json",
      StandardCharsets.UTF_8,
      this.getClass().getClassLoader()
    );

    // when
    JobConfiguration jobConfiguration = ConfigurationReader.create(config);

    // then
    assertThat(jobConfiguration.getV8version()).isEqualTo("8.3.14.1944");

    assertThat(jobConfiguration.getSonarQubeOptions().getSonarScannerToolName()).isEqualTo("sonar-scanner");
    assertThat(jobConfiguration.getSonarQubeOptions().getSonarQubeInstallation()).isEqualTo("qa");
    assertThat(jobConfiguration.getSonarQubeOptions().getUseSonarScannerFromPath()).isTrue();

    assertThat(jobConfiguration.getSecrets())
      .hasFieldOrPropertyWithValue("storage", "1234")
      .hasFieldOrPropertyWithValue("storagePath", "UNKNOWN_ID")
    ;

    assertThat(jobConfiguration.getSyntaxCheckOptions().getCheckModes()).hasSize(1);

    assertThat(jobConfiguration.getResultsTransformOptions().getRemoveSupport()).isFalse();
    assertThat(jobConfiguration.getResultsTransformOptions().getSupportLevel()).isZero();

    assertThat(jobConfiguration.getSmokeTestOptions().getVrunnerSettings()).contains("./tools/vrunner-smoke.json");
    assertThat(jobConfiguration.getSmokeTestOptions().isPublishToAllureReport()).isFalse();
    assertThat(jobConfiguration.getSmokeTestOptions().isPublishToJUnitReport()).isTrue();

    assertThat(jobConfiguration.getInitInfoBaseOptions().getRunMigration()).isFalse();
    assertThat(jobConfiguration.getInitInfoBaseOptions().getAdditionalInitializationSteps()).contains("vanessa --settings ./tools/vrunner.first.json");

    assertThat(jobConfiguration.getBddOptions().getVrunnerSteps()).contains("vanessa --settings ./tools/vrunner.json");

    assertThat(jobConfiguration.getLogosConfig()).isEqualTo("logger.rootLogger=DEBUG");

    assertThat(jobConfiguration.getTimeoutOptions().getBdd()).isEqualTo(120);
    assertThat(jobConfiguration.getTimeoutOptions().getZipInfoBase()).isEqualTo(123);
  }

}