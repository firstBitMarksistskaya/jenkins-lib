package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.notification.im.DiscordBotMessenger
import ru.pulsar.jenkins.library.configuration.notification.im.DiscordWebhookMessenger
import ru.pulsar.jenkins.library.configuration.notification.im.MaxMessenger
import ru.pulsar.jenkins.library.utils.Logger

class SendNotifications implements Serializable {

    private final JobConfiguration config;

    SendNotifications(JobConfiguration config) {
        this.config = config
    }

    def run() {

        Logger.printLocation()

        if (config == null) {
            Logger.println("jobConfiguration is not initialized")
            return
        }

        def emailNotification = new EmailNotification(config);
        emailNotification.run()

        def telegramNotification = new TelegramNotification(config);
        telegramNotification.run();

        new IMNotification(config, new MaxMessenger()).run()

        new IMNotification(config, new DiscordWebhookMessenger()).run()

        new IMNotification(config, new DiscordBotMessenger()).run()

    }
}
