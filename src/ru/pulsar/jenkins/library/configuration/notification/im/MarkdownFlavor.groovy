package ru.pulsar.jenkins.library.configuration.notification.im

interface MarkdownFlavor extends Serializable {

    String escape(String text)

    String bullet()

    String hash()

    String openParen()

    String closeParen()

    String link(String text, String url)
}
