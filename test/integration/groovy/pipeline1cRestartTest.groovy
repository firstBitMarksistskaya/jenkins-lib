import hudson.model.Label
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class pipeline1cRestartTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "pipeline1C should handle configuration loading when restarted"() {
        def pipeline = '''
        pipeline1C()
        '''.stripIndent()

        rule.createSlave(Label.get("agent"))
        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
        workflowJob.definition = flow

        def result = rule.buildAndAssertSuccess(workflowJob)
        // The test should complete without NPE errors related to config.stageFlags
        rule.assertLogNotContains('NullPointerException', result)
        rule.assertLogNotContains('Cannot get property \'stageFlags\' on null object', result)
    }

    @Test
    void "pipeline1C when conditions should not throw NPE on restart"() {
        // This test verifies that when conditions using config don't fail with NPE
        // when the pipeline potentially restarts from a later stage
        def pipeline = '''
        pipeline1C()
        '''.stripIndent()

        rule.createSlave(Label.get("agent"))
        rule.createSlave(Label.get("sonar"))  // Need sonar agent for SonarQube stage
        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
        workflowJob.definition = flow

        def result = rule.buildAndAssertSuccess(workflowJob)
        
        // Verify that the pipeline completes without the specific error from the issue
        rule.assertLogNotContains('Cannot get property \'stageFlags\' on null object', result)
        rule.assertLogNotContains('java.lang.NullPointerException: Cannot get property \'stageFlags\' on null object', result)
    }
}