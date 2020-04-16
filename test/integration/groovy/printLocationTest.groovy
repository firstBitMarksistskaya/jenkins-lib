import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class printLocationTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "Logger should echo current node name"() {
        def pipeline = '''
        import ru.pulsar.jenkins.library.utils.Logger
        
        pipeline {
            agent any
            stages {
                stage('test') {
                    steps {
                        printLocation()
                    }
                }
            }
        } 
    '''.stripIndent()
        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
        workflowJob.definition = flow

        rule.assertLogContains('Running on node master', rule.buildAndAssertSuccess(workflowJob))
    }

}
