package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonIgnoreProperties(ignoreUnknown = true)
class TimeoutOptions implements Serializable {
    
    @JsonPropertyDescription('''Таймаут шага трансформации исходников из формата EDT в формат Конфигуратора, в минутах.
    По умолчанию содержит значение 60.
    ''')
    Integer edtToDesignerFormatTransformation

    @JsonPropertyDescription('''Таймаут шага создания информационной базы, в минутах.
    По умолчанию содержит значение 60.
    ''')
    Integer createInfoBase

    @JsonPropertyDescription('''Таймаут шага инициализации информационной базы, в минутах.
    По умолчанию содержит значение 60.
    ''')
    Integer initInfoBase

    @JsonPropertyDescription('''Таймаут шага архивирования информационной базы, в минутах.
    По умолчанию содержит значение 60.
    ''')
    Integer zipInfoBase

    @JsonPropertyDescription('''Таймаут шага трансформации исходников из формата Конфигуратора в формат EDT, в минутах.
    По умолчанию содержит значение 60.
    ''')
    Integer designerToEdtFormatTransformation

    @JsonPropertyDescription('''Таймаут шага валидации EDT, в минутах.
    По умолчанию содержит значение 240.
    ''')
    Integer edtValidate

    @JsonPropertyDescription('''Таймаут шага трансформации результатов EDT, в минутах.
    По умолчанию содержит значение 10.
    ''')
    Integer resultTransformation

    @JsonPropertyDescription('''Таймаут шага проверки сценариев поведения, в минутах.
    По умолчанию содержит значение 120.
    ''')
    Integer bdd

    @JsonPropertyDescription('''Таймаут шага синтаксического контроля, в минутах.
    По умолчанию содержит значение 240.
    ''')
    Integer syntaxCheck

    @JsonPropertyDescription('''Таймаут шага дымовых тестов, в минутах.
    По умолчанию содержит значение 240.
    ''')
    Integer smoke

    @JsonPropertyDescription('''Таймаут шага статического анализа SonarQube, в минутах.
    По умолчанию содержит значение 90.
    ''')
    Integer sonarqube
}
