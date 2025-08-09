package ru.pulsar.jenkins.library.steps

import spock.lang.Specification
import spock.lang.Unroll
import ru.pulsar.jenkins.library.edt.EdtCliEngineFactory
import ru.pulsar.jenkins.library.edt.EdtCliEngine
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.ioc.IContext
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger

/**
 * Unit tests for EdtToDesignerFormatTransformation class.
 * Tests the transformation logic from EDT format to Designer format.
 * 
 * Testing framework: Spock Framework 1.3-groovy-2.4
 * Testing approach: Unit testing with mocks for external dependencies
 */
class EdtToDesignerFormatTransformationSpec extends Specification {

    // Test fixtures and mocks
    def stepExecutor = Mock(IStepExecutor)
    def context = Mock(IContext)
    def config = Mock(JobConfiguration)
    def engine = Mock(EdtCliEngine)
    def env = [WORKSPACE: '/test/workspace']
    
    // Store original metaClasses for cleanup
    def originalContextRegistryMetaClass
    def originalLoggerMetaClass
    def originalFileUtilsMetaClass
    def originalEdtCliEngineFactoryMetaClass
    
    def setup() {
        // Store original metaClasses to restore them later
        originalContextRegistryMetaClass = ContextRegistry.metaClass
        originalLoggerMetaClass = Logger.metaClass
        originalFileUtilsMetaClass = FileUtils.metaClass
        originalEdtCliEngineFactoryMetaClass = EdtCliEngineFactory.metaClass
        
        // Mock static methods using Groovy metaClass
        ContextRegistry.metaClass.static.getContext = { -> context }
        context.getStepExecutor() >> stepExecutor
        stepExecutor.env() >> env
        
        Logger.metaClass.static.printLocation = { -> }
        Logger.metaClass.static.println = { String msg -> }
        
        FileUtils.metaClass.static.getFilePath = { String path -> 
            [getRemote: { -> path }] as File
        }
        
        EdtCliEngineFactory.metaClass.static.getEngine = { String version -> engine }
    }
    
    def cleanup() {
        // Restore original metaClasses to prevent test pollution
        ContextRegistry.metaClass = originalContextRegistryMetaClass
        Logger.metaClass = originalLoggerMetaClass
        FileUtils.metaClass = originalFileUtilsMetaClass
        EdtCliEngineFactory.metaClass = originalEdtCliEngineFactoryMetaClass
    }

    // ===== Happy Path Tests =====
    
    def "should skip transformation when source format is not EDT"() {
        given: "Configuration with non-EDT source format (DESIGNER)"
        config.sourceFormat >> SourceFormat.DESIGNER
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "No transformation operations are performed"
        0 * stepExecutor.deleteDir(_)
        0 * engine.edtToDesignerTransformConfiguration(_, _)
        0 * engine.edtToDesignerTransformExtensions(_, _)
        0 * stepExecutor.zip(_, _)
        0 * stepExecutor.stash(_, _)
    }
    
