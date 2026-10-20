import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import hudson.FilePath
import hudson.model.Result
import hudson.util.Secret
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class clusterCoverageTest {

    private static final boolean WINDOWS = File.separatorChar == '\\' as char
    private static final String CONNECTION = '-i test_base -u http://srv1c:1550'

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "bdd should measure coverage on cluster debug server of server base"() {
        SystemCredentialsProvider.getInstance().getCredentials().add(new StringCredentialsImpl(
            CredentialsScope.GLOBAL, 'debug-server-password', 'debug server password', Secret.fromString('s3cret')))
        SystemCredentialsProvider.getInstance().save()

        WorkflowJob workflowJob = createJob(', "secrets": {"debugServerPassword": "debug-server-password"}', false)
        WorkflowRun build = rule.buildAndAssertStatus(Result.SUCCESS, workflowJob)

        String log = rule.getLog(build)
        assertInOrder(log, "fake-coverage41c check $CONNECTION", 'fake-vrunner vanessa', "fake-coverage41c stop $CONNECTION")
        // dbgs на агенте не ищется и не запускается, порт сервера отладки агента не блокируется
        rule.assertLogNotContains('dbgs', build)
        rule.assertLogNotContains('built-in_1550', build)

        // start запускается в фоне, его вывод пишется в файл: пароль доходит до Coverage41C через переменную окружения
        String startLog = rule.jenkins.getWorkspaceFor(workflowJob).child('build/BDD-start-Coverage41C.log').readToString()
        assert startLog.contains("fake-coverage41c start $CONNECTION")
        assert startLog.contains('-p:env COVERAGE41C_DEBUG_SERVER_PASSWORD')
        assert startLog.contains('password-set')
        assert !startLog.contains('s3cret')
        rule.assertLogNotContains('s3cret', build)
    }

    @Test
    void "bdd should stop Coverage41C when tests fail on server base"() {
        WorkflowRun build = rule.buildAndAssertStatus(Result.FAILURE, createJob('', true))

        assertInOrder(rule.getLog(build), 'fake-vrunner vanessa', "fake-coverage41c stop $CONNECTION")
    }

    @Test
    void "bdd should fail without cluster debug server on server base"() {
        WorkflowRun build = rule.buildAndAssertStatus(Result.FAILURE, createJob('', false, ''))

        rule.assertLogContains('coverage -> clusterDebugServer', build)
        rule.assertLogNotContains('fake-coverage41c', build)
        rule.assertLogNotContains('fake-vrunner', build)
    }

    @Test
    void "standard debug password is found and bound in current build"() {
        addCredential(new StringCredentialsImpl(CredentialsScope.GLOBAL,
            'DEBUG_SERVER_PASSWORD', '', Secret.fromString('fixture-password')))
        WorkflowJob job = createJob(', "secrets": {"clusterAdmin": "custom-admin", "debugServerPassword": ""}', false)
        WorkflowRun build = rule.buildAndAssertStatus(Result.SUCCESS, job)
        rule.assertLogContains('Credentials: standard ID DEBUG_SERVER_PASSWORD', build)
        String startLog = rule.jenkins.getWorkspaceFor(job).child('build/BDD-start-Coverage41C.log').readToString()
        assert startLog.contains('-p:env COVERAGE41C_DEBUG_SERVER_PASSWORD')
        assert startLog.contains('password-set')
        assert !startLog.contains('fixture-password')
    }

    @Test
    void "absent standard debug password runs without credentials"() {
        WorkflowJob job = createJob('', false)
        WorkflowRun build = rule.buildAndAssertStatus(Result.SUCCESS, job)
        rule.assertLogContains('Credentials: without credentials (DEBUG_SERVER_PASSWORD not available)', build)
        String startLog = rule.jenkins.getWorkspaceFor(job).child('build/BDD-start-Coverage41C.log').readToString()
        assert !startLog.contains('-p:env')
    }

    @Test
    void "missing explicit debug password does not fall back to standard"() {
        addCredential(new StringCredentialsImpl(CredentialsScope.GLOBAL,
            'DEBUG_SERVER_PASSWORD', '', Secret.fromString('fixture-password')))
        WorkflowRun build = rule.buildAndAssertStatus(Result.FAILURE,
            createJob(', "secrets": {"debugServerPassword": "missing-password"}', false))
        rule.assertLogNotContains('fake-coverage41c', build)
        rule.assertLogNotContains('fake-vrunner', build)
        rule.assertLogNotContains('Credentials: standard ID', build)
    }

    @Test
    void "wrong type standard debug password fails before start"() {
        addCredential(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
            'DEBUG_SERVER_PASSWORD', '', 'fixture-user', 'fixture-password'))
        WorkflowRun build = rule.buildAndAssertStatus(Result.FAILURE, createJob('', false))
        rule.assertLogContains('Credentials: standard ID DEBUG_SERVER_PASSWORD', build)
        rule.assertLogNotContains('fake-coverage41c', build)
    }

    @Test
    void "wrong type explicit debug password does not fall back to standard"() {
        addCredential(new StringCredentialsImpl(CredentialsScope.GLOBAL,
            'DEBUG_SERVER_PASSWORD', '', Secret.fromString('fixture-password')))
        addCredential(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
            'wrong-password', '', 'fixture-user', 'fixture-password'))
        WorkflowRun build = rule.buildAndAssertStatus(Result.FAILURE,
            createJob(', "secrets": {"debugServerPassword": "wrong-password"}', false))
        rule.assertLogNotContains('fake-coverage41c', build)
        rule.assertLogNotContains('Credentials: standard ID', build)
    }

    private static void addCredential(credential) {
        SystemCredentialsProvider.getInstance().getCredentials().add(credential)
        SystemCredentialsProvider.getInstance().save()
    }

    // vrunner из oscript_modules проекта и Coverage41C в PATH подменяются скриптами, которые только выводят свои аргументы.
    // При failTests подставной vrunner пишет код возврата 3, и стадия BDD падает внутри замеров.
    // Порт сервера отладки не указан: по умолчанию 1550
    private WorkflowJob createJob(String extraJson, boolean failTests, String clusterDebugServer = 'srv1c') {
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-cluster-coverage')
        workflowJob.definition = new CpsFlowDefinition('''
        node {
            withEnv(["PATH+FAKE=${pwd()}${isUnix() ? '/' : '\\\\'}fakebin"]) {
                stage('BDD') {
                    bdd jobConfiguration()
                }
            }
        }
        '''.stripIndent(), true)

        FilePath workspace = rule.jenkins.getWorkspaceFor(workflowJob)
        workspace.child('jobConfiguration.json').write(
            '{"basePath": "/Ssrv1c\\\\test_base", "stages": {"bdd": true}, "bdd": {"coverage": true}, ' +
                '"coverage": {"clusterDebugServer": "' + clusterDebugServer + '"}' + extraJson + '}', 'UTF-8')

        if (WINDOWS) {
            String failure = failTests ? '\r\n>build\\out\\bdd-exit-code.log echo 3' : ''
            workspace.child('oscript_modules/bin/vrunner.bat').write("@echo off\r\necho fake-vrunner %*$failure\r\n", 'UTF-8')
            workspace.child('fakebin/Coverage41C.bat').write('@echo off\r\necho fake-coverage41c %*\r\n' +
                'if "%1"=="start" if defined COVERAGE41C_DEBUG_SERVER_PASSWORD echo password-set\r\n', 'UTF-8')
        } else {
            String failure = failTests ? '\necho 3 > build/out/bdd-exit-code.log' : ''
            writeExecutable(workspace.child('oscript_modules/bin/vrunner'), "#!/bin/sh\necho \"fake-vrunner \$*\"$failure\nexit 0\n")
            writeExecutable(workspace.child('fakebin/Coverage41C'), '#!/bin/sh\necho "fake-coverage41c $*"\n' +
                '[ "$1" = "start" ] && [ -n "$COVERAGE41C_DEBUG_SERVER_PASSWORD" ] && echo password-set\nexit 0\n')
        }

        return workflowJob
    }

    private static void writeExecutable(FilePath file, String content) {
        file.write(content, 'UTF-8')
        file.chmod(0755)
    }

    private static void assertInOrder(String log, String... fragments) {
        int position = -1
        fragments.each { String fragment ->
            int next = log.indexOf(fragment, position + 1)
            assert next > position: "'$fragment' не найден после предыдущего фрагмента в логе:\n$log"
            position = next
        }
    }
}
