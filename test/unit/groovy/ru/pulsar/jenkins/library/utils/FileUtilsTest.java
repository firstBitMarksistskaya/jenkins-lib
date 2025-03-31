package ru.pulsar.jenkins.library.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.IStepExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class FileUtilsTest {

    private final IStepExecutor steps = TestUtils.getMockedStepExecutor();

    @BeforeEach
    void setUp() {
        TestUtils.setupMockedContext(steps);
    }

    @Test
    void testGetLocalPath() {
        // given
        var env = new EnvUtils();
        when(steps.env()).thenReturn(env);
        var filePath = FileUtils.getFilePath(env.WORKSPACE + "/src/cf");

        // when
        String localPath = FileUtils.getLocalPath(filePath);

        // then
        assertThat(localPath).isEqualTo("src/cf");
    }
}
