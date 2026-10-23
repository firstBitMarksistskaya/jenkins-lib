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

class infobaseCredentialsTest {

    private static final boolean WINDOWS = File.separatorChar == '\\' as char
    private static final String STANDARD_ID = '_INFOBASE_USER'
    private static final String PASSWORD = 'fixture-secret'

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "standard infobase user reaches vrunner and VA test client without leaks"() {
        addCredential(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, STANDARD_ID, '', 'test-user', PASSWORD))

        WorkflowRun build = runBdd(Result.SUCCESS)

        rule.assertLogContains("Credentials: standard ID $STANDARD_ID", build)
        rule.assertLogContains('pwd=[****]', build)
        // копия настроек VA с паролем клиента тестирования передана команде vanessa
        rule.assertLogContains('--vanessasettings', build)
        rule.assertLogContains('/P\\"****\\"', build)
        rule.assertLogNotContains(PASSWORD, build)
        assert copies(build).isEmpty(): 'копия настроек VA с паролем осталась после команды'
    }

    @Test
    void "empty password is passed explicitly instead of password from settings"() {
        addCredential(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, 'empty-password', '', 'test-user', ''))

        WorkflowRun build = runBdd(Result.SUCCESS, '"secrets": {"infobaseUser": "empty-password"},')

        rule.assertLogContains('Credentials: explicit ID empty-password', build)
        rule.assertLogContains('pwd=[""]', build)
        // без пароля VA сама запускает клиент тестирования с пользователем менеджера
        rule.assertLogNotContains('--vanessasettings', build)
    }

    @Test
    void "absent standard infobase user keeps vrunner settings"() {
        WorkflowRun build = runBdd(Result.SUCCESS)

        rule.assertLogContains("Credentials: without credentials ($STANDARD_ID not available)", build)
        rule.assertLogContains('pwd=[]', build)
        rule.assertLogNotContains('--vanessasettings', build)
    }

    @Test
    void "missing explicit infobase user does not fall back to standard"() {
        addCredential(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, STANDARD_ID, '', 'test-user', PASSWORD))

        WorkflowRun build = runBdd(Result.FAILURE, '"secrets": {"infobaseUser": "missing-user"},')

        rule.assertLogNotContains('fake-vrunner', build)
        rule.assertLogNotContains('Credentials: standard ID', build)
    }

    @Test
    void "wrong type standard infobase user fails before vrunner"() {
        addCredential(new StringCredentialsImpl(CredentialsScope.GLOBAL, STANDARD_ID, '', Secret.fromString(PASSWORD)))

        WorkflowRun build = runBdd(Result.FAILURE)

        rule.assertLogContains("Credentials: standard ID $STANDARD_ID", build)
        rule.assertLogNotContains('fake-vrunner', build)
        rule.assertLogNotContains(PASSWORD, build)
    }

    private static void addCredential(credential) {
        SystemCredentialsProvider.getInstance().getCredentials().add(credential)
        SystemCredentialsProvider.getInstance().save()
    }

    // vrunner из oscript_modules проекта подменяется скриптом, который выводит аргументы, переменные
    // с пользователем ИБ и содержимое файла --vanessasettings
    private WorkflowRun runBdd(Result expectedResult, String secretsJson = '') {
        WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-infobase-credentials')
        workflowJob.definition = new CpsFlowDefinition('''
        node {
            bdd jobConfiguration()
        }
        '''.stripIndent(), true)

        FilePath workspace = rule.jenkins.getWorkspaceFor(workflowJob)
        workspace.child('jobConfiguration.json').write('{' + secretsJson + ' "stages": {"bdd": true}}', 'UTF-8')
        workspace.child('tools/vrunner.json').write('''
            {
              "default": { "--db-user": "file-user", "--db-pwd": "file-password" },
              "vanessa": { "--vanessasettings": "./tools/VBParams.json" }
            }
            '''.stripIndent(), 'UTF-8')
        workspace.child('tools/VBParams.json').write('{ "ВыполнитьСценарии": true }', 'UTF-8')

        if (WINDOWS) {
            workspace.child('oscript_modules/bin/vrunner.bat').write('@echo off\r\n' +
                'echo fake-vrunner %* user=[%RUNNER_DBUSER%] pwd=[%RUNNER_DBPWD%]\r\n' +
                ':loop\r\n' +
                'if "%~1"=="" goto end\r\n' +
                'if "%~1"=="--vanessasettings" type "%~2"\r\n' +
                'shift\r\n' +
                'goto loop\r\n' +
                ':end\r\n', 'UTF-8')
        } else {
            FilePath vrunner = workspace.child('oscript_modules/bin/vrunner')
            vrunner.write('#!/bin/sh\n' +
                'echo "fake-vrunner $* user=[$RUNNER_DBUSER] pwd=[$RUNNER_DBPWD]"\n' +
                'while [ $# -gt 0 ]; do\n' +
                '  if [ "$1" = "--vanessasettings" ]; then cat "$2"; echo; fi\n' +
                '  shift\n' +
                'done\n' +
                'exit 0\n', 'UTF-8')
            vrunner.chmod(0755)
        }

        return rule.buildAndAssertStatus(expectedResult, workflowJob)
    }

    // Копии настроек VA во временном каталоге рабочего каталога сборки
    private List<FilePath> copies(WorkflowRun build) {
        FilePath workspace = rule.jenkins.getWorkspaceFor(build.parent)
        FilePath copiesDir = workspace.sibling(workspace.name + '@tmp').child('infobase-credentials')
        return copiesDir.exists() ? copiesDir.list() : []
    }
}
