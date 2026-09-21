package ru.pulsar.jenkins.library.steps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.configuration.Secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.pulsar.jenkins.library.configuration.Secrets.UNKNOWN_ID;

class TelegramNotificationTest {

  private static final String SLUG = "org_repo";
  private static final String AUTO_CHAT = SLUG + "_TELEGRAM_CHAT_ID";

  @Test
  @DisplayName("main + задан telegramChatIdDefaultBranch → чат основной ветки")
  void defaultBranchUsesExplicitDefaultBranchChat() {
    Secrets secrets = secrets("other-chat", "main-chat");

    assertThat(TelegramNotification.resolveTelegramChatIdCredential(secrets, "main", "main", SLUG))
        .isEqualTo("main-chat");
  }

  @Test
  @DisplayName("feature при заданном втором чате → telegramChatId")
  void featureBranchUsesOtherChatWhenSplitEnabled() {
    Secrets secrets = secrets("other-chat", "main-chat");

    assertThat(TelegramNotification.resolveTelegramChatIdCredential(secrets, "feature/foo", "main", SLUG))
        .isEqualTo("other-chat");
  }

  @Test
  @DisplayName("PR-* при заданном втором чате → telegramChatId")
  void pullRequestUsesOtherChatWhenSplitEnabled() {
    Secrets secrets = secrets("other-chat", "main-chat");

    assertThat(TelegramNotification.resolveTelegramChatIdCredential(secrets, "PR-12", "main", SLUG))
        .isEqualTo("other-chat");
  }

  @Test
  @DisplayName("UNKNOWN_ID второго чата → авто {slug}_TELEGRAM_CHAT_ID на любой ветке")
  void unknownDefaultBranchChatFallsBackToAutoId() {
    Secrets secrets = secrets(UNKNOWN_ID, UNKNOWN_ID);

    assertThat(TelegramNotification.resolveTelegramChatIdCredential(secrets, "main", "main", SLUG))
        .isEqualTo(AUTO_CHAT);
    assertThat(TelegramNotification.resolveTelegramChatIdCredential(secrets, "feature/foo", "main", SLUG))
        .isEqualTo(AUTO_CHAT);
  }

  @Test
  @DisplayName("пустая строка второго чата → как сейчас, все ветки в telegramChatId")
  void emptyDefaultBranchChatFallsBackToOtherChat() {
    Secrets secrets = secrets("other-chat", "");

    assertThat(TelegramNotification.resolveTelegramChatIdCredential(secrets, "main", "main", SLUG))
        .isEqualTo("other-chat");
    assertThat(TelegramNotification.resolveTelegramChatIdCredential(secrets, "feature/foo", "main", SLUG))
        .isEqualTo("other-chat");
  }

  @Test
  @DisplayName("null второго чата → все ветки в telegramChatId")
  void nullDefaultBranchChatFallsBackToOtherChat() {
    Secrets secrets = secrets("other-chat", null);

    assertThat(TelegramNotification.resolveTelegramChatIdCredential(secrets, "main", "main", SLUG))
        .isEqualTo("other-chat");
  }

  @Test
  @DisplayName("тестовый EnvUtils BRANCH_NAME=master не совпадает с defaultBranch=main")
  void masterIsNotDefaultMain() {
    Secrets secrets = secrets("other-chat", "main-chat");

    assertThat(TelegramNotification.resolveTelegramChatIdCredential(secrets, "master", "main", SLUG))
        .isEqualTo("other-chat");
  }

  @Test
  @DisplayName("пустой BRANCH_NAME при заданном втором чате → telegramChatId")
  void emptyBranchNameUsesOtherChat() {
    Secrets secrets = secrets("other-chat", "main-chat");

    assertThat(TelegramNotification.resolveTelegramChatIdCredential(secrets, "", "main", SLUG))
        .isEqualTo("other-chat");
    assertThat(TelegramNotification.resolveTelegramChatIdCredential(secrets, null, "main", SLUG))
        .isEqualTo("other-chat");
  }

  private static Secrets secrets(String telegramChatId, String telegramChatIdDefaultBranch) {
    Secrets secrets = new Secrets();
    secrets.setTelegramChatId(telegramChatId);
    secrets.setTelegramChatIdDefaultBranch(telegramChatIdDefaultBranch);
    return secrets;
  }
}
