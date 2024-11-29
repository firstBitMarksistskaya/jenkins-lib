package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.configuration.GlobalCoverageOptions

class CoverageContext {

    String lockableResource
    GlobalCoverageOptions coverageOptions
    int port
    ArrayList<String> pids
    ArrayList<String> dbgsPids
    ArrayList<String> coverage41CPids

    CoverageContext(String lockableResource, GlobalCoverageOptions coverageOptions, int port, ArrayList<String> dbgsPids, ArrayList<String> coverage41CPids) {
        this.lockableResource = lockableResource
        this.coverageOptions = coverageOptions
        this.port = port
        this.pids = dbgsPids + coverage41CPids
        this.dbgsPids = dbgsPids
        this.coverage41CPids = coverage41CPids
    }

}