    def "should perform only configuration transformation when source format is EDT without extensions"() {
        given: "Configuration with EDT source format and no extensions needed"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "Workspace directory is cleaned"
        1 * stepExecutor.deleteDir("/test/workspace/build/edt-workspace")
        
        then: "Configuration transformation is performed"
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
        
        then: "Configuration is zipped and stashed"
        1 * stepExecutor.zip(EdtToDesignerFormatTransformation.CONFIGURATION_DIR, 
                            EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
        1 * stepExecutor.stash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH, 
                              EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
        
        and: "Extension transformation is not performed"
        0 * engine.edtToDesignerTransformExtensions(_, _)
        0 * stepExecutor.zip(EdtToDesignerFormatTransformation.EXTENSION_DIR, _)
        0 * stepExecutor.stash(EdtToDesignerFormatTransformation.EXTENSION_ZIP_STASH, _)
    }
    
    def "should perform both configuration and extension transformation when extensions are needed"() {
        given: "Configuration with EDT source format and extensions needed"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> true
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "Workspace directory is cleaned"
        1 * stepExecutor.deleteDir("/test/workspace/build/edt-workspace")
        
        then: "Configuration transformation is performed"
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
        1 * stepExecutor.zip(EdtToDesignerFormatTransformation.CONFIGURATION_DIR, 
                            EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
        1 * stepExecutor.stash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH, 
                              EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
        
        then: "Extension transformation is also performed"
        1 * engine.edtToDesignerTransformExtensions(stepExecutor, config)
        1 * stepExecutor.zip(EdtToDesignerFormatTransformation.EXTENSION_DIR, 
                            EdtToDesignerFormatTransformation.EXTENSION_ZIP)
        1 * stepExecutor.stash(EdtToDesignerFormatTransformation.EXTENSION_ZIP_STASH, 
                              EdtToDesignerFormatTransformation.EXTENSION_ZIP)
    }
    
    // ===== Edge Cases and Different Versions =====
    
    @Unroll
    def "should handle different EDT versions correctly: #edtVersion"() {
        given: "Configuration with specific EDT version"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> edtVersion
        config.needLoadExtensions() >> false
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "Correct engine is retrieved for the version"
        1 * EdtCliEngineFactory.getEngine(edtVersion) >> engine
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
        
        where:
        edtVersion << ["2021.3", "2022.1", "2022.2", "2023.1", "2023.2", "2024.1", "latest", ""]
    }
    
    def "should handle workspace path with special characters"() {
        given: "Configuration with EDT source format and workspace with special characters"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        env.WORKSPACE = "/test/work space/@special#chars&symbols"
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "Workspace directory path is correctly constructed"
        1 * stepExecutor.deleteDir("/test/work space/@special#chars&symbols/build/edt-workspace")
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
    }
    
    @Unroll
    def "should handle different source formats correctly: #format"() {
        given: "Configuration with specific source format"
        config.sourceFormat >> format
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "Transformation is only performed for EDT format"
        if (format == SourceFormat.EDT) {
            1 * config.edtVersion >> "2023.1"
            1 * config.needLoadExtensions() >> false
            1 * stepExecutor.deleteDir(_)
            1 * engine.edtToDesignerTransformConfiguration(_, _)
            1 * stepExecutor.zip(_, _)
            1 * stepExecutor.stash(_, _)
        } else {
            0 * stepExecutor.deleteDir(_)
            0 * engine._
            0 * stepExecutor.zip(_, _)
            0 * stepExecutor.stash(_, _)
        }
        
        where:
        format << [SourceFormat.EDT, SourceFormat.DESIGNER, SourceFormat.XML]
    }
    
    // ===== Failure Conditions =====
    
    def "should propagate exception when configuration transformation fails"() {
        given: "Configuration with EDT source format"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        and: "Engine throws exception during transformation"
        def expectedException = new RuntimeException("Transformation failed")
        engine.edtToDesignerTransformConfiguration(stepExecutor, config) >> { 
            throw expectedException
        }
        
        when: "Running transformation"
        transformation.run()
        
        then: "Exception is propagated"
        def thrownException = thrown(RuntimeException)
        thrownException.message == "Transformation failed"
        
        and: "Cleanup was attempted but zip and stash were not called"
        1 * stepExecutor.deleteDir(_)
        0 * stepExecutor.zip(_, _)
        0 * stepExecutor.stash(_, _)
    }
    
    def "should propagate exception when extension transformation fails"() {
        given: "Configuration with EDT source format and extensions needed"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> true
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        and: "Engine throws exception during extension transformation"
        engine.edtToDesignerTransformExtensions(stepExecutor, config) >> { 
            throw new RuntimeException("Extension transformation failed") 
        }
        
        when: "Running transformation"
        transformation.run()
        
        then: "Configuration transformation completes successfully"
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
        1 * stepExecutor.zip(EdtToDesignerFormatTransformation.CONFIGURATION_DIR, 
                            EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
        1 * stepExecutor.stash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH, 
                              EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
        
        then: "Exception is thrown during extension transformation"
        thrown(RuntimeException)
    }
    
    def "should handle null configuration"() {
        given: "Null configuration"
        def transformation = new EdtToDesignerFormatTransformation(null)
        
        when: "Running transformation"
        transformation.run()
        
        then: "NullPointerException is thrown when accessing config"
        thrown(NullPointerException)
    }
    
    def "should handle missing WORKSPACE environment variable"() {
        given: "Configuration with EDT source format but missing WORKSPACE env var"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        stepExecutor.env() >> [:]  // Empty environment map
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "Transformation proceeds with null workspace"
        1 * stepExecutor.deleteDir("null/build/edt-workspace")
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
    }
    
    def "should propagate IOException when zip operation fails for configuration"() {
        given: "Configuration with EDT source format"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        and: "Zip operation fails"
        stepExecutor.zip(EdtToDesignerFormatTransformation.CONFIGURATION_DIR, 
                        EdtToDesignerFormatTransformation.CONFIGURATION_ZIP) >> {
            throw new IOException("Failed to create zip")
        }
        
        when: "Running transformation"
        transformation.run()
        
        then: "IOException is propagated"
        thrown(IOException)
        
        and: "Stash is not called"
        0 * stepExecutor.stash(_, _)
    }
    
    def "should propagate IOException when zip operation fails for extensions"() {
        given: "Configuration with EDT source format and extensions"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> true
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        and: "Extension zip operation fails"
        stepExecutor.zip(EdtToDesignerFormatTransformation.EXTENSION_DIR, 
                        EdtToDesignerFormatTransformation.EXTENSION_ZIP) >> {
            throw new IOException("Failed to create extension zip")
        }
        
        when: "Running transformation"
        transformation.run()
        
        then: "Configuration completes successfully"
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
        1 * stepExecutor.zip(EdtToDesignerFormatTransformation.CONFIGURATION_DIR, _)
        1 * stepExecutor.stash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH, _)
        
        then: "IOException is thrown for extension zip"
        thrown(IOException)
    }
    
    def "should propagate exception when stash operation fails for configuration"() {
        given: "Configuration with EDT source format"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        and: "Stash operation fails"
        stepExecutor.stash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH, 
                          EdtToDesignerFormatTransformation.CONFIGURATION_ZIP) >> {
            throw new RuntimeException("Failed to stash")
        }
        
        when: "Running transformation"
        transformation.run()
        
        then: "RuntimeException is propagated"
        thrown(RuntimeException)
    }
    
    def "should propagate exception when stash operation fails for extensions"() {
        given: "Configuration with EDT source format and extensions"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> true
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        and: "Extension stash operation fails"
        stepExecutor.stash(EdtToDesignerFormatTransformation.EXTENSION_ZIP_STASH, 
                          EdtToDesignerFormatTransformation.EXTENSION_ZIP) >> {
            throw new RuntimeException("Failed to stash extensions")
        }
        
        when: "Running transformation"
        transformation.run()
        
        then: "Configuration completes successfully"
        1 * stepExecutor.stash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH, _)
        
        then: "RuntimeException is thrown for extension stash"
        thrown(RuntimeException)
    }
    
