# Описание интеграционных тестов

## Общая информация

Все интеграционные тесты используют:
- **Jenkins Test Harness** для запуска тестового экземпляра Jenkins
- **RuleBootstrapper** для настройки локальной библиотеки как shared library
- **JUnit 4** для структуры тестов
- **Declarative Pipeline** для проверки работы в реальном Jenkins окружении

---

## 1. `jobConfigurationTest.groovy`

**Назначение:** Проверка функции `jobConfiguration()` для чтения и слияния конфигураций.

### Тесты:

1. **`"jobConfiguration should not fail without file"`**
   - Проверяет, что `jobConfiguration()` не падает при отсутствии файла конфигурации
   - Использует значения по умолчанию
   - Ожидается успешное выполнение пайплайна

2. **`"jobConfiguration should merge configurations"`**
   - Проверяет слияние конфигураций из файла `jobConfiguration.json`
   - Создает файл конфигурации в workspace и проверяет значения:
     - `v8version='8.3.12.1500'`
     - `sonarScannerToolName='sonar-scanner'`
     - `initMethod=FROM_SOURCE`
     - `dbgsPath=C:\Program files\1cv8\8.3.12.1500\bin\dbgs.exe`
     - `coverage41CPath=C:\coverage\Coverage41C.exe`

---

## 2. `pipeline1cTest.groovy`

**Назначение:** Проверка основного пайплайна `pipeline1C()`.

### Тесты:

1. **`"pipeline1C should do something"`**
   - Проверяет базовое выполнение `pipeline1C()`
   - Создает агента с меткой `"agent"`
   - Проверяет наличие `'(pre-stage)'` в логах
   - Минимальная проверка, что пайплайн запускается

---

## 3. `cmdTest.groovy`

**Назначение:** Проверка функции `cmd()` для выполнения команд.

### Тесты:

1. **`"cmd should echo something"`**
   - Проверяет выполнение команды `echo helloWorld`
   - Проверяет наличие `'helloWorld'` в логах
   - Проверяет базовое выполнение команды

2. **`"cmd should return status"`**
   - Проверяет возврат кода возврата команды
   - Выполняет `cmd("false", true)` (второй параметр - возврат статуса)
   - Проверяет, что статус равен `1` (команда `false` возвращает 1)
   - Проверяет корректную обработку кода возврата

---

## 4. `printLocationTest.groovy`

**Назначение:** Проверка функции `printLocation()` для логирования информации о ноде.

### Тесты:

1. **`"Logger should echo current node name"`**
   - Проверяет вывод имени текущего нода
   - Проверяет наличие `'Running on node built-in'` в логах
   - Проверяет корректное определение нода выполнения

---

## 5. `RuleBootstrapper.groovy` (вспомогательный класс)

**Назначение:** Утилита для настройки тестового окружения.

### Функции:
- Настраивает `JenkinsRule` для использования локального исходного кода как shared library
- Создает `LibraryConfiguration` с именем `'testLibrary'`
- Устанавливает библиотеку как неявно загружаемую (`implicit = true`)
- Устанавливает таймаут тестов в 30 секунд
- Используется во всех тестах через аннотацию `@Before`

---

## Ресурсы

### `test/integration/resources/jobConfiguration.json`

Тестовый файл конфигурации, содержит:
- Версию платформы 1С: `8.3.12.1500`
- Настройки инициализации ИБ (`fromSource`)
- Пример расширения конфигурации
- Настройки покрытия кода (пути к `dbgs.exe` и `Coverage41C.exe`)

---

## Итоговая статистика

- **Всего тестовых классов:** 4
- **Всего тестов:** 6
- **Вспомогательных классов:** 1 (`RuleBootstrapper`)
- **Тестовых ресурсов:** 1 (`jobConfiguration.json`)

## Покрытие функциональности

- ✅ Чтение и слияние конфигураций (`jobConfiguration`)
- ✅ Основной пайплайн (`pipeline1C`)
- ✅ Выполнение команд (`cmd`)
- ✅ Логирование информации о ноде (`printLocation`)

---

## Запуск тестов

Для запуска всех интеграционных тестов используйте:

```bash
.\gradlew.bat integrationTest
```

Для запуска конкретного теста:

```bash
.\gradlew.bat integrationTest --tests "jobConfigurationTest"
```

Тесты проверяют базовую функциональность библиотеки в реальном окружении Jenkins через Jenkins Test Harness.
