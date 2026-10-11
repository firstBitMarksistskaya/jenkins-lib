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

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "createInfobase should restore dt into server base between session kill and unlock"() {
        WorkflowRun build = buildWithFakeVrunner(false, Result.SUCCESS)

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
    }

    @Test
    void "createInfobase should unlock server base when restore fails"() {
        WorkflowRun build = buildWithFakeVrunner(true, Result.FAILURE)

        String log = rule.getLog(build)
        assertInOrder(log, 'fake-vrunner session kill', 'fake-vrunner restore', 'fake-vrunner session unlock')
    }

    // vrunner из oscript_modules проекта подменяется скриптом, который только выводит свои аргументы
    private WorkflowRun buildWithFakeVrunner(boolean failRestore, Result expectedResult) {
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-exclusive-access')
        workflowJob.definition = new CpsFlowDefinition('''
        node {
            createInfobase jobConfiguration()
        }
        '''.stripIndent(), true)

        FilePath workspace = rule.jenkins.getWorkspaceFor(workflowJob)
        workspace.child('jobConfiguration.json').write(
            '{"basePath": "/Ssrv1c\\\\test_base", "initInfobase": {"templateDBPath": "template.dt"}}', 'UTF-8')
        workspace.child('template.dt').write('dt', 'UTF-8')

        if (File.separatorChar == '\\' as char) {
            String failure = failRestore ? '\r\nif "%1"=="restore" exit /b 1' : ''
            workspace.child('oscript_modules/bin/vrunner.bat').write("@echo off\r\necho fake-vrunner %*$failure\r\n", 'UTF-8')
        } else {
            String failure = failRestore ? '\n[ "$1" = "restore" ] && exit 1' : ''
            FilePath vrunner = workspace.child('oscript_modules/bin/vrunner')
            vrunner.write("#!/bin/sh\necho \"fake-vrunner \$*\"$failure\nexit 0\n", 'UTF-8')
            vrunner.chmod(0755)
        }

        return rule.buildAndAssertStatus(expectedResult, workflowJob)
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
