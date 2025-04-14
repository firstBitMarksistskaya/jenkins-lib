package ru.pulsar.jenkins.library.steps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.utils.TestUtils;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class InitInfoBaseTest {

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
        Integer exitStatus = InitInfoBase.readExitStatusFromFile(resource);
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
        Integer exitStatus = InitInfoBase.readExitStatusFromFile(resource);
        // then
        assertThat(exitStatus).isEqualTo(1);

    }

    @Test
    void readExitStatusFromFile_does_not_exist() {

        // given
        String resource = "exitStatusDoesNotExist";

        // when
        Integer exitStatus = InitInfoBase.readExitStatusFromFile(resource);
        // then
        assertThat(exitStatus).isEqualTo(1);

    }
}
