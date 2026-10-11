package ru.pulsar.jenkins.library.utils

/**
 * Разбор строки подключения к информационной базе в формате --ibconnection:
 * /Fкаталог для файловой базы, /Sсервер\база или /Sсервер:порт\база для серверной.
 */
class InfobaseConnection implements Serializable {

    static boolean isServer(String connection) {
        return connection != null && connection.trim().toUpperCase().startsWith('/S')
    }

    // Каталог файловой базы из строки подключения /Fкаталог
    static String fileBaseDir(String connection) {
        return unquote(connection.trim().substring(2))
    }

    // Имя базы в кластере из строки подключения /Sсервер\база или /Sсервер:порт\база
    static String serverBaseName(String connection) {
        String serverAndBase = unquote(connection.trim().substring(2))
        return unquote(serverAndBase.substring(baseSeparator(serverAndBase) + 1))
    }

    // Порт кластера из строки подключения /Sсервер:порт\база, пустая строка, если порт не указан
    static String serverPort(String connection) {
        String serverAndBase = unquote(connection.trim().substring(2))
        int separator = baseSeparator(serverAndBase)
        String servers = separator >= 0 ? serverAndBase.substring(0, separator) : serverAndBase
        String server = servers.split(',')[0]
        int portSeparator = server.lastIndexOf(':')
        return portSeparator >= 0 ? server.substring(portSeparator + 1).trim() : ''
    }

    private static int baseSeparator(String serverAndBase) {
        return Math.max(serverAndBase.lastIndexOf('\\'), serverAndBase.lastIndexOf('/'))
    }

    private static String unquote(String value) {
        String result = value.trim()
        if (result.length() >= 2 && result.startsWith('"') && result.endsWith('"')) {
            result = result.substring(1, result.length() - 1)
        }
        return result
    }
}
