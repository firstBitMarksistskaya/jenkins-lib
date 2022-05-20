package ru.pulsar.jenkins.library.steps

import hudson.model.Result
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.notification.email.EmailExtConfiguration
import ru.pulsar.jenkins.library.configuration.notification.email.RecipientProvider
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.StringJoiner

class EmailNotification implements Serializable {

    private final JobConfiguration config;

    EmailNotification(JobConfiguration config) {
        this.config = config
    }

    def run() {

        Logger.printLocation()

        if (!config.stageFlags.email) {
            Logger.println("Email notifications are disabled")
            return
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def options = config.notificationsOptions.emailNotificationOptions

        def currentBuild = steps.currentBuild()
        def currentResult = Result.fromString(currentBuild.getCurrentResult())

        EmailExtConfiguration configuration = null;
        if (options.onAlways) {
            configuration = options.alwaysEmailOptions
        } else if (options.onFailure && (currentResult == Result.FAILURE || currentResult == Result.ABORTED)) {
            configuration = options.failureEmailOptions
        } else if (options.onUnstable && currentResult == Result.UNSTABLE) {
            configuration = options.unstableEmailOptions
        } else if (options.onSuccess && currentResult == Result.SUCCESS) {
            configuration = options.successEmailOptions
        }

        sendEmail(configuration)

    }

    private static void sendEmail(EmailExtConfiguration configuration) {

        if (configuration == null) {
            Logger.println("Unknown build result. Can't send an email!")
            return
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String subject = '$DEFAULT_SUBJECT'
        String body = '$DEFAULT_CONTENT'

        StringJoiner toJoiner = new StringJoiner(",")
        configuration.directRecipients.each {
            toJoiner.add(it)
        }
        String to = toJoiner.toString()

        List recipientProviders = new ArrayList();
        configuration.recipientProviders.each {
            switch (it) {
                case RecipientProvider.BROKEN_BUILD_SUSPECTS:
                    recipientProviders.add(steps.brokenBuildSuspects())
                    break
                case RecipientProvider.BROKEN_TESTS_SUSPECTS:
                    recipientProviders.add(steps.brokenTestsSuspects())
                    break
                case RecipientProvider.DEVELOPERS:
                    recipientProviders.add(steps.developers())
                    break
                case RecipientProvider.REQUESTOR:
                    recipientProviders.add(steps.requestor())
                    break
            }
        }

        steps.emailext(
            subject,
            body,
            to,
            recipientProviders,
            configuration.attachLog
        )
    }
}
