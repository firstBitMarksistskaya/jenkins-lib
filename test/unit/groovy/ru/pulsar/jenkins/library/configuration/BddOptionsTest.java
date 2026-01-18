package ru.pulsar.jenkins.library.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.utils.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BddOptionsTest {

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext();
    }

    @Test
    @DisplayName("getVrunnerSteps должен возвращать null если не задан явно")
    void testGetVrunnerStepsReturnsNullByDefault() {
        // given
        BddOptions options = new BddOptions();

        // when
        String[] steps = options.getVrunnerSteps();

        // then
        assertThat(steps).isNull();
    }

    @Test
    @DisplayName("getEffectiveVrunnerSteps должен использовать значение по умолчанию из vrunnerSettings")
    void testEffectiveVrunnerStepsUsesDefaultFromVrunnerSettings() {
        // given
        BddOptions options = new BddOptions();

        // when
        String[] steps = options.getEffectiveVrunnerSteps();

        // then
        assertThat(steps).hasSize(1);
        assertThat(steps[0]).isEqualTo("vanessa --settings ./tools/vrunner.json");
    }

    @Test
    @DisplayName("getEffectiveVrunnerSteps должен использовать кастомный путь из vrunnerSettings")
    void testEffectiveVrunnerStepsUsesCustomVrunnerSettings() {
        // given
        BddOptions options = new BddOptions();
        options.setVrunnerSettings("./custom/path/vrunner.json");

        // when
        String[] steps = options.getEffectiveVrunnerSteps();

        // then
        assertThat(steps).hasSize(1);
        assertThat(steps[0]).isEqualTo("vanessa --settings ./custom/path/vrunner.json");
    }

    @Test
    @DisplayName("getEffectiveVrunnerSteps должен использовать явно заданное значение vrunnerSteps")
    void testEffectiveVrunnerStepsUsesExplicitValue() {
        // given
        BddOptions options = new BddOptions();
        options.setVrunnerSettings("./custom/path/vrunner.json");
        options.setVrunnerSteps(new String[]{"vanessa --settings ./explicit/path.json"});

        // when
        String[] steps = options.getEffectiveVrunnerSteps();

        // then
        assertThat(steps).hasSize(1);
        assertThat(steps[0]).isEqualTo("vanessa --settings ./explicit/path.json");
    }

    @Test
    @DisplayName("getVrunnerSteps должен возвращать явно заданные шаги")
    void testGetVrunnerStepsReturnsExplicitSteps() {
        // given
        BddOptions options = new BddOptions();
        options.setVrunnerSteps(new String[]{
            "vanessa --settings ./tools/vrunner.json",
            "vanessa --settings ./tools/vrunner2.json"
        });

        // when
        String[] steps = options.getVrunnerSteps();

        // then
        assertThat(steps).hasSize(2);
        assertThat(steps[0]).isEqualTo("vanessa --settings ./tools/vrunner.json");
        assertThat(steps[1]).isEqualTo("vanessa --settings ./tools/vrunner2.json");
    }

    @Test
    @DisplayName("hasCustomVrunnerSteps должен возвращать false по умолчанию")
    void testHasCustomVrunnerStepsReturnsFalseByDefault() {
        // given
        BddOptions options = new BddOptions();

        // then
        assertThat(options.hasCustomVrunnerSteps()).isFalse();
    }

    @Test
    @DisplayName("hasCustomVrunnerSteps должен возвращать true после явной установки значения")
    void testHasCustomVrunnerStepsReturnsTrueAfterSet() {
        // given
        BddOptions options = new BddOptions();
        options.setVrunnerSteps(new String[]{"vanessa --settings ./custom.json"});

        // then
        assertThat(options.hasCustomVrunnerSteps()).isTrue();
    }

    @Test
    @DisplayName("изменение vrunnerSettings должно влиять на getEffectiveVrunnerSteps")
    void testChangingVrunnerSettingsAffectsEffectiveVrunnerSteps() {
        // given
        BddOptions options = new BddOptions();

        // when - проверяем начальное значение
        String[] initialSteps = options.getEffectiveVrunnerSteps();
        assertThat(initialSteps[0]).isEqualTo("vanessa --settings ./tools/vrunner.json");

        // when - меняем vrunnerSettings
        options.setVrunnerSettings("./another/path.json");
        String[] updatedSteps = options.getEffectiveVrunnerSteps();

        // then - значение по умолчанию должно измениться
        assertThat(updatedSteps[0]).isEqualTo("vanessa --settings ./another/path.json");
    }

    @Test
    @DisplayName("явно заданное vrunnerSteps не должно изменяться при смене vrunnerSettings")
    void testExplicitVrunnerStepsNotAffectedByVrunnerSettingsChange() {
        // given
        BddOptions options = new BddOptions();
        options.setVrunnerSteps(new String[]{"vanessa --settings ./explicit.json"});

        // when - меняем vrunnerSettings
        options.setVrunnerSettings("./another/path.json");
        String[] steps = options.getEffectiveVrunnerSteps();

        // then - явно заданное значение не должно измениться
        assertThat(steps[0]).isEqualTo("vanessa --settings ./explicit.json");
    }
}

