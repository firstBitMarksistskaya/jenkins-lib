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
    void "pipeline1C should prepare own base of stage and run stage on it"() {
        def agent = rule.createSlave('agent-1c-stage-base', 'agent 8.3', null)
        rule.waitOnline(agent)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-stage-base')
        workflowJob.definition = new CpsFlowDefinition('pipeline1C()', true)

        FilePath workspace = agent.getWorkspaceFor(workflowJob)
        workspace.child('jobConfiguration.json').write('''
            {
              "basePath": "/Ssrv1c\\\\ssl",
              "stages": { "bdd": true, "syntaxCheck": true },
              "initInfobase": { "initMethod": "notInit" },
              "syntaxCheck": { "publishToJUnitReport": false, "basePath": "/Ssrv1c\\\\ssl2" }
            }
            ''', 'UTF-8')
        writeFakeVrunner(workspace)

        def build = rule.buildAndAssertSuccess(workflowJob)

        List<String> lines = rule.getLog(build).readLines()
        String buildTag = 'jenkins-project-stage-base-1'
        List<String> acquired = lines.findAll { it.contains('acquired') }
        // внешняя блокировка берет обе базы сборки одним шагом
        assert acquired.any { !it.contains(buildTag) && it.contains(':1541\\ssl2') && it =~ /:1541\\ssl(?![2_])/ }: acquired.join('\n')
        // каждая тестовая стадия встает в очередь своей базы
        assert acquired.any { it.contains("/Ssrv1c:1541\\ssl_$buildTag") }: acquired.join('\n')
        assert acquired.any { it.contains("/Ssrv1c:1541\\ssl2_$buildTag") }: acquired.join('\n')
        // база стадии готовится в своей ветке отдельно от базы сборки, стадия работает с ней.
        // Сообщения сверяются без кириллицы: журнал агента на Windows пишется не в UTF-8
        assert lines.any { it.contains('(Branch: /Ssrv1c\\ssl2)') }: lines.join('\n')
        assert lines.any { it.endsWith(' /Ssrv1c\\ssl2') && !it.contains('fake-vrunner') }: lines.join('\n')
        assert lines.any { it.endsWith(' /Ssrv1c\\ssl') && !it.contains('fake-vrunner') }: lines.join('\n')
        String syntaxCheck = lines.find { it.contains('fake-vrunner syntax-check') }
        assert syntaxCheck != null && syntaxCheck.contains('/Ssrv1c\\ssl2'): lines.join('\n')
        String bdd = lines.find { it.contains('fake-vrunner vanessa') }
        assert bdd != null && bdd.contains('/Ssrv1c\\ssl') && !bdd.contains('ssl2'): lines.join('\n')
    }

    @Test
    void "pipeline1C should dump external server base when saving is enabled"() {
        def agent = rule.createSlave('agent-1c-dump', 'agent 8.3', null)
        rule.waitOnline(agent)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-dump')
        workflowJob.definition = new CpsFlowDefinition('pipeline1C()', true)

        FilePath workspace = agent.getWorkspaceFor(workflowJob)
        workspace.child('jobConfiguration.json').write('''
            {
              "basePath": "/Ssrv1c\\\\ssl",
              "stages": { "syntaxCheck": true },
              "initInfobase": { "initMethod": "notInit", "archiveInfobase": { "onAlways": true } },
              "syntaxCheck": { "publishToJUnitReport": false }
            }
            ''', 'UTF-8')
        // подставной vrunner создает файл выгрузки, как настоящий dump
        if (File.separatorChar == '\\' as char) {
            workspace.child('oscript_modules/bin/vrunner.bat').write(
                '@echo off\r\necho fake-vrunner %*\r\nif "%1"=="dump" echo dt> %2\r\nexit /b 0\r\n', 'UTF-8')
        } else {
            FilePath vrunner = workspace.child('oscript_modules/bin/vrunner')
            vrunner.write('#!/bin/sh\necho "fake-vrunner $*"\n[ "$1" = "dump" ] && echo dt > "$2"\nexit 0\n', 'UTF-8')
            vrunner.chmod(0755)
        }

        def build = rule.buildAndAssertSuccess(workflowJob)

        List<String> lines = rule.getLog(build).readLines()
        assert lines.any { it.contains('fake-vrunner dump 1Cv8.dt --ibconnection') && it.contains('/Ssrv1c\\ssl') }: lines.join('\n')
        // внешняя база сохраняется только как артефакт, архив файла базы для тестовых стадий не нужен
        assert build.artifacts*.fileName.contains('1Cv8.dt')
        assert !lines.any { it.contains('1Cv8.1CD.zip') }: lines.join('\n')
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
