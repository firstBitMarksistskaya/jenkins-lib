import com.cloudbees.groovy.cps.NonCPS

def configuration(rootFile = 'src/cf/Configuration.xml') {
    def configurationText = readFile encoding: 'UTF-8', file: rootFile
    return version(configurationText, /<Version>(.*)<\/Version>/)
}

def storage(versionFile = 'src/cf/VERSION') {
    storageVersionText = readFile encoding: 'UTF-8', file: versionFile
    return version(versionFile, /<VERSION>(.*)<\/VERSION>/)
}

@NonCPS
private static String version(String text, String regexp) {
    def matcher = text =~ regexp
    return matcher ? matcher.group(1) : ""
}