package ru.pulsar.jenkins.library.steps;

import hudson.FilePath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;
import ru.pulsar.jenkins.library.configuration.SourceFormat;
import ru.pulsar.jenkins.library.edt.EdtCliEngine;
import ru.pulsar.jenkins.library.edt.EdtCliEngineFactory;
import ru.pulsar.jenkins.library.ioc.ContextRegistry;
import ru.pulsar.jenkins.library.ioc.IContext;
import ru.pulsar.jenkins.library.utils.FileUtils;
import ru.pulsar.jenkins.library.utils.Logger;
import ru.pulsar.jenkins.library.utils.TestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("EdtToDesignerFormatTransformation Tests")
class EdtToDesignerFormatTransformationTest {

    private IStepExecutor steps;
    private IContext context;
    private JobConfiguration config;
    private EdtCliEngine engine;
    private EdtToDesignerFormatTransformation transformation;
    
    @BeforeEach
    void setUp() {
        steps = TestUtils.getMockedStepExecutor();
        context = TestUtils.setupMockedContext(steps);
        
        config = mock(JobConfiguration.class);
        engine = mock(EdtCliEngine.class);
        
        // Setup default environment
        Map<String, Object> env = new HashMap<>();
        env.put("WORKSPACE", "/workspace");
        when(steps.env()).thenReturn(env);
    }
    
    @Test
    @DisplayName("Should skip transformation when source format is not EDT")
    void testSkipTransformationWhenSourceFormatIsNotEDT() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            // Given
            when(config.getSourceFormat()).thenReturn(SourceFormat.DESIGNER);
            transformation = new EdtToDesignerFormatTransformation(config);
            
            // When
            transformation.run();
            
