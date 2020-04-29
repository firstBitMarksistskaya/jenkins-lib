import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class cmdTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "cmd should echo something"() {
        def pipeline = '''
        pipeline {
            agent any
            stages {
                stage('test') {
                    steps {
                        cmd("echo helloWorld")
                    }
                }
            }
        } 
    '''.stripIndent()
        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
        workflowJob.definition = flow

        rule.assertLogContains('helloWorld', rule.buildAndAssertSuccess(workflowJob))
    }

    @Test
    void "cmd should return status"() {
        def pipeline = '''
        pipeline {
            agent any
            stages {
                stage('test') {
                    steps {
                        script {
                            def status = cmd("false", true)
                            echo "status = $status"
                        }
                    }
                }
            }
        } 
    '''.stripIndent()
        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
        workflowJob.definition = flow

        rule.assertLogContains('status = 1', rule.buildAndAssertSuccess(workflowJob))
    }
}