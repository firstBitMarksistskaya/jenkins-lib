package ru.pulsar.jenkins.library.utils;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class EDTTest {

  @BeforeEach
  void setUp() {
    TestUtils.setupMockedContext();
  }

  @Test
  void testRingModule() throws IOException {

    // given
    String config = IOUtils.resourceToString(
            "jobConfiguration.json",
            StandardCharsets.UTF_8,
            this.getClass().getClassLoader()
    );

    // when
    JobConfiguration jobConfiguration = ConfigurationReader.create(config);
    String edtModule = EDT.ringModule(jobConfiguration);

    // then
    assertThat(edtModule).isEqualTo("edt@2021.3.4:x86_64");
  }

}