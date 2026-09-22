# How To Add Debug Settings

## Что это дает
Этот механизм позволяет подложить свои файлы настроек в Jenkins workspace для MR-пайплайна, не меняя прикладной репозиторий.

Поддерживаемые файлы:
- `jobConfiguration.json`
- `sonar-project.properties`
- `tools/vrunner.json`
- `tools/VAParams.json`

## Что нужно подготовить в Jenkins

### 1. Проверить plugin
Убедиться, что в Jenkins установлен plugin `Config File Provider`.

Если plugin не установлен:
- механизм подмен не сработает;
- пайплайн продолжит работу по старому сценарию.

### 2. Создать control JSON
В Jenkins нужно создать managed file с `fileId`:

```text
jenkins-debug-overrides-control
```

Рекомендуемый тип файла при создании:

```text
Json file
```

В этом файле хранится карта профилей и список подмен.

Допустимо также использовать `Custom file`, но для JSON удобнее `Json file`, потому что в Jenkins он лучше читается визуально.

Пример содержимого:

```json
{
  "profiles": {
    "ci_uh_MR": {
      "enabled": true,
      "description": "Отладочные подмены для MR пайплайнов УХ",
      "replacements": [
        {
          "fileId": "debug-ci-uh-mr-jobConfiguration",
          "target": "jobConfiguration.json"
        },
        {
          "fileId": "debug-ci-uh-mr-sonar-properties",
          "target": "sonar-project.properties"
        },
        {
          "fileId": "debug-ci-uh-mr-vrunner",
          "target": "tools/vrunner.json"
        },
        {
          "fileId": "debug-ci-uh-mr-vaparams",
          "target": "tools/VAParams.json"
        }
      ]
    }
  }
}
```

## Как добавить свою настройку

### Вариант 1. Изменить существующий профиль
Если вы работаете в уже существующем Jenkins folder, например `ci_uh_MR`, то:

1. Откройте managed file `jenkins-debug-overrides-control`.
2. Найдите профиль:

```json
"ci_uh_MR": {
  ...
}
```

3. Убедитесь, что:

```json
"enabled": true
```

4. Обновите список `replacements`, если нужно добавить или убрать файлы.

### Вариант 2. Создать новый профиль под другой folder
Если у вас другой Jenkins folder, добавьте новый ключ в `profiles`.

Пример:

```json
{
  "profiles": {
    "ci_uh_MR": {
      "enabled": true,
      "replacements": [
        {
          "fileId": "debug-ci-uh-mr-jobConfiguration",
          "target": "jobConfiguration.json"
        }
      ]
    },
    "ci_erp_MR": {
      "enabled": true,
      "replacements": [
        {
          "fileId": "debug-ci-erp-mr-jobConfiguration",
          "target": "jobConfiguration.json"
        }
      ]
    }
  }
}
```

Ключ профиля должен совпадать с предпоследним сегментом `JOB_NAME`.

Пример:
- `CPC/ci_uh_MR/MR-1101` -> профиль `ci_uh_MR`
- `CPC/ci_erp_MR/MR-42` -> профиль `ci_erp_MR`

## Как создать свои файлы подмены
Для каждого файла из `replacements` нужно создать отдельный managed file в Jenkins.

Пример набора для `ci_uh_MR`:
- `debug-ci-uh-mr-jobConfiguration`
- `debug-ci-uh-mr-sonar-properties`
- `debug-ci-uh-mr-vrunner`
- `debug-ci-uh-mr-vaparams`

Содержимое:
- `debug-ci-uh-mr-jobConfiguration` -> ваш `jobConfiguration.json`
- `debug-ci-uh-mr-sonar-properties` -> ваш `sonar-project.properties`
- `debug-ci-uh-mr-vrunner` -> ваш `tools/vrunner.json`
- `debug-ci-uh-mr-vaparams` -> ваш `tools/VAParams.json`

