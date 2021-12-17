package ru.pulsar.jenkins.library.utils

class StringJoiner implements Serializable {
    private final String delimiter
    private StringBuilder value

    StringJoiner(CharSequence delimiter) {
        this.delimiter = delimiter
    }

    StringJoiner add(CharSequence newElement) {
        prepareBuilder().append(newElement)
        return this
    }

    int length() {
        return (value != null ? value.length() : 0)
    }

    @Override
    String toString() {
        if (value == null) {
            return ""
        } else {
            return value.toString()
        }
    }

    private StringBuilder prepareBuilder() {
        if (value != null) {
            value.append(delimiter)
        } else {
            value = new StringBuilder()
        }
        return value
    }
}
