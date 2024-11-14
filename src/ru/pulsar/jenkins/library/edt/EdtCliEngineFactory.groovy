package ru.pulsar.jenkins.library.edt

import java.lang.module.ModuleDescriptor

class EdtCliEngineFactory {

    private static final ModuleDescriptor.Version EDT_CLI_MIN_VERSION = ModuleDescriptor.Version.parse("2024")

    /**
     * Создает движок конвертации в зависимости от версии EDT
     * @param edtVersion версия EDT в формате YYYY.X.Z, YYYY.X или YYYY
     * @return IEdtCliEngine подходящая реализация движка
     * @throws IllegalArgumentException если версия имеет некорректный формат
     */
    static IEdtCliEngine getEngine(String edtVersion) {

        if (edtVersion == null || edtVersion.trim().empty) {
            throw new IllegalArgumentException("Версия EDT не может быть пустой")
        }

        try {
            def currentVersion = ModuleDescriptor.Version.parse(edtVersion)

            return isEdtCliRequired(currentVersion)
                    ? new NativeEdtCliConverter()
                    : new RingConverter()
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректный формат версии EDT: ${edtVersion}", e)
        }
    }

    /**
     * Проверяет необходимость использования 1cedtcli
     * @param edtVersion текущая версия EDT
     * @return true если нужно использовать 1cedtcli
     */
    private static boolean isEdtCliRequired (ModuleDescriptor.Version edtVersion) {
        return edtVersion >= EDT_CLI_MIN_VERSION
    }
}
