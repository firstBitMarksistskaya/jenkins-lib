package ru.pulsar.jenkins.library.steps

import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class Checkout implements Serializable {

    private final JobConfiguration config;

    Checkout(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        def gitSCMOptions = config.gitSCMOptions

        if (!gitSCMOptions.lfsPull) {
            return
        }

        def scm = steps.scm()

        scm = addLFSRemoteConfig(scm)

        steps.checkout(scm)

    }

    private GitSCM addLFSRemoteConfig(GitSCM scm) {
        def gitSCMOptions = config.gitSCMOptions

        if (gitSCMOptions.lfsURI.isEmpty()) {
            return scm
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        // TODO: get git.exe path from scm settings
        steps.cmd("git config -f .lfsconfig lfs.url $gitSCMOptions.lfsURI")

        List<UserRemoteConfig> userRemoteConfigs = new ArrayList<>(scm.getUserRemoteConfigs())

        if (gitSCMOptions.lfsRepoURI.isEmpty()) {
            return scm
        }

        def userRemoteConfig = userRemoteConfigs.find { it.url == gitSCMOptions.lfsRepoURI }
        boolean needToUpdateUserRemoteConfigs
        if (userRemoteConfig == null) {
            def credentialsId = config.secrets.lfs.isEmpty() ? null : config.secrets.lfs
            userRemoteConfig = new UserRemoteConfig(
                config.gitSCMOptions.lfsRepoURI,
                null,
                null,
                credentialsId
            )

            needToUpdateUserRemoteConfigs = true
        } else {
            def credentialsId = config.secrets.lfs.isEmpty() ? userRemoteConfig.credentialsId : config.secrets.lfs

            if (userRemoteConfig.credentialsId != credentialsId) {
                userRemoteConfig = new UserRemoteConfig(
                    userRemoteConfig.url,
                    null,
                    userRemoteConfig.refspec,
                    credentialsId
                )

                needToUpdateUserRemoteConfigs = true
            }
        }

        if (needToUpdateUserRemoteConfigs) {
            userRemoteConfigs.add(0, userRemoteConfig)
            scm = new GitSCM(
                userRemoteConfigs,
                scm.branches,
                scm.doGenerateSubmoduleConfigurations,
                scm.submoduleCfg,
                scm.browser,
                scm.gitTool,
                scm.extensions
            )
        }

        return scm
    }
}
