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

    // mods и Fresh - расширения пайплайна, BDD - служебное расширение стадии bdd
    private static final String CONFIGURATION = '''
        {
          "basePath": "/Ssrv1c\\\\test_base",
          "initInfobase": {
            "extensions": [
              { "name": "mods" },
              { "name": "Fresh" },
              { "name": "BDD", "stages": ["bdd"] }
            ]
          }
        }
        '''

    @Rule
    public JenkinsRule rule = new JenkinsRule()

    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }

    @Test
    void "preparation should remove leftover stage extensions and update existing extensions"() {
        // в базе остались mods и служебное BDD прерванной сборки, Fresh еще не устанавливалось
        WorkflowRun build = buildWithFakeVrunner('''
            node {
                def config = jobConfiguration()
                loadExtensions config, 'initInfoBase'
            }
            ''', ['mods', 'BDD'], Result.SUCCESS)

        List<String> lines = rule.getLog(build).readLines()
        assert lines.any { it.contains('fake-vrunner designer') && it.contains('/DeleteCfg -Extension BDD') }
        // расширение пайплайна, которое уже есть в базе, обновляется конфигуратором, а не переустанавливается.
        // Подставной vrunner на Linux выводит аргументы без кавычек, на Windows - с кавычками
        assert lines.any { it.contains('fake-vrunner loadext') && it =~ /--extension "?mods"? --updatedb/ }
        assert !lines.any { it.contains('fake-vrunner run') && it.contains('mods.cfe') }
        assert lines.any { it.contains('fake-vrunner run') && it.contains('Fresh.cfe') }
        assert !lines.any { it.contains('/DeleteCfg -Extension mods') }
    }

    @Test
    void "stage should remove its extensions after it even when it fails"() {
        WorkflowRun build = buildWithFakeVrunner('''
            @groovy.transform.Field
            def config

            pipeline {
                agent none
                stages {
                    stage('BDD') {
                        agent any
                        options {
                            lock('test-base')
                        }
                        stages {
                            stage('load') {
                                steps {
                                    script {
                                        config = jobConfiguration()
                                    }
                                    loadExtensions config, 'bdd'
                                }
                            }
                            stage('tests') {
                                steps {
                                    error 'tests failed'
                                }
                            }
                        }
                        post {
                            always {
                                removeStageExtensions config, 'bdd'
                            }
                        }
                    }
                }
            }
            ''', ['mods', 'BDD'], Result.FAILURE)

        List<String> lines = rule.getLog(build).readLines()
        int loaded = lines.findIndexOf { it.contains('fake-vrunner run') && it.contains('BDD.cfe') }
        int removed = lines.findIndexOf { it.contains('fake-vrunner designer') && it.contains('/DeleteCfg -Extension BDD') }
        int released = lines.findIndexOf { it.contains('Lock released') && it.contains('test-base') }
        assert loaded >= 0 && removed > loaded: lines.join('\n')
        // расширение удаляется, пока стадия держит блокировку базы: следующая стадия его уже не увидит
        assert released > removed: lines.join('\n')
        // расширения пайплайна стадия не удаляет
        assert !lines.any { it.contains('/DeleteCfg -Extension mods') }
    }

    // vrunner из oscript_modules проекта подменяется скриптом: он выводит свои аргументы,
    // а на команды конфигуратора пишет список расширений базы в файл, который читает библиотека
    private WorkflowRun buildWithFakeVrunner(String script, List<String> infobaseExtensions, Result expectedResult) {
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project-extensions')
        workflowJob.definition = new CpsFlowDefinition(script.stripIndent(), true)

        FilePath workspace = rule.jenkins.getWorkspaceFor(workflowJob)
        workspace.child('jobConfiguration.json').write(CONFIGURATION, 'UTF-8')

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
