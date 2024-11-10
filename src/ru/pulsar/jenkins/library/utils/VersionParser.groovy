package ru.pulsar.jenkins.library.utils

import com.cloudbees.groovy.cps.NonCPS
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

import java.util.regex.Pattern

class VersionParser implements Serializable {
    final static VERSION_REGEXP = ~/(?i)<version>(.*)<\/version>/
    final static VERSION_REGEXP_SSL = ~/(?i)Описание.Версия = "(.*)";/

    static String configuration(rootFile = 'src/cf/Configuration.xml') {
        return extractVersionFromFile(rootFile, VERSION_REGEXP)
    }

    static String edt(rootFile = 'src/Configuration/Configuration.mdo') {
        return extractVersionFromFile(rootFile, VERSION_REGEXP)
    }

    static String storage(versionFile = 'src/cf/VERSION') {
        return extractVersionFromFile(versionFile, VERSION_REGEXP)
    }

    static String ssl(versionFile) {
        return extractVersionFromFile(versionFile, VERSION_REGEXP_SSL)
    }

    private static String extractVersionFromFile(String filePath, Pattern regexp) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        if (!steps.fileExists(filePath)) {
            return ""
        }

        def configurationText = steps.readFile(filePath)
        return version(configurationText, regexp)
    }

    @NonCPS
    private static String version(String text, Pattern regexp) {
        def matcher = text =~ regexp
        return matcher != null && matcher.getCount() == 1 ? matcher[0][1] : ""
    }

}
