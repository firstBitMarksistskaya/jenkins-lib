package ru.pulsar.jenkins.library.utils

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry

class BspDetector implements Serializable {

    public static final String DEFAULT_INFO_BASE_UPDATE_MODULE_NAME = "ОбновлениеИнформационнойБазыБСП"

    static boolean isBspConfiguration(JobConfiguration config) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        String sourceDir = config.srcDir?.trim()?.replace('\\', '/')
        if (!sourceDir) {
            Logger.println("Не указан srcDir, конфигурация считается не на БСП")
            return false
        }

        String moduleName = getInfoBaseUpdateModuleName(config)
        String modulePath = getCommonModulePath(sourceDir, config.sourceFormat, moduleName)
        boolean bspConfiguration = steps.fileExists(modulePath)

        Logger.println("Определение БСП по общему модулю ${modulePath}: ${bspConfiguration ? 'БСП' : 'не БСП'}")
        return bspConfiguration
    }

    private static String getInfoBaseUpdateModuleName(JobConfiguration config) {
        String moduleName = config.sonarQubeOptions?.infoBaseUpdateModuleName?.trim()
        return moduleName ?: DEFAULT_INFO_BASE_UPDATE_MODULE_NAME
    }

    private static String getCommonModulePath(String sourceDir, SourceFormat sourceFormat, String moduleName) {
        if (sourceFormat == SourceFormat.EDT) {
            return "${sourceDir}/src/CommonModules/${moduleName}/Module.bsl"
        }

        return "${sourceDir}/CommonModules/${moduleName}/Ext/Module.bsl"
    }
}
