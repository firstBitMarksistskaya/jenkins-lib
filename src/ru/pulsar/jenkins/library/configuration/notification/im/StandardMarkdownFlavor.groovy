package ru.pulsar.jenkins.library.configuration.notification.im

import com.cloudbees.groovy.cps.NonCPS

/**
 * Разметка по спецификации CommonMark с расширениями GFM.
 *
 * Экранируются только символы, которые CommonMark или GFM могут трактовать как разметку:
 * {@code \ ` * _ [ ] ( ) ! ~ | < > # + - = .}
 *
 * Символы {@code &#123;} и {@code &#125;} намеренно не экранируются: их требует экранировать
 * MarkdownV2 у Telegram (см. {@link MarkdownV2Flavor}), но в CommonMark у них нет
 * никакого значения.
 */
class StandardMarkdownFlavor implements MarkdownFlavor {

    @Override
    @NonCPS
    String escape(String text) {
        if (text == null) {
            return null
        }
        return text
            .replace('\\', '\\\\')
            .replace('_', '\\_')
            .replace('*', '\\*')
            .replace('`', '\\`')
            .replace('[', '\\[')
            .replace(']', '\\]')
            .replace('(', '\\(')
            .replace(')', '\\)')
            .replace('!', '\\!')
            .replace('~', '\\~')
            .replace('|', '\\|')
            .replace('<', '\\<')
            .replace('>', '\\>')
            .replace('#', '\\#')
            .replace('+', '\\+')
            .replace('-', '\\-')
            .replace('=', '\\=')
            .replace('.', '\\.')
    }

    @Override
    @NonCPS
    String bullet() {
        return '*'
    }

    @Override
    @NonCPS
    String hash() {
        return '#'
    }

    @Override
    @NonCPS
    String openParen() {
        return '('
    }

    @Override
    @NonCPS
    String closeParen() {
        return ')'
    }

    @Override
    @NonCPS
    String link(String text, String url) {
        if (url == null) {
            return escape(text)
        }
        return "[${escape(text)}](${escapeUrl(url)})"
    }

    @NonCPS
    private static String escapeUrl(String url) {
        if (url == null) {
            return null
        }
        return url
            .replace('\\', '\\\\')
            .replace(')', '\\)')
    }
}
