package ru.pulsar.jenkins.library.utils

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry

/**
 * Пароль пользователя ИБ для клиента тестирования Vanessa Automation.
 * Менеджер тестирования запускает vanessa-runner с пользователем и паролем, а клиент тестирования "Этот клиент"
 * запускает сама VA: с /N пользователя менеджера, но без пароля. Дополнительные ключи запуска клиентов тестирования
 * VA берет из настройки ДополнительныеПараметрыЗапускаКлиентаТестирования, поэтому команда vanessa получает копию
 * своих настроек VA с паролем в этой настройке. Копия лежит вне рабочего каталога и удаляется после команды.
 */
class VanessaTestClient implements Serializable {

    private static final String DEFAULT_RUNNER_SETTINGS = 'env.json'

    // Имена настроек VA в нижнем регистре, русское и английское: VA принимает оба и не различает регистр
    private static final List<String> ADDITIONAL_PARAMETERS = [
        'дополнительныепараметрызапускаклиентатестирования', 'additionalparametersforlaunchingthetestingclient'
    ]
    private static final List<String> MASK_PASSWORD = ['маскироватьпарольвлоге', 'maskpwdinlog']
    private static final List<String> TEST_CLIENTS = ['данныеклиентовтестирования', 'datatestclients']
    private static final List<String> CLIENT_PARAMETERS = ['доппараметры', 'additionalparameters']

    /**
     * Выполняет тело с командой vrunner. Команда vanessa под учетными данными ИБ с паролем получает
     * --vanessasettings с копией своих настроек VA, остальные команды передаются без изменений.
     */
    static def withPassword(JobConfiguration config, String command, boolean credentialsBound, Closure body) {
        if (!credentialsBound || !isVanessaCommand(command)) {
            return body.call(command)
        }

        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env()
        if (InfobaseCredentials.isPasswordEmpty(env)) {
            // Без пароля VA сама запускает клиент тестирования с /N пользователя менеджера
            return body.call(command)
        }

        String settingsPath = settingsPath(command, env)
        String sourcePath = settingsPath ? FileUtils.getAbsolutePath(settingsPath, env) : null
        if (!sourcePath || !steps.fileExists(sourcePath)) {
            Logger.println("Файл настроек Vanessa Automation для команды не найден, " +
                "клиент тестирования запускается без пароля пользователя ИБ")
            return body.call(command)
        }

        String tmpDir = InfobaseCredentials.envValue(env, 'WORKSPACE_TMP') ?: "${env.WORKSPACE}@tmp"
        FilePath copy = FileUtils.getFilePath("$tmpDir/infobase-credentials/vanessa-${UUID.randomUUID()}.json")
        String problem = writeCopy(FileUtils.getFilePath(sourcePath), copy, env, steps.isUnix())
        if (problem) {
            steps.error("Настройки Vanessa Automation $settingsPath: $problem")
        }

        Logger.println("Клиент тестирования Vanessa Automation получит пароль пользователя ИБ " +
            "из копии настроек $settingsPath вне рабочего каталога")
        try {
            return body.call(withSettings(command, copy.getRemote()))
        } finally {
            copy.delete()
            InfobaseCredentials.clearTestClientEventLog(config)
        }
    }

    @NonCPS
    static boolean isVanessaCommand(String command) {
        List<String> tokens = RunnerSettings.tokens(command)
        return tokens.size() > 1 && tokens[1] == 'vanessa'
    }

    /**
     * Файл настроек VA команды vanessa, как его выбирает vanessa-runner: --vanessasettings из командной строки,
     * переменная окружения RUNNER_VANESSASETTINGS, затем файл настроек vanessa-runner. null, если файл не задан.
     */
    static String settingsPath(String command, def env) {
        String path = RunnerSettings.argument(command, '--vanessasettings')
        if (!path) {
            path = InfobaseCredentials.envValue(env, 'RUNNER_VANESSASETTINGS')
        }
        if (!path) {
            String runnerSettings = RunnerSettings.argument(command, '--settings') ?: DEFAULT_RUNNER_SETTINGS
            path = RunnerSettings.value(FileUtils.getAbsolutePath(runnerSettings, env), 'vanessa', '--vanessasettings')
        }
        return path ? path.replace('$workspaceRoot', env.WORKSPACE as String) : null
    }

