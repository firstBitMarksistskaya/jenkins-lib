package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

@JsonIgnoreProperties(ignoreUnknown = true)
class JobConfiguration implements Serializable {
    @JsonPropertyDescription("Версия платформы 1С:Предприятие в формате 8.3.хх.хххх.")
    String v8version

    @JsonPropertyDescription("Путь к корневому каталогу с исходниками конфигурации, в случае хранения исходников в формате EDT, необходимо указать путь к проекту")
    String srcDir

    @JsonPropertyDescription("Массив путей к исходникам расширенийв формате EDT, (в формате XML - не работает)")
    String[] srcExtPath

    @JsonPropertyDescription("Формат исходников конфигурации")
    SourceFormat sourceFormat;

    @JsonProperty("stages")
    @JsonPropertyDescription("Включение этапов сборок")
    StageFlags stageFlags;

    @JsonPropertyDescription("Имя ветки по умолчанию. Значение по умолчанию - main.")
    String defaultBranch

    @JsonPropertyDescription("Идентификаторы сохраненных секретов")
    Secrets secrets;

    @JsonProperty("initInfobase")
    @JsonPropertyDescription("Настройки шага инициализации ИБ")
    InitInfobaseOptions initInfobaseOptions;

    @JsonProperty("bdd")
    @JsonPropertyDescription("Настройки шага запуска BDD сценариев")
    BddOptions bddOptions;

    @JsonProperty("sonarqube")
    @JsonPropertyDescription("Настройки анализа SonarQube")
    SonarQubeOptions sonarQubeOptions;

    @JsonProperty("syntaxCheck")
    @JsonPropertyDescription("Настройки синтаксического контроля")
    SyntaxCheckOptions syntaxCheckOptions;

    @JsonProperty("resultsTransform")
    @JsonPropertyDescription("Настройки трансформации результатов анализа")
    ResultsTransformOptions resultsTransformOptions;

    @JsonProperty("logosConfig")
    @JsonPropertyDescription("Конфигурация библиотеки logos. Применяется перед запуском каждой стадии сборки")
    String logosConfig;

    @Override
    @NonCPS
    String toString() {
        return "JobConfiguration{" +
            "v8version='" + v8version + '\'' +
            ", srcDir='" + srcDir + '\'' +
            ", srcExtDir='" + srcExtDir + '\'' +
            ", sourceFormat=" + sourceFormat +
            ", defaultBranch=" + defaultBranch +
            ", stageFlags=" + stageFlags +
            ", secrets=" + secrets +
            ", initInfobaseOptions=" + initInfobaseOptions +
            ", bddOptions=" + bddOptions +
            ", sonarQubeOptions=" + sonarQubeOptions +
            ", syntaxCheckOptions=" + syntaxCheckOptions +
            ", resultsTransformOptions=" + resultsTransformOptions +
            ", logosConfig=" + logosConfig +
            '}';
    }

    boolean infobaseFromFiles(){
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env();
        String branchName = env.BRANCH_NAME;
        def initMethod = initInfobaseOptions.initMethod

        return (initMethod == InitInfobaseMethod.FROM_SOURCE) ||
            (initMethod == InitInfobaseMethod.DEFAULT_BRANCH_FROM_STORAGE && branchName != defaultBranch)
    }
}