            // Then
            verify(steps, never()).deleteDir(anyString());
            loggerMock.verify(() -> Logger.println("SRC is not in EDT format. No transform is needed."));
        }
    }
    
    @Test
    @DisplayName("Should perform basic transformation without extensions")
    void testBasicTransformationWithoutExtensions() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class);
             MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
             MockedStatic<EdtCliEngineFactory> factoryMock = Mockito.mockStatic(EdtCliEngineFactory.class)) {
            
            // Given
            FilePath filePath = new FilePath(new File("/workspace/build/edt-workspace"));
            fileUtilsMock.when(() -> FileUtils.getFilePath(anyString())).thenReturn(filePath);
            factoryMock.when(() -> EdtCliEngineFactory.getEngine(anyString())).thenReturn(engine);
            
            when(config.getSourceFormat()).thenReturn(SourceFormat.EDT);
            when(config.getEdtVersion()).thenReturn("2023.1");
            when(config.needLoadExtensions()).thenReturn(false);
            
            transformation = new EdtToDesignerFormatTransformation(config);
            
            // When
            transformation.run();
            
            // Then
            verify(steps).deleteDir("/workspace/build/edt-workspace");
            verify(engine).edtToDesignerTransformConfiguration(steps, config);
            verify(steps).zip("build/cfg", "build/cfg.zip");
            verify(steps).stash("cfg-zip", "build/cfg.zip");
            
            // Should not process extensions
            verify(engine, never()).edtToDesignerTransformExtensions(any(), any());
            verify(steps, never()).zip(eq("build/cfe_src"), anyString());
            verify(steps, never()).stash(eq("cfe_src-zip"), anyString());
        }
    }
    
    @Test
    @DisplayName("Should perform full transformation with extensions")
    void testFullTransformationWithExtensions() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class);
             MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
             MockedStatic<EdtCliEngineFactory> factoryMock = Mockito.mockStatic(EdtCliEngineFactory.class)) {
            
            // Given
            FilePath filePath = new FilePath(new File("/workspace/build/edt-workspace"));
            fileUtilsMock.when(() -> FileUtils.getFilePath(anyString())).thenReturn(filePath);
            factoryMock.when(() -> EdtCliEngineFactory.getEngine(anyString())).thenReturn(engine);
            
            when(config.getSourceFormat()).thenReturn(SourceFormat.EDT);
            when(config.getEdtVersion()).thenReturn("2023.1");
            when(config.needLoadExtensions()).thenReturn(true);
            
            transformation = new EdtToDesignerFormatTransformation(config);
            
            // When
            transformation.run();
            
            // Then
            // Verify configuration transformation
            verify(steps).deleteDir("/workspace/build/edt-workspace");
            verify(engine).edtToDesignerTransformConfiguration(steps, config);
            verify(steps).zip("build/cfg", "build/cfg.zip");
            verify(steps).stash("cfg-zip", "build/cfg.zip");
            
            // Verify extension transformation
            verify(engine).edtToDesignerTransformExtensions(steps, config);
            verify(steps).zip("build/cfe_src", "build/cfe_src.zip");
            verify(steps).stash("cfe_src-zip", "build/cfe_src.zip");
        }
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"2021.1", "2022.1", "2023.1", "2024.1"})
    @DisplayName("Should handle different EDT versions")
    void testDifferentEDTVersions(String edtVersion) {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class);
             MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
             MockedStatic<EdtCliEngineFactory> factoryMock = Mockito.mockStatic(EdtCliEngineFactory.class)) {
            
            // Given
            FilePath filePath = new FilePath(new File("/workspace/build/edt-workspace"));
            fileUtilsMock.when(() -> FileUtils.getFilePath(anyString())).thenReturn(filePath);
            factoryMock.when(() -> EdtCliEngineFactory.getEngine(edtVersion)).thenReturn(engine);
            
            when(config.getSourceFormat()).thenReturn(SourceFormat.EDT);
            when(config.getEdtVersion()).thenReturn(edtVersion);
            when(config.needLoadExtensions()).thenReturn(false);
            
            transformation = new EdtToDesignerFormatTransformation(config);
            
            // When
            transformation.run();
            
            // Then
            factoryMock.verify(() -> EdtCliEngineFactory.getEngine(edtVersion));
            verify(engine).edtToDesignerTransformConfiguration(steps, config);
        }
    }
    
    @Test
    @DisplayName("Should handle custom workspace path")
    void testCustomWorkspacePath() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class);
             MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
             MockedStatic<EdtCliEngineFactory> factoryMock = Mockito.mockStatic(EdtCliEngineFactory.class)) {
            
            // Given
            Map<String, Object> customEnv = new HashMap<>();
            customEnv.put("WORKSPACE", "/custom/workspace");
            when(steps.env()).thenReturn(customEnv);
            
            FilePath filePath = new FilePath(new File("/custom/workspace/build/edt-workspace"));
            fileUtilsMock.when(() -> FileUtils.getFilePath(anyString())).thenReturn(filePath);
            factoryMock.when(() -> EdtCliEngineFactory.getEngine(anyString())).thenReturn(engine);
            
            when(config.getSourceFormat()).thenReturn(SourceFormat.EDT);
            when(config.getEdtVersion()).thenReturn("2023.1");
            when(config.needLoadExtensions()).thenReturn(false);
            
            transformation = new EdtToDesignerFormatTransformation(config);
            
            // When
            transformation.run();
            
            // Then
            verify(steps).deleteDir("/custom/workspace/build/edt-workspace");
        }
    }
    
    @Test
    @DisplayName("Should throw NullPointerException for null configuration")
    void testNullConfiguration() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            // Given
            transformation = new EdtToDesignerFormatTransformation(null);
            
            // When/Then
            assertThatThrownBy(() -> transformation.run())
                .isInstanceOf(NullPointerException.class);
        }
    }
    
    @Test
    @DisplayName("Should propagate exception when engine creation fails")
    void testEngineCreationFailure() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class);
             MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
             MockedStatic<EdtCliEngineFactory> factoryMock = Mockito.mockStatic(EdtCliEngineFactory.class)) {
            
            // Given
            FilePath filePath = new FilePath(new File("/workspace/build/edt-workspace"));
            fileUtilsMock.when(() -> FileUtils.getFilePath(anyString())).thenReturn(filePath);
            factoryMock.when(() -> EdtCliEngineFactory.getEngine("invalid-version"))
                .thenThrow(new IllegalArgumentException("Unknown EDT version"));
            
            when(config.getSourceFormat()).thenReturn(SourceFormat.EDT);
            when(config.getEdtVersion()).thenReturn("invalid-version");
            
            transformation = new EdtToDesignerFormatTransformation(config);
            
            // When/Then
            assertThatThrownBy(() -> transformation.run())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown EDT version");
        }
    }
    
    @Test
    @DisplayName("Should propagate exception when transformation fails")
    void testTransformationFailure() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class);
             MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
             MockedStatic<EdtCliEngineFactory> factoryMock = Mockito.mockStatic(EdtCliEngineFactory.class)) {
            
            // Given
            FilePath filePath = new FilePath(new File("/workspace/build/edt-workspace"));
            fileUtilsMock.when(() -> FileUtils.getFilePath(anyString())).thenReturn(filePath);
            factoryMock.when(() -> EdtCliEngineFactory.getEngine(anyString())).thenReturn(engine);
            
            when(config.getSourceFormat()).thenReturn(SourceFormat.EDT);
            when(config.getEdtVersion()).thenReturn("2023.1");
            when(config.needLoadExtensions()).thenReturn(false);
            
            doThrow(new RuntimeException("Transformation failed"))
                .when(engine).edtToDesignerTransformConfiguration(any(), any());
            
            transformation = new EdtToDesignerFormatTransformation(config);
            
            // When/Then
            assertThatThrownBy(() -> transformation.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transformation failed");
        }
    }
    
    @Test
    @DisplayName("Should propagate exception when zip operation fails")
    void testZipOperationFailure() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class);
             MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
             MockedStatic<EdtCliEngineFactory> factoryMock = Mockito.mockStatic(EdtCliEngineFactory.class)) {
            
            // Given
            FilePath filePath = new FilePath(new File("/workspace/build/edt-workspace"));
            fileUtilsMock.when(() -> FileUtils.getFilePath(anyString())).thenReturn(filePath);
            factoryMock.when(() -> EdtCliEngineFactory.getEngine(anyString())).thenReturn(engine);
            
            when(config.getSourceFormat()).thenReturn(SourceFormat.EDT);
            when(config.getEdtVersion()).thenReturn("2023.1");
            when(config.needLoadExtensions()).thenReturn(false);
            
            doThrow(new RuntimeException("Failed to create zip"))
                .when(steps).zip(anyString(), anyString());
            
            transformation = new EdtToDesignerFormatTransformation(config);
            
            // When/Then
            assertThatThrownBy(() -> transformation.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create zip");
        }
    }
    
    @Test
    @DisplayName("Should verify constant values are correctly defined")
    void testConstantValues() {
        assertThat(EdtToDesignerFormatTransformation.WORKSPACE).isEqualTo("build/edt-workspace");
        assertThat(EdtToDesignerFormatTransformation.CONFIGURATION_DIR).isEqualTo("build/cfg");
        assertThat(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP).isEqualTo("build/cfg.zip");
        assertThat(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH).isEqualTo("cfg-zip");
        assertThat(EdtToDesignerFormatTransformation.EXTENSION_DIR).isEqualTo("build/cfe_src");
        assertThat(EdtToDesignerFormatTransformation.EXTENSION_ZIP).isEqualTo("build/cfe_src.zip");
        assertThat(EdtToDesignerFormatTransformation.EXTENSION_ZIP_STASH).isEqualTo("cfe_src-zip");
    }
    
    @Test
    @DisplayName("Should execute operations in correct order")
    void testOperationOrder() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class);
             MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
             MockedStatic<EdtCliEngineFactory> factoryMock = Mockito.mockStatic(EdtCliEngineFactory.class)) {
            
            // Given
            FilePath filePath = new FilePath(new File("/workspace/build/edt-workspace"));
            fileUtilsMock.when(() -> FileUtils.getFilePath(anyString())).thenReturn(filePath);
            factoryMock.when(() -> EdtCliEngineFactory.getEngine(anyString())).thenReturn(engine);
            
            when(config.getSourceFormat()).thenReturn(SourceFormat.EDT);
            when(config.getEdtVersion()).thenReturn("2023.1");
            when(config.needLoadExtensions()).thenReturn(true);
            
            List<String> operationOrder = new ArrayList<>();
            
            // Track operation order
            doAnswer(invocation -> {
                operationOrder.add("deleteDir");
                return null;
            }).when(steps).deleteDir(anyString());
            
            doAnswer(invocation -> {
                operationOrder.add("transformConfig");
                return null;
            }).when(engine).edtToDesignerTransformConfiguration(any(), any());
            
            doAnswer(invocation -> {
                String source = invocation.getArgument(0);
                if ("build/cfg".equals(source)) {
                    operationOrder.add("zipConfig");
                } else if ("build/cfe_src".equals(source)) {
                    operationOrder.add("zipExt");
                }
                return null;
            }).when(steps).zip(anyString(), anyString());
            
            doAnswer(invocation -> {
                String name = invocation.getArgument(0);
                if ("cfg-zip".equals(name)) {
                    operationOrder.add("stashConfig");
                } else if ("cfe_src-zip".equals(name)) {
                    operationOrder.add("stashExt");
                }
                return null;
            }).when(steps).stash(anyString(), anyString());
            
            doAnswer(invocation -> {
                operationOrder.add("transformExt");
                return null;
            }).when(engine).edtToDesignerTransformExtensions(any(), any());
            
            transformation = new EdtToDesignerFormatTransformation(config);
            
            // When
            transformation.run();
            
            // Then
            assertThat(operationOrder).containsExactly(
                "deleteDir", "transformConfig", "zipConfig", "stashConfig",
                "transformExt", "zipExt", "stashExt"
            );
        }
    }
    
    @Test
    @DisplayName("Should handle empty workspace environment variable")
    void testEmptyWorkspaceEnvironmentVariable() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class);
             MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
             MockedStatic<EdtCliEngineFactory> factoryMock = Mockito.mockStatic(EdtCliEngineFactory.class)) {
            
            // Given
            Map<String, Object> emptyEnv = new HashMap<>();
            emptyEnv.put("WORKSPACE", "");
            when(steps.env()).thenReturn(emptyEnv);
            
            FilePath filePath = new FilePath(new File("/build/edt-workspace"));
            fileUtilsMock.when(() -> FileUtils.getFilePath(anyString())).thenReturn(filePath);
            factoryMock.when(() -> EdtCliEngineFactory.getEngine(anyString())).thenReturn(engine);
            
            when(config.getSourceFormat()).thenReturn(SourceFormat.EDT);
            when(config.getEdtVersion()).thenReturn("2023.1");
            when(config.needLoadExtensions()).thenReturn(false);
            
            transformation = new EdtToDesignerFormatTransformation(config);
            
            // When
            transformation.run();
            
            // Then
            verify(steps).deleteDir("/build/edt-workspace");
            verify(engine).edtToDesignerTransformConfiguration(steps, config);
        }
    }
    
    @ParameterizedTest
    @EnumSource(SourceFormat.class)
    @DisplayName("Should handle different source formats correctly")
    void testDifferentSourceFormats(SourceFormat format) {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class);
             MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
             MockedStatic<EdtCliEngineFactory> factoryMock = Mockito.mockStatic(EdtCliEngineFactory.class)) {
            
            // Given
            if (format == SourceFormat.EDT) {
                FilePath filePath = new FilePath(new File("/workspace/build/edt-workspace"));
                fileUtilsMock.when(() -> FileUtils.getFilePath(anyString())).thenReturn(filePath);
                factoryMock.when(() -> EdtCliEngineFactory.getEngine(anyString())).thenReturn(engine);
            }
            
            when(config.getSourceFormat()).thenReturn(format);
            when(config.getEdtVersion()).thenReturn("2023.1");
            when(config.needLoadExtensions()).thenReturn(false);
            
            transformation = new EdtToDesignerFormatTransformation(config);
            
            // When
            transformation.run();
            
            // Then
            if (format == SourceFormat.EDT) {
                verify(engine).edtToDesignerTransformConfiguration(steps, config);
            } else {
                verify(engine, never()).edtToDesignerTransformConfiguration(any(), any());
            }
        }
    }
    
    @Test
    @DisplayName("Should handle extension transformation failure separately")
    void testExtensionTransformationFailure() {
        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class);
             MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
             MockedStatic<EdtCliEngineFactory> factoryMock = Mockito.mockStatic(EdtCliEngineFactory.class)) {
            
            // Given
            FilePath filePath = new FilePath(new File("/workspace/build/edt-workspace"));
            fileUtilsMock.when(() -> FileUtils.getFilePath(anyString())).thenReturn(filePath);
            factoryMock.when(() -> EdtCliEngineFactory.getEngine(anyString())).thenReturn(engine);
            
            when(config.getSourceFormat()).thenReturn(SourceFormat.EDT);
            when(config.getEdtVersion()).thenReturn("2023.1");
            when(config.needLoadExtensions()).thenReturn(true);
            
            doThrow(new RuntimeException("Extension transformation failed"))
                .when(engine).edtToDesignerTransformExtensions(any(), any());
            
            transformation = new EdtToDesignerFormatTransformation(config);
            
            // When/Then
            assertThatThrownBy(() -> transformation.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Extension transformation failed");
            
            // Configuration should have been transformed before failure
            verify(engine).edtToDesignerTransformConfiguration(steps, config);
            verify(steps).zip("build/cfg", "build/cfg.zip");
            verify(steps).stash("cfg-zip", "build/cfg.zip");
        }
    }
}