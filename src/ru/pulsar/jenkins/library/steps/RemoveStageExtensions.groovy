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
 * Удаляет из внешней базы служебные расширения стадии после ее выполнения: такие расширения нужны только
 * на своей стадии, а внешняя база живет между сборками. Вызывается из post стадии, поэтому выполняется
 * и при падении стадии, и при прерывании сборки. Если удаление не выполнилось, например пропал агент,
 * оставшиеся расширения удалит подготовка следующей сборки.
 * Расширения пайплайна и установленные не из конфигурации не трогаются. Данные расширения удаляются вместе с ним.
 */
class RemoveStageExtensions implements Serializable {

    private final JobConfiguration config
    private final String stageName

    RemoveStageExtensions(JobConfiguration config, String stageName) {
        this.config = config
        this.stageName = stageName
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        // Локальная база создается на каждой сборке, а тестовая стадия работает со своей копией из архива
        if (!config.basePath) {
            Logger.println("Удалять служебные расширения не нужно: база создается на каждой сборке")
            return
        }

        List<Extension> ownExtensions = []
        for (Extension extension : (List<Extension>) config.stageExtensions()) {
            if (extension.stages.contains(stageName)) {
                ownExtensions.add(extension)
            }
        }
        if (ownExtensions.isEmpty()) {
            Logger.println("У стадии нет служебных расширений")
            return
        }

        String settingsParameter = LoadExtensions.vrunnerSettingsParameter(config, stageName)

        // Удаление расширения с собственными объектами меняет структуру базы и требует монопольного доступа.
        // Список расширений базы читается под ним же: иначе конфигуратор сборки обращается к базе раньше,
        // чем завершены чужие сеансы
        ExclusiveAccess.run(config) { String uccode ->
            List<String> infobaseExtensions = InfobaseExtensions.list(config, settingsParameter + uccode)

            // Удаляются только расширения из списка базы, каждое один раз: для отсутствующего расширения
            // конфигуратор завершается с ошибкой. Расширения нет в базе, если стадия упала до подключения,
            // а одно расширение может быть описано в конфигурации несколькими записями
            List<String> extensionsToDelete = []
            for (Extension extension : ownExtensions) {
                if (InfobaseExtensions.contains(infobaseExtensions, extension.name)
                    && !InfobaseExtensions.contains(extensionsToDelete, extension.name)) {
                    extensionsToDelete.add(extension.name)
                }
            }

            if (extensionsToDelete.isEmpty()) {
                Logger.println("Служебных расширений стадии в базе нет")
                return
            }

            for (String name : extensionsToDelete) {
                Logger.println("Удалим служебное расширение $name")
                VRunner.exec(InfobaseExtensions.deleteCommand(config, name, settingsParameter) + uccode)
            }
        }
    }
}
