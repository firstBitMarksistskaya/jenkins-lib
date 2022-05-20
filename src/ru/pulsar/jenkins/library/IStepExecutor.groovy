package ru.pulsar.jenkins.library

import jenkins.plugins.http_request.HttpMode
import jenkins.plugins.http_request.MimeType
import jenkins.plugins.http_request.ResponseContentSupplier
import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper
import org.jenkinsci.plugins.workflow.support.actions.EnvironmentAction
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

interface IStepExecutor {

    boolean isUnix()
    
    int sh(String script, boolean returnStatus, String encoding)
    
    int bat(String script, boolean returnStatus, String encoding)

    String libraryResource(String path)

    FileWrapper[] findFiles(String glob)

    FileWrapper[] findFiles(String glob, String excludes)

    String readFile(String file)

    String readFile(String file, String encoding)

    boolean fileExists(String file)

    void echo(message)

    int cmd(String script, boolean returnStatus)

    int cmd(String script)

    void tool(String toolName)

    def withCredentials(List bindings, Closure body)

    def string(String credentialsId, String variable)

    def usernamePassword(String credentialsId, String usernameVariable, String passwordVariable)

    void withSonarQubeEnv(String installationName, Closure body)

    EnvironmentAction env()

    def dir(String path, Closure body)

    void createDir(String path)

    void deleteDir()

    void deleteDir(String path)

    def withEnv(List<String> strings, Closure body)

    def archiveArtifacts(String path)

    def stash(String name, String includes)

    def stash(String name, String includes, boolean allowEmpty)

    def unstash(String name)

    def zip(String dir, String zipFile)

    def zip(String dir, String zipFile, String glob)

    def unzip(String dir, String zipFile)

    def unzip(String dir, String zipFile, quiet)

    def catchError(Closure body)

    ResponseContentSupplier httpRequest(String url, String outputFile, String responseHandle, boolean wrapAsMultipart)

    ResponseContentSupplier httpRequest(String url, HttpMode httpMode, MimeType contentType, String requestBody, String validResponseCodes, boolean consoleLogResponseBody)

    def error(String errorMessage)

    def allure(List<String> results)

    def junit(String testResults, boolean allowEmptyResults)

    def installLocalDependencies()

    def emailext(String subject, String body, String to, List recipientProviders, boolean attachLog)

    def developers()

    def requestor()

    def brokenBuildSuspects()

    def brokenTestsSuspects()

    RunWrapper currentBuild()
}