package ru.pulsar.jenkins.library.utils

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurper
import hudson.FilePath

/**
 * Значения из файла настроек vanessa-runner (--settings). Как в vanessa-runner, блок команды важнее блока default.
 * Файл читается без сохранения содержимого в состоянии Pipeline: в нем может быть пароль.
 */
class RunnerSettings implements Serializable {

    private static final String BOM = String.valueOf((char) 0xFEFF)

    /**
     * Значение параметра команды или null, если файла, блока или параметра нет.
     * @param settingsPath абсолютный путь к файлу настроек на агенте
     */
    static String value(String settingsPath, String command, String key) {
        FilePath file = FileUtils.getFilePath(settingsPath)
        return file.exists() ? parseValue(file, command, key) : null
    }

    @NonCPS
    private static String parseValue(FilePath file, String command, String key) {
        def settings = new JsonSlurper().parseText(read(file))
        if (!(settings instanceof Map)) {
            return null
        }
        for (String section : [command, 'default']) {
            def block = settings[section]
            if (block instanceof Map && block[key] != null) {
                return block[key].toString()
            }
        }
        return null
    }

    /**
     * Текст файла на агенте в UTF-8 без BOM. FilePath.readToString() читает в кодировке агента по умолчанию,
     * на Windows это не UTF-8
     */
    @NonCPS
    static String read(FilePath file) {
        return file.read().getText('UTF-8').replace(BOM, '')
    }

    /** Аргумент именованного параметра в командной строке vanessa-runner без кавычек или null */
    @NonCPS
    static String argument(String command, String name) {
        List<String> tokens = tokens(command)
        int index = tokens.indexOf(name)
        return index >= 0 && index + 1 < tokens.size() ? tokens[index + 1] : null
    }

    // Аргументы командной строки: в двойных кавычках или до пробела, кавычки убираются
    @NonCPS
    static List<String> tokens(String command) {
        List<String> tokens = []
        def matcher = command =~ /"([^"]*)"|(\S+)/
        while (matcher.find()) {
            tokens.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2))
        }
        return tokens
    }
}
