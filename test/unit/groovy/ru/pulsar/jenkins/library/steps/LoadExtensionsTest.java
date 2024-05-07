package ru.pulsar.jenkins.library.steps;

import hudson.FilePath;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.InitInfoBaseOptions;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.utils.FileUtils;
import ru.pulsar.jenkins.library.utils.TestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

class LoadExtensionsTest {

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext();
    }

    @Test
    void runYaxunit() throws IOException {

        try (MockedStatic<FileUtils> fu = Mockito.mockStatic(FileUtils.class)) {
            fu.when(() -> FileUtils.getFilePath(anyString()))
                    .thenReturn(new FilePath(new File("/")));

            // given
            // файл содержит 4 расширения для разных стейджей
            String config = IOUtils.resourceToString(
                    "jobConfiguration.json",
                    StandardCharsets.UTF_8,
                    this.getClass().getClassLoader()
            );
            JobConfiguration jobConfiguration = ConfigurationReader.create(config);

            // when
            LoadExtensions loadExtensions = new LoadExtensions(jobConfiguration);
            loadExtensions.run();

            // then
            InitInfoBaseOptions.Extension[] extensions = loadExtensions.getExtensionsFiltered();
            assertThat(extensions.length).isEqualTo(2);
            assertThat(extensions[0].getName()).isEqualTo("mods");
            assertThat(extensions[1].getName()).isEqualTo("mods2");

            // when
            LoadExtensions loadExtensionsWithStage = new LoadExtensions(jobConfiguration, "yaxunit");
            loadExtensionsWithStage.run();

            // then
            extensions = loadExtensionsWithStage.getExtensionsFiltered();
            assertThat(extensions.length).isEqualTo(1);
            assertThat(extensions[0].getName()).isEqualTo("YAXUnit");
        }
    }
}
