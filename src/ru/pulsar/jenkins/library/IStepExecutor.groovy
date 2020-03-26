package ru.pulsar.jenkins.library

interface IStepExecutor {

    boolean isUnix()
    
    int sh(String script, boolean returnStatus, String encoding)
    
    int bat(String script, boolean returnStatus, String encoding)

}