import hudson.model.Label
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class pipeline1cTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "pipeline1C should do something"() {
        def pipeline = '''
        pipeline1C()
        '''.stripIndent()

        rule.createSlave(Label.get("agent"))
        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
        workflowJob.definition = flow

        rule.assertLogContains('(pre-stage)', rule.buildAndAssertSuccess(workflowJob))
    }

    @Test
    void "pipeline1C should skip debug overrides when profile key is unavailable"() {
        def pipeline = '''
        pipeline1C()
        '''.stripIndent()

        rule.createSlave(Label.get("agent"))
        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-debug-overrides')
        workflowJob.definition = flow

        def build = rule.buildAndAssertSuccess(workflowJob)

        rule.assertLogContains('Debug overrides: profile key is unavailable, skip', build)
    }

}