    def "should propagate exception when deleteDir fails"() {
        given: "Configuration with EDT source format"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        and: "deleteDir throws exception"
        stepExecutor.deleteDir(_) >> { 
            throw new IOException("Cannot delete directory") 
        }
        
        when: "Running transformation"
        transformation.run()
        
        then: "IOException is propagated"
        thrown(IOException)
        
        and: "Subsequent operations are not performed"
        0 * engine.edtToDesignerTransformConfiguration(_, _)
        0 * stepExecutor.zip(_, _)
        0 * stepExecutor.stash(_, _)
    }
    
    // ===== Validation Tests =====
    
    def "should verify all constant values are correct"() {
        expect: "All constants have expected values"
        EdtToDesignerFormatTransformation.WORKSPACE == 'build/edt-workspace'
        EdtToDesignerFormatTransformation.CONFIGURATION_DIR == 'build/cfg'
        EdtToDesignerFormatTransformation.CONFIGURATION_ZIP == 'build/cfg.zip'
        EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH == 'cfg-zip'
        EdtToDesignerFormatTransformation.EXTENSION_DIR == 'build/cfe_src'
        EdtToDesignerFormatTransformation.EXTENSION_ZIP == 'build/cfe_src.zip'
        EdtToDesignerFormatTransformation.EXTENSION_ZIP_STASH == 'cfe_src-zip'
    }
    
