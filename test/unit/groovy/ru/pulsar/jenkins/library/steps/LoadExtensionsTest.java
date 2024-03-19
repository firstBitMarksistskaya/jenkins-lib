package ru.pulsar.jenkins.library.steps;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.utils.TestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;


class LoadExtensionsTest {

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext();
    }

    @Test
    void runYaxunit() throws IOException {

        // given
        String config = IOUtils.resourceToString(
                "jobConfiguration.json",
                StandardCharsets.UTF_8,
                this.getClass().getClassLoader()
        );

        // when
        JobConfiguration jobConfiguration = ConfigurationReader.create(config);

        LoadExtensions loadExtensions = new LoadExtensions(jobConfiguration, "yaxunit");

        // when
        Object run = loadExtensions.run();

        // assertThat(log.toString).(1);
    }
}
