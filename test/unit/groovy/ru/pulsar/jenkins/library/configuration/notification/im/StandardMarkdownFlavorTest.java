package ru.pulsar.jenkins.library.configuration.notification.im;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StandardMarkdownFlavorTest {

    private final StandardMarkdownFlavor flavor = new StandardMarkdownFlavor();

    @Test
    void escape_returns_null_for_null() {
        assertThat(flavor.escape(null)).isNull();
    }

    @Test
    void escape_leaves_plain_text_untouched() {
        assertThat(flavor.escape("Сборка успешно завершена")).isEqualTo("Сборка успешно завершена");
    }

    @Test
    void escape_escapes_backslash_first() {
        assertThat(flavor.escape("C:\\temp")).isEqualTo("C:\\\\temp");
    }

    @Test
    void escape_escapes_inline_markup() {
        assertThat(flavor.escape("*bold*")).isEqualTo("\\*bold\\*");
        assertThat(flavor.escape("_italic_")).isEqualTo("\\_italic\\_");
        assertThat(flavor.escape("`code`")).isEqualTo("\\`code\\`");
        assertThat(flavor.escape("~strike~")).isEqualTo("\\~strike\\~");
        assertThat(flavor.escape("![img](url)")).isEqualTo("\\!\\[img\\]\\(url\\)");
    }

    @Test
    void escape_escapes_block_markup() {
        assertThat(flavor.escape("# heading")).isEqualTo("\\# heading");
        assertThat(flavor.escape("> quote")).isEqualTo("\\> quote");
        assertThat(flavor.escape("- item")).isEqualTo("\\- item");
        assertThat(flavor.escape("+ item")).isEqualTo("\\+ item");
        assertThat(flavor.escape("1. item")).isEqualTo("1\\. item");
        assertThat(flavor.escape("=== setext")).isEqualTo("\\=\\=\\= setext");
    }

    @Test
    void escape_escapes_raw_html_and_autolinks() {
        assertThat(flavor.escape("<b>")).isEqualTo("\\<b\\>");
    }

    @Test
    void escape_escapes_table_delimiter() {
        assertThat(flavor.escape("a | b")).isEqualTo("a \\| b");
    }

    @Test
    void escape_does_not_escape_characters_without_markdown_meaning() {
        assertThat(flavor.escape("2^10")).isEqualTo("2^10");
        assertThat(flavor.escape("{\"key\": 1}")).isEqualTo("{\"key\": 1}");
    }

    @Test
    void link_escapes_text_and_url() {
        assertThat(flavor.link("build #42", "https://ci/job/a_b/42/"))
            .isEqualTo("[build \\#42](https://ci/job/a_b/42/)");
    }

    @Test
    void link_escapes_closing_paren_in_url() {
        assertThat(flavor.link("job", "https://ci/job(1)/"))
            .isEqualTo("[job](https://ci/job(1\\)/)");
    }

    @Test
    void link_without_url_returns_escaped_text() {
        assertThat(flavor.link("build #42", null)).isEqualTo("build \\#42");
    }

    @Test
    void markup_accessors_return_unescaped_markup() {
        assertThat(flavor.bullet()).isEqualTo("*");
        assertThat(flavor.hash()).isEqualTo("#");
        assertThat(flavor.openParen()).isEqualTo("(");
        assertThat(flavor.closeParen()).isEqualTo(")");
    }
}
