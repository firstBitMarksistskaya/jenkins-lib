package ru.pulsar.jenkins.library.configuration.notification

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class IMNotificationOptions implements Serializable {

    @JsonPropertyDescription("Отправлять всегда")
    Boolean onAlways
    @JsonPropertyDescription("Отправлять при успешной сборке")
    Boolean onSuccess
    @JsonPropertyDescription("Отправлять при падении сборки")
    Boolean onFailure
    @JsonPropertyDescription("Отправлять при нестабильной сборке")
    Boolean onUnstable

    @JsonPropertyDescription("Отправлять через HTTP-прокси. Адрес и авторизация берутся из фиксированных Jenkins credentials TELEGRAM_HTTP_PROXY (secret text) и TELEGRAM_HTTP_PROXY_AUTH (username with password), в jobConfiguration не указываются")
    Boolean useHttpProxy

    @JsonPropertyDescription("Отправлять сборки прочих веток в отдельный чат. Chat id берётся из фиксированного Jenkins credential TELEGRAM_CHAT_ID_OTHER_BRANCHES (secret text), в jobConfiguration не указывается. Основная ветка (defaultBranch) и сборки без BRANCH_NAME всегда идут в исходный telegramChatId")
    Boolean useOtherBranchesChat

    @Override
    @NonCPS
    String toString() {
        return "IMNotificationOptions{" +
            "onAlways=" + onAlways +
            ", onSuccess=" + onSuccess +
            ", onFailure=" + onFailure +
            ", onUnstable=" + onUnstable +
            ", useHttpProxy=" + useHttpProxy +
            ", useOtherBranchesChat=" + useOtherBranchesChat +
            '}';
    }
}
