package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.configuration.GlobalCoverageOptions

class CoverageContext {

    String lockableResource
    String srcDir
    GlobalCoverageOptions coverageOptions
    int port
    String dbgsPid

    CoverageContext(String lockableResource, String srcDir, GlobalCoverageOptions coverageOptions, int port) {
        this.lockableResource = lockableResource
        this.srcDir = srcDir
        this.coverageOptions = coverageOptions
        this.port = port
    }

}
