package ru.pulsar.jenkins.library.utils;

import hudson.EnvVars;
import org.jenkinsci.plugins.workflow.support.actions.EnvironmentAction;

import java.io.IOException;

public class EnvUtils implements EnvironmentAction {

    public String NODE_NAME = "built-in";
    public String WORKSPACE = "ws";
    public String BRANCH_NAME = "master";

    @Override
    public EnvVars getEnvironment() throws IOException, InterruptedException {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }
}
