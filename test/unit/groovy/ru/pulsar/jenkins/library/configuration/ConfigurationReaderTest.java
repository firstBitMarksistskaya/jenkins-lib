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
    assertThat(jobConfiguration.getSonarScannerToolName()).isEqualTo("sonar-scanner");
  }

}