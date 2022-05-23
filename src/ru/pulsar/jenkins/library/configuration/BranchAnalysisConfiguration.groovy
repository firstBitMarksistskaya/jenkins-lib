package ru.pulsar.jenkins.library.configuration

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

enum BranchAnalysisConfiguration {
    @JsonPropertyDescription(
        """Применяется автоконфигурация sonar-scanner силами branchplugin. 
        Так же может применяться для отключения конфигурирования, если branch plugin отсутствует"""
    )
    @JsonProperty("auto")
    AUTO,

    @JsonPropertyDescription("Применяется ручная конфигурация sonar-scanner на основе переменных среды")
    @JsonProperty("fromEnv")
    FROM_ENV
}
