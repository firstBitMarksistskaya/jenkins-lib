import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import hudson.util.Secret
import hudson.FilePath
import hudson.model.Result
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class exclusiveAccessTest {

    private static final boolean WINDOWS = File.separatorChar == '\\' as char

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "createInfobase should restore dt into server base between session kill and unlock"() {
        WorkflowRun build = buildWithFakeVrunner('/Ssrv1c\\\\test_base', false, Result.SUCCESS)

        String log = rule.getLog(build)
        assertInOrder(log, 'fake-vrunner session kill', 'fake-vrunner restore', 'fake-vrunner session unlock')

        // restore получает тот же код разрешения, что и session kill, а снятие запрета идет без кода
        List<String> lines = log.readLines()
        String kill = lines.find { it.contains('fake-vrunner session kill') }
        String restore = lines.find { it.contains('fake-vrunner restore') }
        String unlock = lines.find { it.contains('fake-vrunner session unlock') }
        String uccode = (kill =~ /--uccode (\S+)/)[0][1]
        assert restore.contains("--uccode $uccode")
        assert !unlock.contains('--uccode')

        // без порта в строке подключения кластер не ищется, vanessa-runner берет первый кластер RAS
        assert !kill.contains('--cluster')
        rule.assertLogNotContains('build/tmp/rac_', build)
    }

    @Test
    void "createInfobase should unlock server base when restore fails"() {
        WorkflowRun build = buildWithFakeVrunner('/Ssrv1c\\\\test_base', true, Result.FAILURE)

        String log = rule.getLog(build)
        assertInOrder(log, 'fake-vrunner session kill', 'fake-vrunner restore', 'fake-vrunner session unlock')
    }

    @Test
    void "createInfobase should select cluster by port from connection"() {
        WorkflowRun build = buildWithFakeVrunner('/Ssrv1c:1641\\\\test_base', false, Result.SUCCESS)

        rule.assertLogContains('build/tmp/rac_', build)
        List<String> lines = rule.getLog(build).readLines()
        String kill = lines.find { it.contains('fake-vrunner session kill') }
        String unlock = lines.find { it.contains('fake-vrunner session unlock') }
        assert kill.contains('--cluster 9a8b7c6d-2222-4e1f-b2c3-d4e5f6a7b8c9')
        assert unlock.contains('--cluster 9a8b7c6d-2222-4e1f-b2c3-d4e5f6a7b8c9')
    }

    @Test
    void "standard cluster admin is found and bound in current build"() {
        addCredential(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
            'CLUSTER_ADMIN', 'test administrator', 'fixture-user', 'fixture-password'))
        WorkflowRun build = buildWithFakeVrunner('/Ssrv1c\\\\test_base', false, Result.SUCCESS)
        rule.assertLogContains('Credentials: standard ID CLUSTER_ADMIN', build)
        rule.assertLogContains('Masking supported pattern matches', build)
        rule.assertLogNotContains('fixture-password', build)
    }

    @Test
    void "absent standard cluster admin runs without credentials"() {
        WorkflowRun build = buildWithFakeVrunner('/Ssrv1c\\\\test_base', false, Result.SUCCESS)
        rule.assertLogContains('Credentials: without credentials (CLUSTER_ADMIN not available)', build)
    }

    @Test
    void "missing explicit cluster admin does not fall back to standard"() {
        addCredential(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
            'CLUSTER_ADMIN', '', 'fixture-user', 'fixture-password'))
        WorkflowRun build = buildWithFakeVrunner('/Ssrv1c\\\\test_base', false, Result.FAILURE,
            ', "secrets": {"clusterAdmin": "missing-admin"}')
        rule.assertLogNotContains('fake-vrunner session kill', build)
        rule.assertLogNotContains('Credentials: standard ID', build)
    }

    @Test
    void "wrong type cluster admin fails before session kill"() {
        addCredential(new StringCredentialsImpl(CredentialsScope.GLOBAL,
            'CLUSTER_ADMIN', '', Secret.fromString('fixture-password')))
        WorkflowRun build = buildWithFakeVrunner('/Ssrv1c\\\\test_base', false, Result.FAILURE)
        rule.assertLogContains('Credentials: standard ID CLUSTER_ADMIN', build)
        rule.assertLogNotContains('fake-vrunner session kill', build)
    }

    @Test
    void "wrong type explicit cluster admin does not fall back to standard"() {
        addCredential(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
            'CLUSTER_ADMIN', '', 'fixture-user', 'fixture-password'))
        addCredential(new StringCredentialsImpl(CredentialsScope.GLOBAL,
            'wrong-admin', '', Secret.fromString('fixture-password')))
        WorkflowRun build = buildWithFakeVrunner('/Ssrv1c\\\\test_base', false, Result.FAILURE,
            ', "secrets": {"clusterAdmin": "wrong-admin"}')
        rule.assertLogNotContains('fake-vrunner session kill', build)
        rule.assertLogNotContains('Credentials: standard ID', build)
    }

    private static void addCredential(credential) {
        SystemCredentialsProvider.getInstance().getCredentials().add(credential)
        SystemCredentialsProvider.getInstance().save()
    }

    // vrunner из oscript_modules проекта подменяется скриптом, который только выводит свои аргументы.
    // oscript и rac в PATH подменяются скриптами: oscript отдает путь к поддельному rac, а тот выводит два кластера.
    private WorkflowRun buildWithFakeVrunner(String basePathJson, boolean failRestore, Result expectedResult, String secretsJson = '') {
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-exclusive-access')
        workflowJob.definition = new CpsFlowDefinition('''
        node {
            withEnv(["PATH+FAKE=${pwd()}${isUnix() ? '/' : '\\\\'}fakebin"]) {
                createInfobase jobConfiguration()
            }
        }
        '''.stripIndent(), true)

        FilePath workspace = rule.jenkins.getWorkspaceFor(workflowJob)
        workspace.child('jobConfiguration.json').write(
            '{"basePath": "' + basePathJson + '", "rasAddress": "srv1c:1545", "initInfobase": {"templateDBPath": "template.dt"}' + secretsJson + '}', 'UTF-8')
        workspace.child('template.dt').write('dt', 'UTF-8')

        String clusters = 'cluster : 0ee2d8a2-1111-4b6f-a0a8-7f1e2c3d4e5f|host : srv1c|port : 1541||' +
            'cluster : 9a8b7c6d-2222-4e1f-b2c3-d4e5f6a7b8c9|host : srv1c|port : 1641'
        if (WINDOWS) {
            String failure = failRestore ? '\r\nif "%1"=="restore" exit /b 1' : ''
            workspace.child('oscript_modules/bin/vrunner.bat').write("@echo off\r\necho fake-vrunner %*$failure\r\n", 'UTF-8')
            workspace.child('fakebin/oscript.bat').write('@echo off\r\necho %~dp0rac.bat> "%3"\r\n', 'UTF-8')
            String racOutput = clusters.split('\\|', -1).collect { it ? "echo $it" : 'echo.' }.join('\r\n')
            workspace.child('fakebin/rac.bat').write("@echo off\r\n$racOutput\r\n", 'UTF-8')
        } else {
            String failure = failRestore ? '\n[ "$1" = "restore" ] && exit 1' : ''
            writeExecutable(workspace.child('oscript_modules/bin/vrunner'),
                "#!/bin/sh\necho \"fake-vrunner \$*\"$failure\nexit 0\n")
            writeExecutable(workspace.child('fakebin/oscript'), '#!/bin/sh\necho "$(dirname "$0")/rac" > "$3"\n')
            writeExecutable(workspace.child('fakebin/rac'), "#!/bin/sh\nprintf '${clusters.replace('|', '\\n')}\\n'\n")
        }

        return rule.buildAndAssertStatus(expectedResult, workflowJob)
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
