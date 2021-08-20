package ru.pulsar.jenkins.library.utils

import com.cloudbees.groovy.cps.NonCPS
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

import java.util.regex.Pattern

class VersionParser implements Serializable {
    final static VERSION_REGEXP = ~/(?i)<version>(.*)<\/version>/

    static String configuration(rootFile = 'src/cf/Configuration.xml') {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def configurationText = steps.readFile(rootFile, 'UTF-8');
        return version(configurationText, VERSION_REGEXP)
    }

    static String storage(versionFile = 'src/cf/VERSION') {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def storageVersionText = steps.readFile(versionFile, 'UTF-8')
        return version(storageVersionText, VERSION_REGEXP)
    }

    @NonCPS
    private static String version(String text, Pattern regexp) {
        def matcher = text =~ regexp
        return matcher != null && matcher.getCount() == 1 ? matcher[0][1] : ""
    }

    static String edt(rootFile = 'src/Configuration/Configuration.mdo') {

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def configurationText = steps.readFile(rootFile, 'UTF-8');
        return version(configurationText, VERSION_REGEXP)
    }

}
