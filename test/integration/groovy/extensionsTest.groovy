import hudson.FilePath
import hudson.model.Result
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class extensionsTest {

    private static final boolean WINDOWS = File.separatorChar == '\\' as char

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "stage should delete extensions of other stages and load its own"() {
        WorkflowRun build = buildWithFakeVrunner(['YAXUnit', 'BDD'], Result.SUCCESS)

        List<String> lines = rule.getLog(build).readLines()
        // в базе осталось расширение стадии yaxunit от прошлой сборки, свое расширение стадия не удаляет
        assert lines.any { it.contains('fake-vrunner designer') && it.contains('/DeleteCfg -Extension YAXUnit') }
        assert !lines.any { it.contains('/DeleteCfg -Extension BDD') }
        assert lines.any { it.contains('fake-vrunner run') && it.contains('BDD.cfe') }
    }

    @Test
    void "stage should fail when its extension is missing after loading"() {
        // обработка подключения завершилась без ошибки, но расширения в базе нет
        WorkflowRun build = buildWithFakeVrunner(['YAXUnit'], Result.FAILURE)

        rule.assertLogContains('BDD', build)
        assert rule.getLog(build).readLines().any { it.contains('fake-vrunner run') }
    }

    // vrunner из oscript_modules проекта подменяется скриптом: он выводит свои аргументы,
    // а на команды конфигуратора пишет список расширений базы в файл, который читает библиотека
    private WorkflowRun buildWithFakeVrunner(List<String> infobaseExtensions, Result expectedResult) {
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-extensions')
        workflowJob.definition = new CpsFlowDefinition('''
        node {
            def config = jobConfiguration()
            normalizeExtensions config, 'bdd'
            loadExtensions config, 'bdd'
        }
        '''.stripIndent(), true)

        FilePath workspace = rule.jenkins.getWorkspaceFor(workflowJob)
        workspace.child('jobConfiguration.json').write('''
            {
              "basePath": "/Ssrv1c\\\\test_base",
              "initInfobase": {
                "extensions": [
                  { "name": "BDD", "stages": ["bdd"] },
                  { "name": "YAXUnit", "stages": ["yaxunit"] }
                ]
              }
            }
            ''', 'UTF-8')

        if (WINDOWS) {
            String list = infobaseExtensions.collect { "echo $it>> build\\tmp\\infobase-extensions.txt" }.join('\r\n')
            workspace.child('oscript_modules/bin/vrunner.bat').write(
                "@echo off\r\necho fake-vrunner %*\r\nif not \"%1\"==\"designer\" exit /b 0\r\n" +
                "type nul> build\\tmp\\infobase-extensions.txt\r\n$list\r\n", 'UTF-8')
        } else {
            String list = infobaseExtensions.join('\\n')
            FilePath vrunner = workspace.child('oscript_modules/bin/vrunner')
            vrunner.write("#!/bin/sh\necho \"fake-vrunner \$*\"\n[ \"\$1\" = \"designer\" ] || exit 0\n" +
                "printf '$list\\n' > build/tmp/infobase-extensions.txt\n", 'UTF-8')
            vrunner.chmod(0755)
        }

        return rule.buildAndAssertStatus(expectedResult, workflowJob)
    }
}
