package ru.pulsar.jenkins.library.utils

import com.cloudbees.groovy.cps.NonCPS

/**
 * Разбор строки подключения к информационной базе в формате --ibconnection:
 * /Fкаталог для файловой базы, /Sсервер\база или /Sсервер:порт\база для серверной.
 */
class InfobaseConnection implements Serializable {

    private static final String DEFAULT_CLUSTER_PORT = '1541'

    @NonCPS
    static boolean isServer(String connection) {
        return connection != null && connection.trim().toUpperCase().startsWith('/S')
    }

    // Каталог файловой базы из строки подключения /Fкаталог
    @NonCPS
    static String fileBaseDir(String connection) {
        return unquote(connection.trim().substring(2))
    }

    // Имя базы в кластере из строки подключения /Sсервер\база или /Sсервер:порт\база
    @NonCPS
    static String serverBaseName(String connection) {
        String serverAndBase = unquote(connection.trim().substring(2))
        return unquote(serverAndBase.substring(baseSeparator(serverAndBase) + 1))
    }

    // Порт кластера из строки подключения /Sсервер:порт\база, пустая строка, если порт не указан
    @NonCPS
    static String serverPort(String connection) {
        String server = servers(connection)[0]
        int portSeparator = server.lastIndexOf(':')
        return portSeparator >= 0 ? server.substring(portSeparator + 1).trim() : ''
    }

    /**
     * Имя ресурса блокировки базы: разные записи одного адреса дают одно имя. Регистр не важен, кавычки
     * убираются, порт кластера по умолчанию указывается явно. Для относительного пути файловой базы
     * возвращается null: на разных агентах такой путь означает разные базы, и блокировать его незачем.
     */
    @NonCPS
    static String lockResource(String connection) {
        if (isServer(connection)) {
            List<String> normalizedServers = servers(connection).collect { String server ->
                String host = server.trim().toLowerCase()
                return host.contains(':') ? host : "$host:$DEFAULT_CLUSTER_PORT".toString()
            }
            return "/S${normalizedServers.join(',')}\\${serverBaseName(connection).toLowerCase()}".toString()
        }

        String path = fileBaseDir(connection).replace('\\', '/')
        if (!path.startsWith('/') && !(path ==~ /^[A-Za-z]:.*/)) {
            return null
        }
        return "/F${path.replaceAll('/+$', '').toLowerCase()}".toString()
    }

    // Одна база при разных записях адреса. Относительный путь файловой базы сравнивается как записан
    @NonCPS
    static String key(String connection) {
        return lockResource(connection) ?: connection.trim()
    }

    // Серверы кластера из строки подключения /Sсервер1,сервер2\база
    @NonCPS
    private static List<String> servers(String connection) {
        String serverAndBase = unquote(connection.trim().substring(2))
        int separator = baseSeparator(serverAndBase)
        String servers = separator >= 0 ? serverAndBase.substring(0, separator) : serverAndBase
        return servers.split(',').toList()
    }

    @NonCPS
    private static int baseSeparator(String serverAndBase) {
        return Math.max(serverAndBase.lastIndexOf('\\'), serverAndBase.lastIndexOf('/'))
    }

    @NonCPS
    private static String unquote(String value) {
        String result = value.trim()
        if (result.length() >= 2 && result.startsWith('"') && result.endsWith('"')) {
            result = result.substring(1, result.length() - 1)
        }
        return result
    }
}
