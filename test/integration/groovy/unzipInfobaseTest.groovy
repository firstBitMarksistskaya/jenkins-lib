import hudson.model.Result
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class unzipInfobaseTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "unzipInfobase should skip infobase archive for external base"() {
        WorkflowJob workflowJob = createJob('project-unzip-external')
        rule.jenkins.getWorkspaceFor(workflowJob)
            .child('jobConfiguration.json')
            .write('{"basePath": "/Ssrv1c\\\\test_base"}', 'UTF-8')

        def build = rule.buildAndAssertSuccess(workflowJob)

        rule.assertLogContains('unzip done', build)
    }

    @Test
    void "unzipInfobase should unstash infobase archive for local base"() {
        WorkflowJob workflowJob = createJob('project-unzip-local')

        // архива локальной базы в этой сборке нет, поэтому шаг падает на его распаковке
        def build = rule.buildAndAssertStatus(Result.FAILURE, workflowJob)

        rule.assertLogContains('1Cv8.1CD.zip', build)
        rule.assertLogNotContains('unzip done', build)
    }

    private WorkflowJob createJob(String name) {
        WorkflowJob workflowJob = rule.createProject(WorkflowJob, name)
        workflowJob.definition = new CpsFlowDefinition('''
        node {
            unzipInfobase jobConfiguration()
            echo 'unzip done'
        }
        '''.stripIndent(), true)
        return workflowJob
    }
}
