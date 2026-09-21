package ru.pulsar.jenkins.library.configuration.notification

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class TelegramNotificationOptions implements Serializable {

    @JsonPropertyDescription("Отправлять всегда")
    Boolean onAlways
    @JsonPropertyDescription("Отправлять при успешной сборке")
    Boolean onSuccess
    @JsonPropertyDescription("Отправлять при падении сборки")
    Boolean onFailure
    @JsonPropertyDescription("Отправлять при нестабильной сборке")
    Boolean onUnstable

    @JsonPropertyDescription("URL HTTP-прокси для запросов к api.telegram.org, например http://proxy.company.local:8080. Если не задан, запрос идёт напрямую")
    String httpProxy

    @JsonPropertyDescription("Идентификатор Jenkins credential типа Username with password для авторизации на HTTP-прокси")
    String proxyAuthentication

    @Override
    @NonCPS
    String toString() {
        return "TelegramNotificationOptions{" +
            "onAlways=" + onAlways +
            ", onSuccess=" + onSuccess +
            ", onFailure=" + onFailure +
            ", onUnstable=" + onUnstable +
            ", httpProxy='" + httpProxy + '\'' +
            ", proxyAuthentication='" + proxyAuthentication + '\'' +
            '}';
    }
}


