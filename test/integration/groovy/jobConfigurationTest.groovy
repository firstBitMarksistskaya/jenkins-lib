import org.apache.commons.io.IOUtils
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

import java.nio.charset.StandardCharsets

class jobConfigurationTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "jobConfiguration should not fail without file"() {

        def pipeline = """
        pipeline {
            agent any
            stages {
                stage('test') {
                    steps {
                        echo jobConfiguration().toString()
                    }
                }
            }
        } 
    """.stripIndent()
        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
        workflowJob.definition = flow

        rule.buildAndAssertSuccess(workflowJob)
    }

    @Test
    void "jobConfiguration should merge configurations"() {

        def file = IOUtils.resourceToString(
            'jobConfiguration.json',
            StandardCharsets.UTF_8,
            this.getClass().getClassLoader()
        );

        def writeFile = """
            writeFile text: \"\"\"$file\"\"\", file: 'jobConfiguration.json'
        """

        def pipeline = """
        pipeline {
            agent any
            stages {
                stage('test') {
                    steps {
                        $writeFile
                        echo jobConfiguration().toString()
                    }
                }
            }
        } 
    """.stripIndent()
        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
        workflowJob.definition = flow

        def run = rule.buildAndAssertSuccess(workflowJob)
        rule.assertLogContains("v8version='8.3.12.1500'", run)
        rule.assertLogContains("sonarScannerToolName='sonar-scanner'", run)
        rule.assertLogContains("initMethod=FROM_SOURCE", run)
    }
}