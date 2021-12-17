package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.email.EmailExtConfiguration
import ru.pulsar.jenkins.library.configuration.email.RecipientProvider
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.StringJoiner

class EmailNotification implements Serializable {

    private final JobConfiguration config;
    private final EmailExtConfiguration options

    EmailNotification(JobConfiguration config, EmailExtConfiguration options) {
        this.config = config
        this.options = options
    }

    def run() {

        Logger.printLocation()

        if (!config.stageFlags.email) {
            Logger.println("Email notifications are disabled")
            return
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        String subject = '$DEFAULT_SUBJECT'
        String body = '$DEFAULT_CONTENT'

        StringJoiner toJoiner = new StringJoiner(",")
        options.directRecipients.each {
            toJoiner.add(it)
        }
        String to = toJoiner.toString()

        List recipientProviders = new ArrayList();
        options.recipientProviders.each {
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
            options.attachLog
        )
    }
}
