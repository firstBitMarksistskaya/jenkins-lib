package ru.pulsar.jenkins.library.steps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IMNotificationTruncateTest {

  @Test
  @DisplayName("text within the limit is unchanged")
  void shortTextIsUnchanged() {
    assertThat(IMNotification.truncate("ok", 10)).isEqualTo("ok");
    assertThat(IMNotification.truncate(null, 10)).isNull();
  }

  @Test
  @DisplayName("cut keeps the last whole line and does not use three dots")
  void cutsAtLastWholeLine() {
    String text = "line-one\nline-two\nline-three-is-longer";
    String result = IMNotification.truncate(text, 19);

    assertThat(result).isEqualTo("line-one\nline-two\n\u2026");
    assertThat(result).doesNotContain("...");
  }

  @Test
  @DisplayName("a long line drops the unclosed MarkdownV2 link instead of splitting the URL")
  void dropsUnclosedLinkOnALongLine() {
    String text = "msg [author](http://localhost:8080/user/kropachev)";
    int limit = "msg [author](http://local".length();

    String result = IMNotification.truncate(text, limit);

    assertThat(result).isEqualTo("msg\n\u2026");
    assertThat(result).doesNotContain("](");
    assertThat(result.length()).isLessThanOrEqualTo(limit);
  }

  @Test
  @DisplayName("changeset tail keeps complete commit lines")
  void keepsCompleteChangesetLines() {
    String line = "\\* [e1e9d5c](https://github.com/example/commit/e1e9d5c) fix \\([user](http://localhost/user/name)\n";
    String text = line + line + "partial";
    int limit = line.length() + 4;

    String result = IMNotification.truncate(text, limit);

    assertThat(result).isEqualTo(line.substring(0, line.length() - 1) + "\n\u2026");
    assertThat(result).doesNotContain("kropac");
    assertThat(result).contains("](http://localhost/user/name)");
  }
}
