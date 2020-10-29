package ru.pulsar.jenkins.library.steps


import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig
import hudson.plugins.git.extensions.impl.GitLFSPull
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger

class EdtTransform implements Serializable {

    public static final String PROJECT_NAME = 'temp'
    public static final String WORKSPACE = 'build/edt-workspace'
    public static final String WORKSPACE_ZIP = 'build/edt-workspace.zip'
    public static final String WORKSPACE_ZIP_STASH = 'edt-workspace-zip'

    private final JobConfiguration config;

    EdtTransform(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.edtValidate) {
            Logger.println("EDT validate step is disabled. No transform is needed.")
            return
        }

        doSCM()

        def env = steps.env();

        def workspaceDir = "$env.WORKSPACE/$WORKSPACE"
        def configurationRoot = new File(env.WORKSPACE, config.srcDir).getAbsolutePath()

        steps.createDir(workspaceDir)

        Logger.println("Конвертация исходников из формата конфигуратора в формат EDT")

        def ringCommand = "ring edt workspace import --configuration-files '$configurationRoot' --project-name $PROJECT_NAME --workspace-location '$workspaceDir'"

        def ringOpts = ['RING_OPTS=-Dfile.encoding=UTF-8 -Dosgi.nl=ru -Duser.language=ru']
        steps.withEnv(ringOpts) {
            steps.cmd(ringCommand)
        }

        steps.zip(WORKSPACE, WORKSPACE_ZIP)
        steps.stash(WORKSPACE_ZIP_STASH, WORKSPACE_ZIP)
    }

    private void doSCM() {

        def gitSCMOptions = config.gitSCMOptions

        if (!gitSCMOptions.lfsPull) {
            return
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def scm = steps.scm()

        boolean needToCheckout = false
        needToCheckout = addLFS(scm, needToCheckout)
        scm = addLFSRemoteConfig(scm)

        if (needToCheckout) {
            steps.checkout(scm)
        }
    }

    private boolean addLFS(GitSCM scm, boolean needToCheckout) {
        GitLFSPull gitLFS = new GitLFSPull();
        def extensions = scm.getExtensions()
        if (!extensions.contains(gitLFS)) {
            needToCheckout = true
            extensions.add(gitLFS)
        }
        needToCheckout
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
