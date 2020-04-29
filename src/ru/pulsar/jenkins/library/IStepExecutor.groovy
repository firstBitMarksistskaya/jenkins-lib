package ru.pulsar.jenkins.library

import org.jenkinsci.plugins.workflow.support.actions.EnvironmentAction

interface IStepExecutor {

    boolean isUnix()
    
    int sh(String script, boolean returnStatus, String encoding)
    
    int bat(String script, boolean returnStatus, String encoding)

    String libraryResource(String path)

    String readFile(String file, String encoding)

    void echo(message)

    void cmd(String script, boolean returnStatus)

    void cmd(String script)

    void tool(String toolName)

    void withSonarQubeEnv(String installationName, Closure body)

    EnvironmentAction env()

    void createDir(String path)

    def withEnv(List<String> strings, Closure body)

    def archiveArtifacts(String path)

    def stash(String name, String includes)

    def unstash(String name)

    def zip(String dir, String zipFile)

    def zip(String dir, String zipFile, String glob)

    def unzip(String dir, String zipFile)
    
    def unzip(String dir, String zipFile, quiet)
}