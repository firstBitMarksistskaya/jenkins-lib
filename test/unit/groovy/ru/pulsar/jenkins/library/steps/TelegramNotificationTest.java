package ru.pulsar.jenkins.library.steps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.configuration.Secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.pulsar.jenkins.library.configuration.Secrets.UNKNOWN_ID;

class TelegramNotificationTest {

  private static final String SLUG = "org_repo";
  private static final String AUTO_CHAT = SLUG + "_TELEGRAM_CHAT_ID";
  private static final String DEFAULT_BRANCH = "main";
  private static final String ORIGINAL_CHAT = "existing-chat";

  @Test
  @DisplayName("без сплита все ветки идут в исходный telegramChatId")
  void disabledSplitUsesOriginalChatOnEveryBranch() {
    Secrets secrets = secrets(ORIGINAL_CHAT);

    assertThat(resolve(secrets, "main", false)).isEqualTo(ORIGINAL_CHAT);
    assertThat(resolve(secrets, "feature/foo", false)).isEqualTo(ORIGINAL_CHAT);
    assertThat(resolve(secrets, "PR-12", false)).isEqualTo(ORIGINAL_CHAT);
    assertThat(resolve(secrets, null, false)).isEqualTo(ORIGINAL_CHAT);
  }

  @Test
  @DisplayName("сплит: defaultBranch остаётся в исходном чате")
  void defaultBranchStaysOnOriginalChatWhenSplitEnabled() {
    Secrets secrets = secrets(ORIGINAL_CHAT);

    assertThat(resolve(secrets, "main", true)).isEqualTo(ORIGINAL_CHAT);
  }

  @Test
  @DisplayName("сплит: feature → фиксированный TELEGRAM_CHAT_ID_OTHER_BRANCHES")
  void featureBranchUsesOtherBranchesChatWhenSplitEnabled() {
    Secrets secrets = secrets(ORIGINAL_CHAT);

    assertThat(resolve(secrets, "feature/foo", true))
        .isEqualTo(TelegramNotification.TELEGRAM_CHAT_ID_OTHER_BRANCHES_CREDENTIAL_ID);
  }

  @Test
  @DisplayName("сплит: PR-* → TELEGRAM_CHAT_ID_OTHER_BRANCHES")
  void pullRequestUsesOtherBranchesChatWhenSplitEnabled() {
    Secrets secrets = secrets(ORIGINAL_CHAT);

    assertThat(resolve(secrets, "PR-12", true))
        .isEqualTo(TelegramNotification.TELEGRAM_CHAT_ID_OTHER_BRANCHES_CREDENTIAL_ID);
  }

  @Test
  @DisplayName("UNKNOWN_ID без сплита → авто {slug}_TELEGRAM_CHAT_ID на любой ветке")
  void unknownOriginalChatFallsBackToAutoId() {
    Secrets secrets = secrets(UNKNOWN_ID);

    assertThat(resolve(secrets, "main", false)).isEqualTo(AUTO_CHAT);
    assertThat(resolve(secrets, "feature/foo", false)).isEqualTo(AUTO_CHAT);
  }

  @Test
  @DisplayName("UNKNOWN_ID при сплите: main → авто исходный, feature → TELEGRAM_CHAT_ID_OTHER_BRANCHES")
  void unknownOriginalChatStillSplitsOtherBranches() {
    Secrets secrets = secrets(UNKNOWN_ID);

    assertThat(resolve(secrets, "main", true)).isEqualTo(AUTO_CHAT);
    assertThat(resolve(secrets, "feature/foo", true))
        .isEqualTo(TelegramNotification.TELEGRAM_CHAT_ID_OTHER_BRANCHES_CREDENTIAL_ID);
  }

  @Test
  @DisplayName("master не совпадает с defaultBranch=main → чат прочих веток")
  void masterIsNotDefaultMain() {
    Secrets secrets = secrets(ORIGINAL_CHAT);

    assertThat(resolve(secrets, "master", true))
        .isEqualTo(TelegramNotification.TELEGRAM_CHAT_ID_OTHER_BRANCHES_CREDENTIAL_ID);
  }

  @Test
  @DisplayName("пустой BRANCH_NAME при сплите → исходный чат (как до доработки)")
  void emptyBranchNameStaysOnOriginalChat() {
    Secrets secrets = secrets(ORIGINAL_CHAT);

    assertThat(resolve(secrets, "", true)).isEqualTo(ORIGINAL_CHAT);
    assertThat(resolve(secrets, null, true)).isEqualTo(ORIGINAL_CHAT);
  }

  @Test
  @DisplayName("имена credentials прокси и чата прочих веток фиксированы")
  void telegramCredentialIdsAreFixed() {
    assertThat(TelegramNotification.TELEGRAM_HTTP_PROXY_CREDENTIAL_ID)
        .isEqualTo("TELEGRAM_HTTP_PROXY");
    assertThat(TelegramNotification.TELEGRAM_HTTP_PROXY_AUTH_CREDENTIAL_ID)
        .isEqualTo("TELEGRAM_HTTP_PROXY_AUTH");
    assertThat(TelegramNotification.TELEGRAM_CHAT_ID_OTHER_BRANCHES_CREDENTIAL_ID)
        .isEqualTo("TELEGRAM_CHAT_ID_OTHER_BRANCHES");
  }

  private static String resolve(Secrets secrets, String branch, boolean useOtherBranchesChat) {
    return TelegramNotification.resolveTelegramChatIdCredential(
        secrets,
        branch,
        DEFAULT_BRANCH,
        SLUG,
        useOtherBranchesChat
    );
  }

  private static Secrets secrets(String telegramChatId) {
    Secrets secrets = new Secrets();
    secrets.setTelegramChatId(telegramChatId);
    return secrets;
  }
}
