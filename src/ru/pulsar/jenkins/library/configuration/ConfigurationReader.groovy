package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.beanutils.BeanUtils
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class ConfigurationReader implements Serializable {

    private static ObjectMapper mapper = new ObjectMapper()
    private static final String DEFAULT_CONFIGURATION_RESOURCE = 'globalConfiguration.json'

    static JobConfiguration create() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def globalConfig = steps.libraryResource DEFAULT_CONFIGURATION_RESOURCE

        def globalConfiguration = mapper.readValue(globalConfig, JobConfiguration.class)

        return globalConfiguration
    }

    static JobConfiguration create(String config) {
        def globalConfiguration = create()
        def jobConfiguration = mapper.readValue(config, JobConfiguration.class)

        return mergeConfigurations(globalConfiguration, jobConfiguration);
    }

    private static JobConfiguration mergeConfigurations(
        JobConfiguration baseConfiguration,
        JobConfiguration configurationToMerge
    ) {
        def nonMergeableSettings = Arrays.asList(
            "secrets",
            "sonarQubeOptions",
            "syntaxCheckOptions"
        ).toSet()

        mergeObjects(baseConfiguration, configurationToMerge, nonMergeableSettings)

        return baseConfiguration;
    }

    @NonCPS
    private static <T extends Object> void mergeObjects(T baseObject, T objectToMerge, Set<String> nonMergeableSettings) {
        BeanUtils.describe(objectToMerge).entrySet().stream()
            .filter({ e -> e.getValue() != null })
            .filter({ e -> e.getKey() != "class" })
            .filter({ e -> e.getKey() != "metaClass" })
            .filter({ e -> !nonMergeableSettings.contains(e.getKey()) })
            .forEach { e ->
                BeanUtils.setProperty(baseObject, e.getKey(), e.getValue());
            }

        nonMergeableSettings.forEach({ key ->
            mergeObjects(
                baseObject[key],
                objectToMerge[key],
                Collections.emptySet()
            )
        })
    }
}
