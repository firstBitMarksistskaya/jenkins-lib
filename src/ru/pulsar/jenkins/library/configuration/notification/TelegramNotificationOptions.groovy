package ru.pulsar.jenkins.library.configuration.notification

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class TelegramNotificationOptions extends IMNotificationOptions implements Serializable {

    public static final String OPTIONS_KEY = "telegram"

    @JsonPropertyDescription("Отправлять через HTTP-прокси. Адрес и авторизация берутся из фиксированных Jenkins credentials TELEGRAM_HTTP_PROXY (secret text) и TELEGRAM_HTTP_PROXY_AUTH (username with password), в jobConfiguration не указываются")
    Boolean useHttpProxy

    @JsonPropertyDescription("Отправлять сборки прочих веток в отдельный чат. Chat id — secrets.telegramChatIdOtherBranches, при UNKNOWN_ID {slug}_TELEGRAM_CHAT_ID_OTHER_BRANCHES. Основная ветка (defaultBranch) и сборки без BRANCH_NAME всегда идут в telegramChatId")
    Boolean useOtherBranchesChat

    @Override
    @NonCPS
    String toString() {
        return "TelegramNotificationOptions{" +
            "onAlways=" + onAlways +
            ", onSuccess=" + onSuccess +
            ", onFailure=" + onFailure +
            ", onUnstable=" + onUnstable +
            ", useHttpProxy=" + useHttpProxy +
            ", useOtherBranchesChat=" + useOtherBranchesChat +
            '}';
    }
}
