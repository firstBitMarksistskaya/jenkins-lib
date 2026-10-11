package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.InitInfoBaseOptions.Extension
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.ExclusiveAccess
import ru.pulsar.jenkins.library.utils.InfobaseExtensions
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

/**
 * Приводит внешнюю базу в известное состояние по расширениям отдельных стадий: удаляет из нее расширения,
 * предназначенные для других стадий. Внешняя база живет между сборками, и расширение, подключенное для одной
 * стадии, иначе осталось бы в базе для других стадий и следующих сборок, в том числе после прерванной сборки.
 * Расширения этапа initInfoBase и установленные не из конфигурации не трогаются.
 */
class NormalizeExtensions implements Serializable {

    private final JobConfiguration config
    private final String stageName

    NormalizeExtensions(JobConfiguration config, String stageName) {
        this.config = config
        this.stageName = stageName
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        // Локальная база создается на каждой сборке, а в тестовые стадии приходит из архива,
        // сделанного до подключения расширений отдельных стадий
        if (!config.basePath) {
            Logger.println("Нормализация расширений не нужна: база создается на каждой сборке")
            return
        }

        List<Extension> stageExtensions = config.stageExtensions()
        if (stageExtensions.isEmpty()) {
            Logger.println("Нормализация расширений не нужна: в конфигурации нет расширений отдельных стадий")
            return
        }

        String settingsParameter = LoadExtensions.vrunnerSettingsParameter(config, stageName)
        List<String> infobaseExtensions = InfobaseExtensions.list(config, settingsParameter)

        List<String> extensionsToDelete = []
        for (Extension extension : stageExtensions) {
            if (!extension.stages.contains(stageName)
                && InfobaseExtensions.contains(infobaseExtensions, extension.name)
                && !InfobaseExtensions.contains(extensionsToDelete, extension.name)) {
                extensionsToDelete.add(extension.name)
            }
        }

        if (extensionsToDelete.isEmpty()) {
            Logger.println("В базе нет расширений других стадий")
            return
        }

        // Удаление расширения с собственными объектами меняет структуру базы и требует монопольного доступа
        ExclusiveAccess.run(config) { String uccode ->
            for (String name : extensionsToDelete) {
                Logger.println("Удалим расширение другой стадии $name")
                VRunner.exec(InfobaseExtensions.deleteCommand(config, name, settingsParameter) + uccode)
            }
        }
    }
}
