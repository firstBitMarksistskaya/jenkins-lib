package ru.pulsar.jenkins.library.utils

import ru.pulsar.jenkins.library.configuration.JobConfiguration

/**
 * Ресурсы lockable-resources для баз сборки.
 *
 * Внешняя блокировка держит базы сборки от начала подготовки до окончания последнего теста: все ветки
 * многоветочного задания читают один jobConfiguration.json и получают одни адреса баз, и без блокировки
 * чужая сборка успела бы загрузить в базу свою конфигурацию между подготовкой и тестами.
 * Внутренняя блокировка выстраивает в очередь тестовые стадии одной сборки на одной базе. Блокировка
 * не повторно входимая, поэтому имя внутренней отличается от внешней.
 * Локальной базе блокировка не нужна, но шагу lock нужно имя ресурса. Для нее берутся имена, которые
 * больше никто не запросит, поэтому тестовые стадии на локальной базе идут параллельно.
 */
class InfobaseLocks implements Serializable {

    /**
     * Заполняет ресурсы для шагов lock(extra: ...): внешней блокировки и внутренних блокировок тестовых стадий.
     * Списки заполняются, а не создаются заново: аргументы шагов в options вычисляются в начале сборки
     * и держат ссылки на списки, созданные до первой стадии.
     */
    static void fill(JobConfiguration config, String buildTag, List<Map<String, String>> buildResources,
                     Map<String, List<Map<String, String>>> stageResources) {
        String resource = InfobaseConnection.lockResource(config.baseName())

        buildResources.add([resource: resource ?: buildTag])
        for (String stageName : stageResources.keySet()) {
            String stageResource = resource ? "${resource}_${buildTag}" : "${buildTag}_${stageName}"
            stageResources[stageName].add([resource: stageResource])
        }

        if (resource) {
            // Для запроса нескольких ресурсов плагин не пишет, какая сборка их держит
            Logger.println("Подготовка и проверка качества займут базу $resource. Пока ее держит другая сборка, эта сборка будет ждать")
        }
    }
}