При создании каждого managed file:
- `ID` должен точно совпадать со значением `fileId` из control JSON;
- `Name` можно оставить таким же, как `ID`;
- `Content` должно содержать полный текст соответствующего файла;
- тип файла лучше выбирать по содержимому:
  - для `debug-ci-uh-mr-jobConfiguration` -> `Json file`
  - для `debug-ci-uh-mr-sonar-properties` -> `Properties file`
  - для `debug-ci-uh-mr-vrunner` -> `Json file`
  - для `debug-ci-uh-mr-vaparams` -> `Json file`
- при желании все эти файлы можно хранить и как `Custom file`, механизм библиотеки от этого не меняется.

## Как временно отключить свою настройку
Самый простой способ:

```json
"enabled": false
```

Тогда профиль останется в control JSON, но подмены применяться не будут.

## Что происходит во время сборки
1. В `pre-stage` библиотека определяет профиль по `JOB_NAME`.
2. Загружает `jenkins-debug-overrides-control`.
3. Если профиль найден и включен, раскладывает файлы в workspace.
4. `jobConfiguration.json` используется сразу.
5. Остальные файлы сохраняются в `stash`.
6. На нужных downstream agents файлы восстанавливаются через `unstash`.

## Как проверить, что подмена сработала
В логах сборки должны появиться сообщения вида:

```text
Debug overrides: resolved profile key = ci_uh_MR
Debug overrides: applying 4 replacement(s)
Debug overrides: wrote jobConfiguration.json from managed file debug-ci-uh-mr-jobConfiguration
Debug overrides: stashed files for downstream agents
Debug overrides: restored files from stash
```

Если профиль не применился, в логах будет одно из сообщений:

```text
Debug overrides: profile <name> not found, skip
Debug overrides: profile <name> is disabled, skip
Debug overrides: control file is unavailable, skip
Debug overrides: Config File Provider plugin is unavailable, skip
```

### Что искать в логах Jenkins
Если профиль найден и подмена реально произошла, ищите такие строки:

```text
Debug overrides: resolved profile key = ci_uh_MR
Debug overrides: applying 4 replacement(s)
Debug overrides: wrote jobConfiguration.json from managed file debug-ci-uh-mr-jobConfiguration
Debug overrides: wrote sonar-project.properties from managed file debug-ci-uh-mr-sonar-properties
Debug overrides: wrote tools/vrunner.json from managed file debug-ci-uh-mr-vrunner
Debug overrides: wrote tools/VAParams.json from managed file debug-ci-uh-mr-vaparams
Debug overrides: stashed files for downstream agents
```

Если потом на другом Jenkins agent файлы были успешно восстановлены, будет строка:

```text
Debug overrides: restored files from stash
```

Если подмена не была применена, в логах будет один из вариантов:

```text
Debug overrides: profile ci_uh_MR not found, skip
Debug overrides: profile ci_uh_MR is disabled, skip
Debug overrides: control file is unavailable, skip
Debug overrides: Config File Provider plugin is unavailable, skip
Debug overrides: downstream stash is absent, skip restore
```

Содержимое самих файлов в лог не выводится. В логе виден только:
- найденный профиль;
- факт подмены;
- `fileId`, из которого был взят файл;
- факт восстановления файлов на downstream stages.

## На что обратить внимание
- `fileId` в control JSON должен точно совпадать с `fileId` managed file в Jenkins.
- `target` должен быть относительным путем внутри workspace.
- Абсолютные пути и `..` в `target` не поддерживаются.
- Если в профиле нет downstream-файлов, stash не создается.
- Повторный запуск build просто перезапишет подложенные файлы, это штатно.

## Минимальный пример для быстрого старта
Если хотите проверить только подмену `jobConfiguration.json`, достаточно:

1. Создать managed file:

```text
debug-ci-uh-mr-jobConfiguration
```

2. Указать его в control JSON:

```json
{
  "profiles": {
    "ci_uh_MR": {
      "enabled": true,
      "replacements": [
        {
          "fileId": "debug-ci-uh-mr-jobConfiguration",
          "target": "jobConfiguration.json"
        }
      ]
    }
  }
}
```

После этого пайплайн начнет читать ваш `jobConfiguration.json` из Jenkins, а не из репозитория.
