package ru.pulsar.jenkins.library.steps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IMNotificationTruncateTest {

  @Test
  @DisplayName("короткий текст и null не меняются")
  void shortTextIsUnchanged() {
    assertThat(IMNotification.truncate("ok", 10)).isEqualTo("ok");
    assertThat(IMNotification.truncate(null, 10)).isNull();
  }

  @Test
  @DisplayName("лимит короче суффикса обрезает сырой префикс")
  void tinyLimitReturnsRawPrefix() {
    assertThat(IMNotification.truncate("abcdef", 1)).isEqualTo("a");
    assertThat(IMNotification.truncate("abcdef", 0)).isEmpty();
  }

  @Test
  @DisplayName("обрезка держит последнюю целую строку и не ставит три точки")
  void cutsAtLastWholeLine() {
    String text = "line-one\nline-two\nline-three-is-longer";
    String result = IMNotification.truncate(text, 19);

    assertThat(result).isEqualTo("line-one\nline-two\n\u2026");
    assertThat(result).doesNotContain("...");
  }

  @Test
  @DisplayName("длинная строка выкидывает незакрытую MarkdownV2-ссылку, а не рвёт URL")
  void dropsUnclosedLinkOnALongLine() {
    String text = "msg [author](http://localhost:8080/user/kropachev)";
    int limit = "msg [author](http://local".length();

    String result = IMNotification.truncate(text, limit);

    assertThat(result).isEqualTo("msg\n\u2026");
    assertThat(result).doesNotContain("](");
    assertThat(result.length()).isLessThanOrEqualTo(limit);
  }

  @Test
  @DisplayName("обрыв внутри текста ссылки выкидывает всю ссылку")
  void dropsLinkCutInsideLabel() {
    String text = "msg [author](http://localhost:8080/user/kropachev)";
    int limit = "msg [autho".length() + 2;

    String result = IMNotification.truncate(text, limit);

    assertThat(result).isEqualTo("msg\n\u2026");
    assertThat(result).doesNotContain("[");
    assertThat(result.length()).isLessThanOrEqualTo(limit);
  }

  @Test
  @DisplayName("обрыв между ] и ( выкидывает всю ссылку")
  void dropsLinkCutBetweenBracketAndParen() {
    String text = "msg [author](http://localhost:8080/user/kropachev)";
    int limit = "msg [author]".length() + 2;

    String result = IMNotification.truncate(text, limit);

    assertThat(result).isEqualTo("msg\n\u2026");
    assertThat(result).doesNotContain("[");
  }

  @Test
  @DisplayName("экранированный ]( не считается началом ссылки")
  void escapedSeparatorIsNotALink() {
    String text = "keep \\](http://localhost/very/long/url/continues";
    int limit = "keep \\](http://local".length();

    String result = IMNotification.truncate(text, limit);

    assertThat(result).startsWith("keep \\](");
    assertThat(result).endsWith("\n\u2026");
    assertThat(result.length()).isLessThanOrEqualTo(limit);
  }

  @Test
  @DisplayName("суффикс не разрезает суррогатную пару")
  void doesNotSplitSurrogatePair() {
    String text = "hello\uD83D\uDE00XXXX";

    String result = IMNotification.truncate(text, 8);

    assertThat(result).isEqualTo("hello\n\u2026");
    assertThat(result).doesNotContain("\uD83D");
    assertThat(result.length()).isLessThanOrEqualTo(8);
  }

  @Test
  @DisplayName("хвост changeset сохраняет целые строки коммитов")
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
