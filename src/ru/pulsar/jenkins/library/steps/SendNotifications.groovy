package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.notification.im.DiscordBotMessenger
import ru.pulsar.jenkins.library.configuration.notification.im.DiscordWebhookMessenger
import ru.pulsar.jenkins.library.configuration.notification.im.MaxMessenger
import ru.pulsar.jenkins.library.configuration.notification.im.Messenger
import ru.pulsar.jenkins.library.configuration.notification.im.TelegramMessenger
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

        List<Messenger> messengers = [
            new TelegramMessenger(),
            new MaxMessenger(),
            new DiscordWebhookMessenger(),
            new DiscordBotMessenger()
        ]

        messengers.each { messenger ->
            try {
                new IMNotification(config, messenger).run()
            } catch (InterruptedException e) {
                throw e
            } catch (Exception e) {
                Logger.println("Failed to send ${messenger.name()} notification: ${e.message}")
            }
        }

    }
}
