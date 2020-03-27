package ru.pulsar.jenkins.library

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils

import java.nio.charset.StandardCharsets

class MockStepExecutor extends StepExecutor {

    MockStepExecutor() {
        super(null)
    }

    @Override
    boolean isUnix() {
        return SystemUtils.IS_OS_UNIX
    }

    @Override
    String libraryResource(String path) {
        return FileUtils.readFileToString(
            new File("resources/" + path),
            StandardCharsets.UTF_8
        )
    }
}
