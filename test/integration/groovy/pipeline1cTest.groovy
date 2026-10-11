import hudson.FilePath
import hudson.model.Label
import hudson.model.Result
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

    @Test
    void "pipeline1C should fail in pre-stage on notInit without template and base"() {
        def pipeline = '''
        pipeline1C()
        '''.stripIndent()

        def agent = rule.createSlave(Label.get("agent"))
        rule.waitOnline(agent)
        final CpsFlowDefinition flow = new CpsFlowDefinition(pipeline, true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-not-init')
        workflowJob.definition = flow
        agent.getWorkspaceFor(workflowJob)
            .child('jobConfiguration.json')
            .write('{"initInfobase": {"initMethod": "notInit"}}', 'UTF-8')

        def build = rule.buildAndAssertStatus(Result.FAILURE, workflowJob)

        rule.assertLogContains('notInit (initInfobase -> initMethod)', build)
    }

    @Test
    void "pipeline1C should lock external base for the build and queue test stages on it"() {
        // один агент для pre-stage и стадий 1С, v8version по умолчанию 8.3
        def agent = rule.createSlave('agent-1c', 'agent 8.3', null)
        rule.waitOnline(agent)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-lock-external')
        workflowJob.definition = new CpsFlowDefinition('pipeline1C()', true)

        FilePath workspace = agent.getWorkspaceFor(workflowJob)
        workspace.child('jobConfiguration.json').write('''
            {
              "basePath": "/SSrv1C\\\\Test_Base",
              "stages": { "syntaxCheck": true },
              "initInfobase": { "initMethod": "notInit" },
              "syntaxCheck": { "publishToJUnitReport": false }
            }
            ''', 'UTF-8')
        writeFakeVrunner(workspace)

        def build = rule.buildAndAssertSuccess(workflowJob)

        String outer = '/Ssrv1c:1541\\test_base'
        String inner = "${outer}_jenkins-project-lock-external-1"
        List<String> acquired = rule.getLog(build).readLines().findAll { it.contains('acquired') }
        // внешняя блокировка берется на нормализованный адрес базы
        assert acquired.count { it.contains(outer) && !it.contains(inner) } == 1: acquired.join('\n')
        // внутреннюю блокировку берет только включенная тестовая стадия, выключенные ее не ждут,
        // а вложенная блокировка внутри parallel не зависает
        assert acquired.count { it.contains(inner) } == 1: acquired.join('\n')
        String syntaxCheck = rule.getLog(build).readLines().find { it.contains('fake-vrunner syntax-check') }
        assert syntaxCheck != null && syntaxCheck.contains('/SSrv1C\\Test_Base')
    }

    @Test
    void "pipeline1C should lock local base only under build tag"() {
        rule.createSlave(Label.get("agent"))
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-lock-local')
        workflowJob.definition = new CpsFlowDefinition('pipeline1C()', true)

        def build = rule.buildAndAssertSuccess(workflowJob)

        // имя, которое никто больше не запросит: локальная база не блокирует чужие сборки
        List<String> acquired = rule.getLog(build).readLines().findAll { it.contains('acquired') }
        assert acquired.size() == 1: acquired.join('\n')
        assert acquired[0].contains('jenkins-project-lock-local-1')
    }

    // vrunner из oscript_modules проекта подменяется скриптом, который только выводит свои аргументы
    private static void writeFakeVrunner(FilePath workspace) {
        if (File.separatorChar == '\\' as char) {
            workspace.child('oscript_modules/bin/vrunner.bat').write('@echo off\r\necho fake-vrunner %*\r\n', 'UTF-8')
        } else {
            FilePath vrunner = workspace.child('oscript_modules/bin/vrunner')
            vrunner.write('#!/bin/sh\necho "fake-vrunner $*"\n', 'UTF-8')
            vrunner.chmod(0755)
        }
    }

}
