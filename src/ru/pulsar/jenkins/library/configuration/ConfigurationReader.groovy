package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.beanutils.BeanUtilsBean
import org.apache.commons.beanutils.ConvertUtilsBean
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.notification.email.EmailExtConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry

import static java.util.Collections.emptySet

class ConfigurationReader implements Serializable {

    private static ObjectMapper mapper
    private static BeanUtilsBean beanUtilsBean;

    static {
        mapper = new ObjectMapper()
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

        beanUtilsBean = new BeanUtilsBean(new ConvertUtilsBean() {
            @Override
            @NonCPS
            Object convert(String value, Class clazz) {
                if (clazz.isEnum()) {
                    return Enum.valueOf(clazz, value);
                } else {
                    return super.convert(value, clazz);
                }
            }
        });
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
            "timeoutOptions",
            "initInfoBaseOptions",
            "bddOptions",
            "sonarQubeOptions",
            "smokeTestOptions",
            "syntaxCheckOptions",
            "resultsTransformOptions",
            "notificationsOptions",
            "emailNotificationOptions",
            "alwaysEmailOptions",
            "successEmailOptions",
            "failureEmailOptions",
            "unstableEmailOptions",
            "recipientProviders",
            "telegramNotificationOptions"
        ).toSet()

        mergeObjects(baseConfiguration, configurationToMerge, nonMergeableSettings)
        mergeInitInfoBaseOptions(baseConfiguration.initInfoBaseOptions, configurationToMerge.initInfoBaseOptions)
        mergeBddOptions(baseConfiguration.bddOptions, configurationToMerge.bddOptions)
        mergeNotificationsOptions(baseConfiguration.notificationsOptions, configurationToMerge.notificationsOptions)

        return baseConfiguration;
    }

    @NonCPS
    private static <T extends Object> void mergeObjects(T baseObject, T objectToMerge, Set<String> nonMergeableSettings) {
        beanUtilsBean.describe(objectToMerge).entrySet().stream()
            .filter({ e -> e.getValue() != null })
            .filter({ e -> e.getKey() != "class" })
            .filter({ e -> e.getKey() != "metaClass" })
            .filter({ e -> !nonMergeableSettings.contains(e.getKey()) })
            .forEach { e ->
                beanUtilsBean.setProperty(baseObject, e.getKey(), e.getValue());
            }

        nonMergeableSettings.forEach({ key ->
            if (!baseObject.hasProperty(key)) {
                return
            }
            if (objectToMerge == null) {
                return
            }
            mergeObjects(
                baseObject[key],
                objectToMerge[key],
                nonMergeableSettings
            )
        })
    }

    @NonCPS
    private static void mergeInitInfoBaseOptions(InitInfoBaseOptions baseObject, InitInfoBaseOptions objectToMerge) {
        if (objectToMerge == null || objectToMerge.additionalInitializationSteps == null) {
            return
        }
        baseObject.additionalInitializationSteps = objectToMerge.additionalInitializationSteps.clone()
    }

    @NonCPS
    private static void mergeBddOptions(BddOptions baseObject, BddOptions objectToMerge) {
        if (objectToMerge == null || objectToMerge.vrunnerSteps == null) {
            return
        }
        baseObject.vrunnerSteps = objectToMerge.vrunnerSteps.clone()
    }


    private static void mergeNotificationsOptions(NotificationsOptions baseObject, NotificationsOptions objectToMerge) {
        if (objectToMerge == null) {
            return
        }

        if (objectToMerge.telegramNotificationOptions != null) {

            mergeObjects(
                baseObject.telegramNotificationOptions,
                objectToMerge.telegramNotificationOptions,
                emptySet()
            )
        }

        def emailNotificationOptionsBase = baseObject.emailNotificationOptions
        def emailNotificationOptionsToMerge = objectToMerge.emailNotificationOptions

        if (emailNotificationOptionsToMerge != null) {
            mergeEmailExtConfiguration(
                emailNotificationOptionsBase.successEmailOptions,
                emailNotificationOptionsToMerge.successEmailOptions
            )
            mergeEmailExtConfiguration(
                emailNotificationOptionsBase.failureEmailOptions,
                emailNotificationOptionsToMerge.failureEmailOptions
            )
            mergeEmailExtConfiguration(
                emailNotificationOptionsBase.unstableEmailOptions,
                emailNotificationOptionsToMerge.unstableEmailOptions
            )
            mergeEmailExtConfiguration(
                emailNotificationOptionsBase.alwaysEmailOptions,
                emailNotificationOptionsToMerge.alwaysEmailOptions
            )
        }
    }

    @NonCPS
    private static void mergeEmailExtConfiguration(EmailExtConfiguration baseObject, EmailExtConfiguration objectToMerge) {
        if (objectToMerge != null && objectToMerge.recipientProviders != null) {
            baseObject.recipientProviders = objectToMerge.recipientProviders.clone()
        }

        if (objectToMerge != null && objectToMerge.directRecipients != null) {
            baseObject.directRecipients = objectToMerge.directRecipients.clone()
        }
    }
}
