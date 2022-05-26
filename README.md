# Jenkins shared library for 1C:Enterprise 8

## Цель

Создание библиотеки (или плагина) для Jenkins, позволяющей:

* максимально упростить написание Jenkinsfile для процесса CI в условиях платформы 1С:Предприятие 8
* иметь схожий и контролируемый пайплайн для всех проектов
* дать пользователю в руки простой декларативный конфигурационный файл, вместо требования описывать всю сложную логику по работе с 1С 

## Общие положения

* в активной разработке и поиске "своего пути" по разработке библиотеки;
* формат конфигурационного файла **не** стабилизирован;
* обратная совместимость **пока** не гарантируется, внимательно читайте changelog;
* количество stage будет со временем увеличиваться;
* использовать на свой страх и риск;
* любая помощь приветствуется.

## Ограничения

1. Для шага подготовки требуется любой агент с меткой `agent`.
1. Для запуска шага анализа SonarQube требуется агент с меткой `sonar`.
1. Для запуска шагов, работающих с EDT (валидация, трансформация формата исходников) требуется агент с меткой `edt` (если используется несколько версий EDT необходимо к метке добавить версию, например `edt@2021.3.4:x86_64`) и агент с меткой `oscript`, на котором глобально установлена библиотека [stebi](https://github.com/Stepa86/stebi) версии 1.9.1 или новее.
1. Для запуска шагов, работающих с 1С (подготовка, синтаксический контроль и т.д.) требуется агент с меткой, совпадающей со значением в поле `v8version` файла конфигурации.
1. В качестве ИБ используется файловая база, создаваемая в каталоге `./build/ib`. При необходимости вы можете создать пользователей на фазе инициализации ИБ.

## Возможности

1. Все шаги можно запустить на базе docker-образов из https://github.com/firstBitSemenovskaya/onec-docker. См. [памятку по слоям и последовательности сборки](https://github.com/firstBitSemenovskaya/onec-docker/blob/feature/first-bit/Layers.md).
1. Поддержка как формата выгрузки из Конфигуратора, так и формата EDT.
1. Подготовка информационной базы по версии из хранилища конфигурации, из исходных файлов конфигурации, комбинированный режим (основная ветка - из хранилища, остальные - из исходников).
1. Запуск ИБ в режиме выполнения обработчиков обновления БСП.
1. Дополнительные шаги инициализации данных в ИБ.
1. Трансформация кода из формата конфигуратора в формат EDT.
1. Трансформация кода из формата EDT в формат конфигуратора.
1. Запуск BDD сценариев с сохранением результатов в формате Allure.
1. Запуск синтаксического контроля средствами конфигуратора и сохранение результатов в виде отчета jUnit.
1. Запуск валидации проекта средствами EDT и конвертация отчета в формате generic issues.
1. Запуск статического анализа для SonarQube.
1. Публикация результатов junit и Allure в интерфейс Jenkins.
1. Рассылка результатов сборки на почту и в Telegram.
1. Конфигурирование логгера запускаемых oscript-приложений.

## Подключение

Инструкция по подключению библиотеки: https://jenkins.io/doc/book/pipeline/shared-libraries/#using-libraries

## Примеры Jenkinsfile

Если в настройках подключения shared-library включен флаг "Load implicitly":

```groovy
pipeline1C()
```

В обратном случае:

```groovy
@Library('jenkins-lib') _

pipeline1C()
```

> Да, вот и весь пайплайн. Конфигурирование через json.

## Внешний вид пайплайна в интерфейсе Blue Ocean

![image](https://user-images.githubusercontent.com/1132840/140793320-d0856afe-0864-4f0e-8964-6a9d6d5822b4.png)

## Конфигурирование

По умолчанию применяется [файл конфигурации из ресурсов библиотеки](resources/globalConfiguration.json)

Поверх него накладывается конфигурация из файла `jobConfiguration.json` в корне проекта, если он присутствует.

Пример переопределения:

* указывается точная версия платформы (и соответственно метка агента, см. ограничения)
* указывается точная версия модуля EDT (и соответственно метка агента, см. ограничения)
* идентификаторы credentials для пути к хранилищу и к паре логин/пароль для авторизации в хранилище (необходимы, если применяются шаги, работающие с информационной базой)
* включаются шаги запуска статического анализа SonarQube, валидации средствами EDT и синтаксического контроля 

```json
{
    "$schema": "https://raw.githubusercontent.com/firstBitSemenovskaya/jenkins-lib/master/resources/schema.json",
    "v8version": "8.3.14.1976",
    "edtVersion": "2021.3.4:x86_64",
    "secrets": {
        "storagePath": "f7b21c02-711a-4883-81c5-d429454e3f8b",
        "storage" : "c1fc5f33-67d4-493f-a2a4-97d3040e4b8c"
    },
    "stages": {
        "sonarqube": true,
        "edtValidation": true,
        "syntaxCheck": true
    }
}
```

## Параметры по умолчанию

В библиотеке применяется принцип "соглашения по конфигурации" (convention over configuration): конфигурационный файл
содержит ряд настроек "по умолчанию". При соблюдении определенных правил структуры репозитория можно сократить
количество переопределений конфигурационного файла до минимума. Ниже представлены имеющиеся соглашения и способы их переопределения.

* Общее:
  * В качестве маски версии платформы используется строка "8.3" (`v8version`).
  * По-умолчанию версия модуля EDT не заполнена, т.к. в случае единственной версии для утилиты ring дополнительного указания не требуется (`edtVersion`). 
  * Исходники конфигурации ожидаются в каталоге `src/cf` (`srcDir`).
  * Формат исходников - выгрузка из Конфигуратора (`sourceFormat`).
  * Ветка по умолчанию (для комбинированного режима загрузки конфигурации) - "main" (`defaultBranch`).
  * Имена большинства "секретов" (jenkins credentials, `secrets`) по умолчанию высчитываются из пути к git-репозиторию (без учета домена, с заменой `/` на `_`) с прибавлением ключа секрета. Например, для репозитория https://github.com/firstBitSemenovskaya/jenkins-lib секрет с адресом хранилища будет выглядеть как `firstBitSemenovskaya_jenkins-lib_STORAGE_PATH`. Ключи секретов:
    * `STORAGE_PATH` - путь к хранилищу конфигурации (для `secrets` -> `storagePath`);
    * `STORAGE_USER` - параметры авторизации в хранилище вида "username with password" (для `secrets` -> `storage`).
    * `TELEGRAM_CHAT_ID` - идентификатор чата Telegram для рассылки уведомлений и результате сборки вида "secret text" (для `secrets` -> `telegramChatId`).
  * Секрет `TELEGRAM_BOT_TOKEN` задается глобально на весь сервер Jenkins, либо может быть переопределен (`secrets` -> `telegramBotToken`)
  * Все "шаги" по умолчанию выключены (`stages`).
  * Если в корне репозитория существует файл `packagedef`, то в шагах, работающих с информационной базой, будет выполнена попытка установки локальных зависимостей средствами `opm`.
  * Если после установки локальных зависимостей в каталоге `oscript_modules/bin` существует файл `vrunner`, то для выполнения команд работы с информационной базой будет использоваться он, а не глобально установленный `vrunner` из `PATH`.
  * Результаты в формате `allure` ожидаются в каталоге `build/out/allure` или его подкаталогах.
  * Каждый шаг имеет свой таймаут в минутах (от 10 до 240 в зависимости от "тяжёлости" шага сборки), но может быть переопределен в секции настроек `timeout`. 
* Инициализация:
  * Информационная база инициализируется только в том случае, если в сборочной линии включены шаги, работающие с базой (например, `bdd` или `syntaxCheck`).
  * Информационная база инициализируется конфигурацией из хранилища конфигурации (`initInfobase` -> `initMethod`).
  * Если выбран метод инициализации ИБ из хранилища конфигурации и в каталоге исходников есть файл `VERSION` (артефакт от работы утилиты `gitsync`), то из хранилища будет загружена версия конфигурации с номером из файла `VERSION`.
* Первичный запуск информационной базы:
  * Если информационная база нужна для запуска в режиме "Предприятие" (например, для шагов `bdd` или `smoke`), то будет запущен шаг "Миграция ИБ".
  * После загрузки конфигурации в ИБ будет выполняться запуск ИБ с целью запуска обработчиков обновления из БСП (`initInfobase` -> `runMigration`).
  * Если в настройках шага инициализации не заполнен массив дополнительных шагов миграции (`initInfobase` -> `additionalInitializationSteps`), но в каталоге `tools` присутствуют файлы с именами, удовлетворяющими шаблону `vrunner.init*.json`, то автоматически выполняется запуск `vrunner vanessa` с передачей найденных файлов в качестве значения настроек (параметр `--settings`) в порядке лексикографической сортировки имен файлов.
* BDD:
  * Если в конфигурационном файле проекта не заполнена настройка `bdd` -> `vrunnerSteps`, то автоматически выполняется запуск `vrunner vanessa --settings tools/vrunner.json`.
* Дымовые тесты:
  * Если в репозитории существует файл `tools/vrunner.json`, то запуск дымовых тестов будет выполняться с передачей файла в параметры запуска `vrunner xunit --settings tools/vrunner.json` (`smoke` -> `vrunnerSettings`).
  * Если установка локальных зависимостей `opm` установит пакет `add`, то будет использоваться обработка `xddTestRunner.epf` из локальных зависимостей.
  * Если в репозитории существует файл `tools/xUnitParams.json`, то этот путь к файлу будет передан в параметр запуска `vrunner xunit --xddConfig ./tools/xUnitParams.json` (`smoke -> xUnitParams`).
  * Если используемый конфигурационный файл (`vrunner.json`) не содержит настройку `testsPath`, то запускается полный комплект дымовых тестов, расположенных в `$addRoot/tests/smoke`.
  * По умолчанию включено формирование отчета в формате `jUnit` (`smoke` -> `publishToJUnitReport`) и выключено формирование отчета в формате Allure (`smoke` -> `publishToAllureReport`).
* Синтаксический контроль:
  * Если в репозитории существует файл `tools/vrunner.json`, то синтаксический контроль конфигурации с помощью конфигуратора будет выполняться с передачей файла в параметры запуска `vrunner syntax-check --settings tools/vrunner.json` (`syntaxCheck` -> `vrunnerSettings`).
  * Применяется группировка ошибок по метаданным (`syntaxCheck` -> `groupErrorsByMetadata`).
  * Выгрузка результатов в формат `jUnit` осуществляется в файл `./build/out/jUnit/syntax.xml` (`syntaxCheck` -> `pathToJUnitReport`).
  * Если в репозитории существует файл `./tools/syntax-check-exception-file.txt`, то команде запуска синтаксического контроля конфигурации данный файл будет передаваться как файл с исключениями сообщений об ошибках (параметр `--exception-file`) (`syntaxCheck` -> `exceptionFile`).
  * Конфигурационный файл по умолчанию уже содержит ряд "режимов проверки" для синтаксического контроля конфигурации (`syntaxCheck` -> `checkModes`).
* Трансформация результатов валидации EDT:
  * По умолчанию из результатов анализа исключаются замечания, сработавшие на модулях с включенным запретом редактирования (желтый куб с замком) (параметры `resultsTransform` -> `removeSupport` и `resultsTransform` -> `supportLevel`).
* Анализ SonarQube:
  * Предполагается наличие единственной настройки `SonarQube installation` (`sonarqube` -> `sonarQubeInstallation`).
  * Используется `sonar-scanner` из переменной окружения `PATH` (`sonarqube` -> `useSonarScannerFromPath`).
  * Если использование `sonar-scanner` из переменной окружения `PATH` выключено, предполагается наличие настроенного глобального инструмента `SonarQube Scanner` с идентификатором инструмента `sonar-scanner` (`sonarqube` -> `sonarScannerToolName`).
  * Применяется расчет аргументов командной строки для работы [`branch plugin`](https://github.com/mc1arke/sonarqube-community-branch-plugin) или коммерческих версий SonarQube (`sonarqube` -> `branchAnalysisConfiguration`).
  * Если разработка ведется с использованием подсистемы [БСП "Обновление версии ИБ"](https://its.1c.ru/db/bsp315doc#content:4:1:issogl1_обновление_версии_иб), то в значение параметра `sonar.projectVersion=$configurationVersion` утилиты `sonar-scanner` можно передавать версию из созданного общего модуля. Для этого необходимо заполнить параметр (`sonarqube` -> `infoBaseUpdateModuleName`). Если параметр не заполнен, версия передается из корня конфигурации.
  * По умолчанию шаг анализа не дожидается окончания фонового задания на сервере SonarQube и не анализирует результат прохождения Порога качества (`sonarqube` -> `waitForQualityGate`).
  * Если выполнялась валидация EDT, результаты валидации в формате `generic issues` передаются утилите `sonar-scanner` как значение параметра `sonar.externalIssuesReportPaths`.
* Рассылка уведомлений:
  * Электронная почта:
    * Для отправки используется плагин [`email-ext`](https://plugins.jenkins.io/email-ext). Шаблоны сообщений конфигурируются в настройках плагина. 
    * Уведомления о результатах сборки по умолчанию рассылаются только при полном падении сборочной линии (`notifications` -> `email` -> `onAlways`, `onFailure`, `onUnstable`, `onSuccess`).
    * Лог сборки прикладывается к письму при полном падении сборочной линии и при отправке в режиме "всегда отправлять" (`notifications` -> `email` -> `*options` -> `attachLog`).
    * В качестве получателей писем (`notifications` -> `email` -> `*options` -> `recipientProviders`) в различных режимах отправки используются:
      * всегда - разработчики и запустивший сборку;
      * при падении - разработчики, запустивший сборку и подозреваемый в причине падения сборки;
      * при успехе - разработчики и запустивший сборку;
      * при нестабильной сборке (упавшие тесты) - разработчики и запустивший сборку.
    * Прямые получатели уведомлений не заполнены (`notifications` -> `email` -> `*options` -> `directRecipients`).
  * Telegram:
    * Уведомления о результатах сборки по умолчанию рассылаются всегда (`notifications` -> `telegram` -> `onAlways`, `onFailure`, `onUnstable`, `onSuccess`).