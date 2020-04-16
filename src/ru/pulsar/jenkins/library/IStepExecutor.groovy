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

    void cmd(String gString)

    void tool(String toolName)

    void withSonarQubeEnv(String installationName, Closure body)

    EnvironmentAction env()
}