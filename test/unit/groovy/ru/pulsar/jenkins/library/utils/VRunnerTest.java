package ru.pulsar.jenkins.library.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class VRunnerTest {

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext();
    }

    @Test
    void readExitStatusFromFile_success() {

        // given
        String resource = Objects.requireNonNull(getClass()
                        .getClassLoader()
                        .getResource("exitStatus0"))
                        .getPath();

        // when
        Integer exitStatus = VRunner.readExitStatusFromFile(resource);
        // then
        assertThat(exitStatus).isEqualTo(0);

    }

    @Test
    void readExitStatusFromFile_failure() {

        // given
        String resource = Objects.requireNonNull(getClass()
                        .getClassLoader()
                        .getResource("exitStatus1"))
                        .getPath();

        // when
        Integer exitStatus = VRunner.readExitStatusFromFile(resource);
        // then
        assertThat(exitStatus).isEqualTo(1);

    }

    @Test
    void readExitStatusFromFile_does_not_exist() {

        // given
        String resource = "exitStatusDoesNotExist";

        // when
        Integer exitStatus = VRunner.readExitStatusFromFile(resource);
        // then
        assertThat(exitStatus).isEqualTo(1);

    }

    @Test
    void appendV8Version_appends_parameter() {

        // given
        String command = "vrunner xunit --ibconnection \"/F./build/ib\"";

        // when
        String result = VRunner.appendV8Version(command, "8.3.21.1644");

        // then
        assertThat(result).isEqualTo("vrunner xunit --ibconnection \"/F./build/ib\" --v8version 8.3.21.1644");
    }

    @Test
    void appendV8Version_returns_command_when_v8version_is_null() {

        // given
        String command = "vrunner xunit";

        // when
        String result = VRunner.appendV8Version(command, null);

        // then
        assertThat(result).isEqualTo(command);
    }

    @Test
    void appendV8Version_returns_command_when_v8version_is_blank() {

        // given
        String command = "vrunner xunit";

        // when
        String result = VRunner.appendV8Version(command, "  ");

        // then
        assertThat(result).isEqualTo(command);
    }

    @Test
    void appendV8Version_does_not_duplicate_parameter() {

        // given
        String command = "vrunner xunit --v8version 8.3.21.1644";

        // when
        String result = VRunner.appendV8Version(command, "8.3.25.1299");

        // then
        assertThat(result).isEqualTo(command);
    }

    @Test
    void readExitStatusFromFile_does_not_exist_uses_provided_fallback() {

        // given
        String resource = "exitStatusDoesNotExist";

        // when
        Integer exitStatus = VRunner.readExitStatusFromFile(resource, 0);
        // then
        assertThat(exitStatus).isEqualTo(0);

    }
}