    // Команда с файлом настроек VA вместо указанного в ней: параметр командной строки важнее файла настроек
    @NonCPS
    static String withSettings(String command, String settingsPath) {
        String withoutSettings = command.replaceAll(/\s--vanessasettings\s+("[^"]*"|\S+)/, '')
        return "$withoutSettings --vanessasettings \"$settingsPath\"".toString()
    }

    // Копия настроек VA с паролем клиента тестирования. Возвращает причину отказа или null. Пароль не попадает
    // ни в состояние Pipeline, ни в аргументы шагов: файл пишется на агенте напрямую
    @NonCPS
    private static String writeCopy(FilePath source, FilePath copy, def env, boolean unix) {
        String problem = InfobaseCredentials.testClientPasswordProblem(env, unix)
        if (problem) {
            return problem
        }
        String password = InfobaseCredentials.envValue(env, InfobaseCredentials.PASSWORD_VARIABLE)

        def settings
        try {
            settings = new JsonSlurper().parseText(RunnerSettings.read(source))
        } catch (Exception e) {
            return "не удалось прочитать файл: ${e.message}"
        }
        if (!(settings instanceof Map)) {
            return 'ожидается объект JSON'
        }
        if (containsPassword(settings)) {
            return 'указан пароль клиента тестирования (/P). С учетными данными ИБ из Jenkins пароль берется ' +
                'из них, уберите его из настроек VA'
        }

        String parameter = "/P\"$password\""
        if (!appendToAdditionalParameters(settings, parameter)) {
            settings.put('ДополнительныеПараметрыЗапускаКлиентаТестирования', parameter)
        }
        // VA маскирует пароль в своем журнале выполнения сценариев
        if (!setMaskPassword(settings)) {
            settings.put('МаскироватьПарольВЛоге', true)
        }

        // Права на Linux ограничиваются до записи пароля, на Windows chmod ничего не делает
        copy.write('', 'UTF-8')
        copy.chmod(0600)
        copy.write(JsonOutput.toJson(settings), 'UTF-8')
        return null
    }

    @NonCPS
    private static boolean containsPassword(def node) {
        if (node instanceof List) {
            return node.any { containsPassword(it) }
        }
        if (!(node instanceof Map)) {
            return false
        }
        return node.any { key, value ->
            String name = key.toString().toLowerCase(Locale.ROOT)
            if (name in ADDITIONAL_PARAMETERS && value instanceof String) {
                return hasPasswordKey(value)
            }
            if (name in TEST_CLIENTS && value instanceof List) {
                return value.any { client ->
                    client instanceof Map && client.any { clientKey, clientValue ->
                        clientKey.toString().toLowerCase(Locale.ROOT) in CLIENT_PARAMETERS &&
                            clientValue instanceof String && hasPasswordKey(clientValue)
                    }
                }
            }
            return containsPassword(value)
        }
    }

    // Ключ пароля /P среди ключей запуска 1С. /Proxy - другой ключ
    @NonCPS
    private static boolean hasPasswordKey(String parameters) {
        return parameters =~ /(?i)(?:^|\s)\/P(?!roxy(?:\s|$))/
    }

    @NonCPS
    private static boolean appendToAdditionalParameters(Map settings, String parameter) {
        boolean found = false
        settings.each { key, value ->
            if (key.toString().toLowerCase(Locale.ROOT) in ADDITIONAL_PARAMETERS && (value == null || value instanceof String)) {
                String current = value?.trim()
                settings[key] = current ? "$current $parameter".toString() : parameter
                found = true
            } else if (value instanceof Map) {
                found = appendToAdditionalParameters(value, parameter) || found
            }
        }
        return found
    }

    @NonCPS
    private static boolean setMaskPassword(Map settings) {
        boolean found = false
        settings.each { key, value ->
            if (key.toString().toLowerCase(Locale.ROOT) in MASK_PASSWORD) {
                settings[key] = true
                found = true
            } else if (value instanceof Map) {
                found = setMaskPassword(value) || found
            }
        }
        return found
    }
}
