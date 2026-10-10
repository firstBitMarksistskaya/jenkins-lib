package ru.pulsar.jenkins.library.steps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.ConfigurationReader;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.edt.EdtCliEngineFactory;
import ru.pulsar.jenkins.library.edt.IEdtCliEngine;
import ru.pulsar.jenkins.library.utils.TestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.pulsar.jenkins.library.steps.EdtToDesignerFormatTransformation.CONFIGURATION_ZIP;
import static ru.pulsar.jenkins.library.steps.EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH;
import static ru.pulsar.jenkins.library.steps.EdtToDesignerFormatTransformation.EXTENSION_DIR;
import static ru.pulsar.jenkins.library.steps.EdtToDesignerFormatTransformation.EXTENSION_ZIP;
import static ru.pulsar.jenkins.library.steps.EdtToDesignerFormatTransformation.EXTENSION_ZIP_STASH;

class EdtToDesignerFormatTransformationTest {

    private static final String EXTENSIONS = """
        [
          {
            "name": "ext",
            "initMethod": "fromSource",
            "path": "src/cfe/ext"
          }
        ]
        """;

    private static final String EXTENSIONS_FROM_FILES_ONLY = """
        [
          {
            "name": "YAXUnit",
            "initMethod": "fromFile",
            "path": "https://example.com/YAXUNIT.cfe"
          }
        ]
        """;

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();
    private final IEdtCliEngine engine = mock(IEdtCliEngine.class);

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void runTransformsOnlyExtensionsWhenConfigurationIsLoadedFromStorage() {
        // given
        JobConfiguration jobConfiguration = createConfiguration("fromStorage", EXTENSIONS);

        // when
        run(jobConfiguration);

        // then
        verifyConfigurationIsNotTransformed();
        verifyExtensionsAreTransformed(jobConfiguration);
    }

    @Test
    void runTransformsOnlyExtensionsForNotInit() {
        // given
        JobConfiguration jobConfiguration = createConfiguration("notInit", EXTENSIONS);

        // when
        run(jobConfiguration);

        // then
        verifyConfigurationIsNotTransformed();
        verifyExtensionsAreTransformed(jobConfiguration);
    }

    @Test
    void runTransformsConfigurationAndExtensionsWhenConfigurationIsLoadedFromSource() {
        // given
        JobConfiguration jobConfiguration = createConfiguration("fromSource", EXTENSIONS);

        // when
        run(jobConfiguration);

        // then
        verify(engine).edtToDesignerTransformConfiguration(steps, jobConfiguration);
        verify(steps).stash(CONFIGURATION_ZIP_STASH, CONFIGURATION_ZIP);
        verifyExtensionsAreTransformed(jobConfiguration);
    }

    @Test
    void runCreatesExtensionArchiveForExtensionsFromFilesOnly() {
        // given
        // расширения из cfe-файлов не конвертируются, но GetExtensions для формата EDT распаковывает архив всегда
        JobConfiguration jobConfiguration = createConfiguration("fromStorage", EXTENSIONS_FROM_FILES_ONLY);

        // when
        run(jobConfiguration);

        // then
        verifyConfigurationIsNotTransformed();
        verifyExtensionsAreTransformed(jobConfiguration);
    }

    @Test
    void runTransformsOnlyConfigurationWithoutExtensions() {
        // given
        JobConfiguration jobConfiguration = createConfiguration("fromSource", "[]");

        // when
        run(jobConfiguration);

        // then
        verify(engine).edtToDesignerTransformConfiguration(steps, jobConfiguration);
        verify(steps).stash(CONFIGURATION_ZIP_STASH, CONFIGURATION_ZIP);
        verify(steps, never()).createDir(eq(EXTENSION_DIR), anyBoolean());
        verify(engine, never()).edtToDesignerTransformExtensions(any(), any());
        verify(steps, never()).stash(eq(EXTENSION_ZIP_STASH), anyString());
    }

    private void run(JobConfiguration jobConfiguration) {
        try (MockedStatic<EdtCliEngineFactory> factory = Mockito.mockStatic(EdtCliEngineFactory.class)) {
            factory.when(() -> EdtCliEngineFactory.getEngine(anyString())).thenReturn(engine);

            new EdtToDesignerFormatTransformation(jobConfiguration).run();
        }
    }

    private void verifyConfigurationIsNotTransformed() {
        verify(engine, never()).edtToDesignerTransformConfiguration(any(), any());
        verify(steps, never()).stash(eq(CONFIGURATION_ZIP_STASH), anyString());
    }

    private void verifyExtensionsAreTransformed(JobConfiguration jobConfiguration) {
        // каталог пересоздается до конвертации, поэтому архив есть, даже если конвертировать нечего
        InOrder inOrder = Mockito.inOrder(steps, engine);
        inOrder.verify(steps).createDir(EXTENSION_DIR, true);
        inOrder.verify(engine).edtToDesignerTransformExtensions(steps, jobConfiguration);
        inOrder.verify(steps).zip(EXTENSION_DIR, EXTENSION_ZIP);
        inOrder.verify(steps).stash(EXTENSION_ZIP_STASH, EXTENSION_ZIP);
    }

    private static JobConfiguration createConfiguration(String initMethod, String extensions) {
        String config = """
            {
              "edtVersion": "2024.1.0",
              "sourceFormat": "edt",
              "initInfobase": {
                "initMethod": "%s",
                "templateDBPath": "template.dt",
                "extensions": %s
              }
            }
            """.formatted(initMethod, extensions);

        return ConfigurationReader.create(config);
    }
}
