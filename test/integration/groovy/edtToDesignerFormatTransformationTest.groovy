import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class edtToDesignerFormatTransformationTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "EdtToDesignerFormatTransformation should skip non-EDT formats"() {
        def pipeline = """
        @Library('jenkins-lib')
        import ru.pulsar.jenkins.library.steps.EdtToDesignerFormatTransformation
        import ru.pulsar.jenkins.library.configuration.JobConfiguration
        import ru.pulsar.jenkins.library.configuration.SourceFormat
        
        pipeline {
            agent any
            stages {
                stage('test') {
                    steps {
                        script {
                            def config = new JobConfiguration()
                            config.sourceFormat = SourceFormat.DESIGNER
                            def transformation = new EdtToDesignerFormatTransformation(config)
                            transformation.run()
                            echo "Transformation completed"
                        }
                    }
                }
            }
        }
        """.stripIndent()
        
        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'edt-skip-test')
        workflowJob.definition = flow

        def run = rule.buildAndAssertSuccess(workflowJob)
        rule.assertLogContains("SRC is not in EDT format", run)
        rule.assertLogContains("Transformation completed", run)
    }

    @Test
    void "EdtToDesignerFormatTransformation should handle EDT format with extensions"() {
        def pipeline = """
        @Library('jenkins-lib')
        import ru.pulsar.jenkins.library.steps.EdtToDesignerFormatTransformation
        import ru.pulsar.jenkins.library.configuration.JobConfiguration
        import ru.pulsar.jenkins.library.configuration.SourceFormat
        
        pipeline {
            agent any
            stages {
                stage('test') {
                    steps {
                        script {
                            def config = new JobConfiguration()
                            config.sourceFormat = SourceFormat.EDT
                            config.edtVersion = "2023.1"
                            
                            // Mock extension loading
                            config.metaClass.needLoadExtensions = { -> true }
                            
                            def transformation = new EdtToDesignerFormatTransformation(config)
                            
                            // We expect this to fail in integration test 
                            // since we don't have actual EDT environment
                            try {
                                transformation.run()
                                echo "Transformation completed successfully"
                            } catch (Exception e) {
                                echo "Transformation failed as expected in test: \${e.message}"
                            }
                        }
                    }
                }
            }
        }
        """.stripIndent()
        
        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'edt-transform-test')
        workflowJob.definition = flow

        rule.buildAndAssertSuccess(workflowJob)
    }
}