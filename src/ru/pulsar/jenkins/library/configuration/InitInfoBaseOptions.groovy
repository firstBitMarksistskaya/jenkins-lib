package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class InitInfoBaseOptions implements Serializable {

    @JsonPropertyDescription("""
    Способ инициализации информационной базы.
    Поддерживается три варианта:
        * fromStorage - инициализация информационной базы из хранилища конфигурации;
        * fromSource - инициализация информационной базы из исходников конфигурации;
        * defaultBranchFromStorage - инициализация основной ветки из хранилища конфигурации, остальных - из исходников конфигурации.
    По умолчанию содержит значение "fromStorage".""")
    InitInfoBaseMethod initMethod = InitInfoBaseMethod.FROM_STORAGE

    @JsonPropertyDescription("Запустить миграцию ИБ")
    Boolean runMigration = true

    @JsonPropertyDescription("""Дополнительные шаги, запускаемые через vrunner.
    В каждой строке передается отдельная команда 
    vrunner и ее аргументы (например, "vanessa --settings ./tools/vrunner.first.json")
    """)
    String[] additionalInitializationSteps

    @JsonPropertyDescription("Массив расширений для загрузки в конфигурацию.")
    Extension[] extensions

    @JsonPropertyDescription("""Путь к конфигурационному файлу vanessa-runner.
    По умолчанию содержит значение "./tools/vrunner.json".
    """)
    String vrunnerSettings

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Extension implements Serializable {
        @JsonPropertyDescription("Имя расширения, используемое при его загрузке в конфигурацию.")
        String name = "extension"

        @JsonPropertyDescription("""
        Способ инициализации расширения.
        Поддерживается два варианта:
            * fromSource - инициализация расширения из исходников;
            * fromFile - скачивание скомпилированного cfe по ссылке.
        """)
        InitExtensionMethod initMethod = InitExtensionMethod.SOURCE

        @JsonPropertyDescription("""
        Хранит в себе путь к расширению.
            * В случае если выбран initMethod <fromSource> - указывается путь к исходникам расширения.
            * В случае если выбран initMethod <fromFile> - указывается путь к cfe-файлу
        """)
        String path = "src/cfe/extension"
    }

    @Override
    @NonCPS
    String toString() {
        return "InitInfoBaseOptions{" +
            "initMethod=" + initMethod +
            ", runMigration=" + runMigration +
            ", additionalInitializationSteps=" + additionalInitializationSteps +
            ", extensions=" + extensions +
            ", vrunnerSettings=" + vrunnerSettings +
                '}'
    }
}