    def "should ensure proper order of operations for full transformation"() {
        given: "Configuration with EDT source format and extensions"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> true
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        def operationOrder = []
        
        // Track operation order
        stepExecutor.deleteDir(_) >> { operationOrder << "deleteDir"; null }
        engine.edtToDesignerTransformConfiguration(_, _) >> { operationOrder << "transformConfig"; null }
        stepExecutor.zip(EdtToDesignerFormatTransformation.CONFIGURATION_DIR, _) >> { operationOrder << "zipConfig"; null }
        stepExecutor.stash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH, _) >> { operationOrder << "stashConfig"; null }
        engine.edtToDesignerTransformExtensions(_, _) >> { operationOrder << "transformExt"; null }
        stepExecutor.zip(EdtToDesignerFormatTransformation.EXTENSION_DIR, _) >> { operationOrder << "zipExt"; null }
        stepExecutor.stash(EdtToDesignerFormatTransformation.EXTENSION_ZIP_STASH, _) >> { operationOrder << "stashExt"; null }
        
        when: "Running transformation"
        transformation.run()
        
        then: "Operations are executed in correct order"
        operationOrder == ["deleteDir", "transformConfig", "zipConfig", "stashConfig", 
                          "transformExt", "zipExt", "stashExt"]
    }
    
    def "should handle null return from ContextRegistry.getContext()"() {
        given: "Configuration with EDT source format"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        
        and: "Context registry returns null"
        ContextRegistry.metaClass.static.getContext = { -> null }
        
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "NullPointerException is thrown"
        thrown(NullPointerException)
    }
    
    def "should handle null return from FileUtils.getFilePath()"() {
        given: "Configuration with EDT source format"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        
        and: "FileUtils returns null"
        FileUtils.metaClass.static.getFilePath = { String path -> null }
        
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "NullPointerException is thrown"
        thrown(NullPointerException)
    }
    
    def "should handle null engine from EdtCliEngineFactory"() {
        given: "Configuration with EDT source format"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        
        and: "Factory returns null engine"
        EdtCliEngineFactory.metaClass.static.getEngine = { String version -> null }
        
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "NullPointerException is thrown when trying to use null engine"
        thrown(NullPointerException)
    }
    
    // ===== Boundary Tests =====
    
    def "should handle empty EDT version string"() {
        given: "Configuration with empty EDT version"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> ""
        config.needLoadExtensions() >> false
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "Engine is retrieved with empty version"
        1 * EdtCliEngineFactory.getEngine("") >> engine
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
    }
    
    def "should handle null EDT version"() {
        given: "Configuration with null EDT version"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> null
        config.needLoadExtensions() >> false
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "Engine is retrieved with null version"
        1 * EdtCliEngineFactory.getEngine(null) >> engine
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
    }
    
    def "should handle very long workspace path"() {
        given: "Configuration with very long workspace path"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        def longPath = "/" + ("a" * 250) + "/workspace"
        env.WORKSPACE = longPath
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "Long path is handled correctly"
        1 * stepExecutor.deleteDir(longPath + "/build/edt-workspace")
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
    }
    
    // ===== Integration-like Unit Tests =====
    
    def "should complete full transformation workflow successfully"() {
        given: "Fully configured transformation with all features enabled"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> true
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running complete transformation"
        transformation.run()
        
        then: "All operations complete successfully"
        1 * stepExecutor.deleteDir(_)
        
        then: "Configuration transformation"
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
        1 * stepExecutor.zip(EdtToDesignerFormatTransformation.CONFIGURATION_DIR, 
                            EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
        1 * stepExecutor.stash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH, 
                              EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
        
        then: "Extension transformation"
        1 * engine.edtToDesignerTransformExtensions(stepExecutor, config)
        1 * stepExecutor.zip(EdtToDesignerFormatTransformation.EXTENSION_DIR, 
                            EdtToDesignerFormatTransformation.EXTENSION_ZIP)
        1 * stepExecutor.stash(EdtToDesignerFormatTransformation.EXTENSION_ZIP_STASH, 
                              EdtToDesignerFormatTransformation.EXTENSION_ZIP)
        
        and: "No exceptions thrown"
        noExceptionThrown()
    }
    
    def "should handle configuration-only transformation workflow"() {
        given: "Configuration for minimal transformation"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2022.2"
        config.needLoadExtensions() >> false
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running configuration-only transformation"
        transformation.run()
        
        then: "Only configuration operations are performed"
        1 * stepExecutor.deleteDir(_)
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
        1 * stepExecutor.zip(EdtToDesignerFormatTransformation.CONFIGURATION_DIR, 
                            EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
        1 * stepExecutor.stash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH, 
                              EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
        
        and: "Extension operations are skipped"
        0 * engine.edtToDesignerTransformExtensions(_, _)
        0 * stepExecutor.zip(EdtToDesignerFormatTransformation.EXTENSION_DIR, _)
        0 * stepExecutor.stash(EdtToDesignerFormatTransformation.EXTENSION_ZIP_STASH, _)
    }
    
    def "should validate that Logger.printLocation is called"() {
        given: "Configuration with EDT source format"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        
        def locationPrinted = false
        Logger.metaClass.static.printLocation = { -> locationPrinted = true }
        
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "Logger.printLocation was called"
        locationPrinted == true
        
        and: "Transformation completes"
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
    }
    
    def "should log message when source format is not EDT"() {
        given: "Configuration with non-EDT source format"
        config.sourceFormat >> SourceFormat.XML
        
        def loggedMessage = null
        Logger.metaClass.static.println = { String msg -> loggedMessage = msg }
        
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "Appropriate message is logged"
        loggedMessage == "SRC is not in EDT format. No transform is needed."
        
        and: "No transformation operations are performed"
        0 * stepExecutor._
        0 * engine._
    }
    
    def "should handle multiple consecutive runs of the same transformation"() {
        given: "Configuration with EDT source format"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation twice"
        transformation.run()
        transformation.run()
        
        then: "Operations are performed twice"
        2 * stepExecutor.deleteDir(_)
        2 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
        2 * stepExecutor.zip(EdtToDesignerFormatTransformation.CONFIGURATION_DIR, 
                            EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
        2 * stepExecutor.stash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH, 
                              EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
    }
    
    def "should handle workspace path with unicode characters"() {
        given: "Configuration with unicode characters in workspace path"
        config.sourceFormat >> SourceFormat.EDT
        config.edtVersion >> "2023.1"
        config.needLoadExtensions() >> false
        env.WORKSPACE = "/test/работа/项目/مشروع"
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        when: "Running transformation"
        transformation.run()
        
        then: "Unicode path is handled correctly"
        1 * stepExecutor.deleteDir("/test/работа/项目/مشروع/build/edt-workspace")
        1 * engine.edtToDesignerTransformConfiguration(stepExecutor, config)
    }
    
    def "should verify transformation is serializable"() {
        given: "Configuration with EDT source format"
        config.sourceFormat >> SourceFormat.EDT
        def transformation = new EdtToDesignerFormatTransformation(config)
        
        expect: "Class implements Serializable"
        transformation instanceof Serializable
    }
}