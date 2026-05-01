import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class createDirTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "createDir should create directory without cleanup"() {
        def pipeline = '''
        pipeline {
            agent any
            stages {
                stage('test') {
                    steps {
                        script {
                            createDir('build/out/custom')
                            writeFile file: 'build/out/custom/created.txt', text: 'created'
                            echo "createdFile=${fileExists('build/out/custom/created.txt')}"
                        }
                    }
                }
            }
        }
    '''.stripIndent()

        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-create-dir')
        workflowJob.definition = flow

        rule.assertLogContains('createdFile=true', rule.buildAndAssertSuccess(workflowJob))
    }

    @Test
    void "createDir should recreate directory after cleanup"() {
        def pipeline = '''
        pipeline {
            agent any
            stages {
                stage('test') {
                    steps {
                        script {
                            createDir('build/out/custom')
                            writeFile file: 'build/out/custom/stale.txt', text: 'old data'

                            createDir('build/out/custom', true)

                            writeFile file: 'build/out/custom/fresh.txt', text: 'fresh data'
                            echo "staleExists=${fileExists('build/out/custom/stale.txt')}"
                            echo "freshExists=${fileExists('build/out/custom/fresh.txt')}"
                        }
                    }
                }
            }
        }
    '''.stripIndent()

        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-create-dir-clean')
        workflowJob.definition = flow

        def build = rule.buildAndAssertSuccess(workflowJob)
        rule.assertLogContains('staleExists=false', build)
        rule.assertLogContains('freshExists=true', build)
    }
}
