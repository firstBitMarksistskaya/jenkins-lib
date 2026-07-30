package ru.pulsar.jenkins.library.configuration.notification.im

import com.cloudbees.groovy.cps.NonCPS

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
}
