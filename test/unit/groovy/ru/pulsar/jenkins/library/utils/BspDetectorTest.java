package ru.pulsar.jenkins.library.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.configuration.SonarQubeOptions;
import ru.pulsar.jenkins.library.configuration.SourceFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BspDetectorTest {

    private IStepExecutor steps;

    @BeforeEach
    void setUp() {
        steps = TestUtils.getMockedStepExecutor();
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void isBspConfiguration_checks_designer_module_path() {

        // given
        JobConfiguration config = createConfig("src/cf", SourceFormat.DESIGNER, "");
        String expectedPath = "src/cf/CommonModules/"
                + BspDetector.DEFAULT_INFO_BASE_UPDATE_MODULE_NAME
                + "/Ext/Module.bsl";
        when(steps.fileExists(expectedPath)).thenReturn(true);

        // when
        boolean result = BspDetector.isBspConfiguration(config);

        // then
        assertThat(result).isTrue();
        verify(steps).fileExists(expectedPath);
    }

    @Test
    void isBspConfiguration_checks_edt_module_path() {

        // given
        JobConfiguration config = createConfig("src/cf", SourceFormat.EDT, "");
        String expectedPath = "src/cf/src/CommonModules/"
                + BspDetector.DEFAULT_INFO_BASE_UPDATE_MODULE_NAME
                + "/Module.bsl";
        when(steps.fileExists(expectedPath)).thenReturn(true);

        // when
        boolean result = BspDetector.isBspConfiguration(config);

        // then
        assertThat(result).isTrue();
        verify(steps).fileExists(expectedPath);
    }

    @Test
    void isBspConfiguration_uses_custom_module_name_and_trims_src_dir() {

        // given
        JobConfiguration config = createConfig(" src\\cf ", SourceFormat.DESIGNER, "InfoBaseUpdateModule");
        String expectedPath = "src/cf/CommonModules/InfoBaseUpdateModule/Ext/Module.bsl";
        when(steps.fileExists(expectedPath)).thenReturn(true);

        // when
        boolean result = BspDetector.isBspConfiguration(config);

        // then
        assertThat(result).isTrue();
        verify(steps).fileExists(expectedPath);
    }

    @Test
    void isBspConfiguration_returns_false_for_blank_src_dir() {

        // given
        JobConfiguration config = createConfig("   ", SourceFormat.DESIGNER, "");

        // when
        boolean result = BspDetector.isBspConfiguration(config);

        // then
        assertThat(result).isFalse();
    }

    private static JobConfiguration createConfig(String srcDir, SourceFormat sourceFormat, String moduleName) {
        SonarQubeOptions sonarQubeOptions = new SonarQubeOptions();
        sonarQubeOptions.setInfoBaseUpdateModuleName(moduleName);

        JobConfiguration config = new JobConfiguration();
        config.setSrcDir(srcDir);
        config.setSourceFormat(sourceFormat);
        config.setSonarQubeOptions(sonarQubeOptions);
        return config;
    }
}
