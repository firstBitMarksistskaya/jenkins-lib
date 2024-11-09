package ru.pulsar.jenkins.library.utils;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.IStepExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class VersionParserTest {

  private final IStepExecutor steps = TestUtils.getMockedStepExecutor();
  private File file;

  @BeforeEach
  void setUp() throws IOException {
    TestUtils.setupMockedContext(steps);
    file = File.createTempFile("version", ".xml");
  }

  @AfterEach
  void tearDown() {
    FileUtils.deleteQuietly(file);
  }

  @Test
  void testStorage() throws IOException {
    // given
    FileUtils.writeStringToFile(file, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<VERSION>3487</VERSION>", StandardCharsets.UTF_8);

    // when
    String storage = VersionParser.storage(file.toString());

    // then
    assertThat(storage).isEqualTo("3487");
  }

  @Test
  void testEmptyConfiguration() throws IOException {
    // given
    FileUtils.writeStringToFile(file,
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<MetaDataObject xmlns=\"http://v8.1c.ru/8.3/MDClasses\" xmlns:app=\"http://v8.1c.ru/8.2/managed-application/core\" xmlns:cfg=\"http://v8.1c.ru/8.1/data/enterprise/current-config\" xmlns:cmi=\"http://v8.1c.ru/8.2/managed-application/cmi\" xmlns:ent=\"http://v8.1c.ru/8.1/data/enterprise\" xmlns:lf=\"http://v8.1c.ru/8.2/managed-application/logform\" xmlns:style=\"http://v8.1c.ru/8.1/data/ui/style\" xmlns:sys=\"http://v8.1c.ru/8.1/data/ui/fonts/system\" xmlns:v8=\"http://v8.1c.ru/8.1/data/core\" xmlns:v8ui=\"http://v8.1c.ru/8.1/data/ui\" xmlns:web=\"http://v8.1c.ru/8.1/data/ui/colors/web\" xmlns:win=\"http://v8.1c.ru/8.1/data/ui/colors/windows\" xmlns:xen=\"http://v8.1c.ru/8.3/xcf/enums\" xmlns:xpr=\"http://v8.1c.ru/8.3/xcf/predef\" xmlns:xr=\"http://v8.1c.ru/8.3/xcf/readable\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"2.8\">\n" +
        "\t<Configuration uuid=\"1e4190e9-76c2-456e-a607-4d817110ffd9\">\n" +
        "\t\t<Properties>\n" +
        "\t\t\t<Vendor>Some vendor</Vendor>\n" +
        "\t\t\t<Version/>\n" +
        "\t\t</Properties>\n" +
        "\t</Configuration>" +
        "</MetaDataObject>"
      , StandardCharsets.UTF_8);

    // when
    String storage = VersionParser.configuration(file.toString());

    // then
    assertThat(storage).isEmpty();
  }

  @Test
  void testConfiguration() throws IOException {
    // given
    FileUtils.writeStringToFile(file,
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<MetaDataObject xmlns=\"http://v8.1c.ru/8.3/MDClasses\" xmlns:app=\"http://v8.1c.ru/8.2/managed-application/core\" xmlns:cfg=\"http://v8.1c.ru/8.1/data/enterprise/current-config\" xmlns:cmi=\"http://v8.1c.ru/8.2/managed-application/cmi\" xmlns:ent=\"http://v8.1c.ru/8.1/data/enterprise\" xmlns:lf=\"http://v8.1c.ru/8.2/managed-application/logform\" xmlns:style=\"http://v8.1c.ru/8.1/data/ui/style\" xmlns:sys=\"http://v8.1c.ru/8.1/data/ui/fonts/system\" xmlns:v8=\"http://v8.1c.ru/8.1/data/core\" xmlns:v8ui=\"http://v8.1c.ru/8.1/data/ui\" xmlns:web=\"http://v8.1c.ru/8.1/data/ui/colors/web\" xmlns:win=\"http://v8.1c.ru/8.1/data/ui/colors/windows\" xmlns:xen=\"http://v8.1c.ru/8.3/xcf/enums\" xmlns:xpr=\"http://v8.1c.ru/8.3/xcf/predef\" xmlns:xr=\"http://v8.1c.ru/8.3/xcf/readable\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"2.8\">\n" +
        "\t<Configuration uuid=\"1e4190e9-76c2-456e-a607-4d817110ffd9\">\n" +
        "\t\t<Properties>\n" +
        "\t\t\t<Vendor>Some vendor</Vendor>\n" +
        "\t\t\t<Version>1.0.0.1</Version>\n" +
        "\t\t</Properties>\n" +
        "\t</Configuration>" +
        "</MetaDataObject>"
      , StandardCharsets.UTF_8);

    // when
    String storage = VersionParser.configuration(file.toString());

    // then
    assertThat(storage).isEqualTo("1.0.0.1");
  }

  @Test
  void testVersionComparisonLessThan() {

    // given
    String thisVersion = "2023.2.4";
    String thatVersion = "2023.3.1";

    // when
    int result = VersionParser.compare(thisVersion, thatVersion);

    // then
    assertThat(result).isEqualTo(-1);

  }

  @Test
  void testVersionComparisonEqualShort() {

    // given
    String thisVersion = "2024.2.4";
    String thatVersion = "2024.2";

    // when
    int result = VersionParser.compare(thisVersion, thatVersion);

    // then
    assertThat(result).isEqualTo(0);

  }

}