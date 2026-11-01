package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class GlobalCoverageOptions implements Serializable {
    
    @JsonPropertyDescription('''Путь к исполняемому файлу dbgs.
    По умолчанию ищется с помощью v8find для указанной версии платформы (v8version).
    ''')
    String dbgsPath

    @JsonPropertyDescription('''Путь к исполняемому файлу Coverage41C
    По умолчанию ищется в PATH.
    ''')
    String coverage41CPath

    @JsonPropertyDescription('''Адрес сервера отладки кластера для замеров покрытия на серверной базе: имя или адрес сервера 1С.
    Серверный код серверной базы выполняется в кластере, его видит только сервер отладки, который поднимает
    агент сервера 1С, запущенный с ключами -debug -http. Порт задает -debugServerPort, по умолчанию 1550;
    если он другой, укажите его через двоеточие: хост:порт.
    Для серверной базы с включенными замерами обязателен.
    ''')
    String clusterDebugServer

    @Override
    @NonCPS
    String toString() {
        return "coverageOptions{" +
                "dbgsPath=" + dbgsPath +
                ", coverage41CPath=" + coverage41CPath +
                ", clusterDebugServer=" + clusterDebugServer +
                '}'
    }

}
