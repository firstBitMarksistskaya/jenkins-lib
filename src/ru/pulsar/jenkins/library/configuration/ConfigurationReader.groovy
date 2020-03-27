package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.beanutils.BeanUtils
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class ConfigurationReader implements Serializable {
    static JobConfiguration create(String config) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        def mapper = new ObjectMapper()
        def globalConfig = steps.libraryResource 'globalConfiguration.json'

        def globalConfiguration = mapper.readValue(globalConfig, JobConfiguration.class)
        def jobConfiguration = mapper.readValue(config, JobConfiguration.class)

        BeanUtils.describe(jobConfiguration).entrySet().stream()
            .filter({ e -> e.getValue() != null })
            .filter({ e -> e.getKey() != "class" })
            .filter({ e -> e.getKey() != "metaClass" })
            .forEach { e ->
                BeanUtils.setProperty(globalConfiguration, e.getKey(), e.getValue());
            }

        return globalConfiguration
    }
}
