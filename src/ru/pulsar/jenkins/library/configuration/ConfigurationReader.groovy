package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.beanutils.BeanUtils
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

import javax.annotation.CheckForNull

class ConfigurationReader implements Serializable {

    private static ObjectMapper mapper
    static {
        mapper = new ObjectMapper()
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    }

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
            "stageFlags",
            "initInfobaseOptions",
            "bddOptions",
            "sonarQubeOptions",
            "syntaxCheckOptions",
            "resultsTransformOptions"
        ).toSet()

        mergeObjects(baseConfiguration, configurationToMerge, nonMergeableSettings)
        mergeInitInfobaseOptions(baseConfiguration.initInfobaseOptions, configurationToMerge.initInfobaseOptions);
        mergeBddOptions(baseConfiguration.bddOptions, configurationToMerge.bddOptions);

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

    @NonCPS
    private static void mergeInitInfobaseOptions(InitInfobaseOptions baseObject, @CheckForNull InitInfobaseOptions objectToMerge) {
        if (objectToMerge == null || objectToMerge.additionalInitializationSteps == null) {
            return
        }
        baseObject.additionalInitializationSteps = objectToMerge.additionalInitializationSteps.clone()
    }

    @NonCPS
    private static void mergeBddOptions(BddOptions baseObject, @CheckForNull BddOptions objectToMerge) {
        if (objectToMerge == null || objectToMerge.vrunnerSteps == null) {
            return
        }
        baseObject.vrunnerSteps = objectToMerge.vrunnerSteps.clone()
    }
}
