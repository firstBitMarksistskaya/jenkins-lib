package ru.pulsar.jenkins.library.steps

import groovy.json.JsonSlurper
import groovy.lang.Binding
import groovy.lang.GroovyShell
import groovy.lang.Script
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Files
import java.nio.file.Path

import static org.assertj.core.api.Assertions.assertThat

class DebugOverridesStepsTest {

    @TempDir
    Path tempDir

    @Test
    void applyDebugOverridesIfNeeded_appliesAndRestoreDebugOverridesIfNeeded_restoresManagedFiles() {
        Map<String, String> env = [JOB_NAME: 'CPC/ci_uh_MR/MR-1101']
        List<String> logs = []
        Map<String, Map<String, String>> stashes = [:]
        Map<String, String> managedFiles = [
            'jenkins-debug-overrides-control': '''
                {
                  "profiles": {
                    "ci_uh_MR": {
                      "enabled": true,
                      "replacements": [
                        {
                          "fileId": "debug-job-configuration",
                          "target": "jobConfiguration.json"
                        },
                        {
                          "fileId": "debug-vrunner-settings",
                          "target": "tools/vrunner.json"
                        }
                      ]
                    }
                  }
                }
            '''.stripIndent().trim(),
            'debug-job-configuration': '{"stages":{"bdd":false,"smoke":false,"yaxunit":false,"syntaxCheck":false,"sonarqube":false,"edtValidate":false}}',
            'debug-vrunner-settings': '{"settings":"debug"}'
        ]

        Script applyScript = loadScript('vars/applyDebugOverridesIfNeeded.groovy', env)
        configureStepScript(applyScript, managedFiles, stashes, logs)

        boolean applied = (boolean) applyScript.invokeMethod('call', null)

        assertThat(applied).isTrue()
        assertThat(logs).contains('Debug overrides: applying 2 replacement(s)')
        assertThat(logs).contains('Debug overrides: stashed files for downstream agents')
        assertThat(readFile('jobConfiguration.json')).contains('"stages"')
        assertThat(readFile('tools/vrunner.json')).isEqualTo('{"settings":"debug"}')

        writeFile('tools/vrunner.json', '{"settings":"stale"}')

        Script restoreScript = loadScript('vars/restoreDebugOverridesIfNeeded.groovy', env)
        configureStepScript(restoreScript, managedFiles, stashes, logs)

        boolean restored = (boolean) restoreScript.invokeMethod('call', null)

        assertThat(restored).isTrue()
        assertThat(logs).contains('Debug overrides: restored files from stash')
        assertThat(readFile('tools/vrunner.json')).isEqualTo('{"settings":"debug"}')
    }

    @Test
    void applyDebugOverridesIfNeeded_skipsInvalidProfileConfigWithoutFailing() {
        Map<String, String> env = [JOB_NAME: 'CPC/ci_uh_MR/MR-1101']
        List<String> logs = []
        Map<String, Map<String, String>> stashes = [:]
        Map<String, String> managedFiles = [
            'jenkins-debug-overrides-control': '''
                {
                  "profiles": {
                    "ci_uh_MR": {
                      "enabled": true,
                      "replacements": {
                        "fileId": "debug-job-configuration",
                        "target": "jobConfiguration.json"
                      }
                    }
                  }
                }
            '''.stripIndent().trim()
        ]

        Script applyScript = loadScript('vars/applyDebugOverridesIfNeeded.groovy', env)
        configureStepScript(applyScript, managedFiles, stashes, logs)

        boolean applied = (boolean) applyScript.invokeMethod('call', null)

        assertThat(applied).isFalse()
        assertThat(logs.find { it.startsWith('Debug overrides: invalid profile ci_uh_MR, skip') }).isNotNull()
    }

    @Test
    void applyDebugOverridesIfNeeded_skipsInvalidControlFileWithoutFailing() {
        Map<String, String> env = [JOB_NAME: 'CPC/ci_uh_MR/MR-1101']
        List<String> logs = []
        Map<String, Map<String, String>> stashes = [:]
        Map<String, String> managedFiles = [
            'jenkins-debug-overrides-control': '{ invalid json'
        ]

        Script applyScript = loadScript('vars/applyDebugOverridesIfNeeded.groovy', env)
        configureStepScript(applyScript, managedFiles, stashes, logs)

        boolean applied = (boolean) applyScript.invokeMethod('call', null)

        assertThat(applied).isFalse()
        assertThat(logs.find { it.startsWith('Debug overrides: control file is invalid, skip') }).isNotNull()
    }

    private Script loadScript(String path, Map<String, String> env) {
        Binding binding = new Binding()
        binding.setVariable('env', env)
        GroovyShell shell = new GroovyShell(this.class.classLoader, binding)
        return shell.parse(new File(path))
    }

    private void configureStepScript(
        Script script,
        Map<String, String> managedFiles,
        Map<String, Map<String, String>> stashes,
        List<String> logs
    ) {
        script.metaClass.echo = { String message ->
            logs.add(message)
        }
        script.metaClass.createDir = { String path ->
            Files.createDirectories(tempDir.resolve(path))
        }
        script.metaClass.readFile = { Map args ->
            String encoding = (args.encoding ?: 'UTF-8') as String
            tempDir.resolve(args.file as String).toFile().getText(encoding)
        }
        script.metaClass.writeFile = { Map args ->
            Path target = tempDir.resolve(args.file as String)
            Files.createDirectories(target.parent ?: tempDir)
            target.toFile().setText(args.text as String, (args.encoding ?: 'UTF-8') as String)
        }
        script.metaClass.readJSON = { Map args ->
            new JsonSlurper().parse(tempDir.resolve(args.file as String).toFile())
        }
        script.metaClass.configFile = { Map args ->
            args
        }
        script.metaClass.configFileProvider = { List entries, Closure body ->
            Files.createDirectories(tempDir.resolve('.managed'))

            entries.each { Map entry ->
                String fileId = entry.fileId as String
                String variable = entry.variable as String
                String content = managedFiles[fileId]

                if (content == null) {
                    throw new RuntimeException("Managed file with id ${fileId} not found")
                }

                Path managedFile = tempDir.resolve(".managed/${variable}.txt")
                managedFile.toFile().setText(content, 'UTF-8')
                script.binding.getVariable('env')[variable] = tempDir.relativize(managedFile).toString().replace('\\', '/')
            }

            body.call()
        }
        script.metaClass.stash = { Map args ->
            Map<String, String> files = [:]
            (args.includes as String).split(',').each { String include ->
                files[include] = readFile(include)
            }
            stashes[args.name as String] = files
        }
        script.metaClass.unstash = { String name ->
            Map<String, String> files = stashes[name]
            if (files == null) {
                throw new RuntimeException("No such saved stash '${name}'")
            }

            files.each { String path, String content ->
                writeFile(path, content)
            }
        }
    }

    private String readFile(String relativePath) {
        tempDir.resolve(relativePath).toFile().getText('UTF-8')
    }

    private void writeFile(String relativePath, String content) {
        Path target = tempDir.resolve(relativePath)
        Files.createDirectories(target.parent ?: tempDir)
        target.toFile().setText(content, 'UTF-8')
    }
}
