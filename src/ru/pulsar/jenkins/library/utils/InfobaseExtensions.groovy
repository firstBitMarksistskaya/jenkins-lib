package ru.pulsar.jenkins.library.utils

import com.cloudbees.groovy.cps.NonCPS
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry

/**
 * Расширения, подключенные к базе, через команды конфигуратора.
 */
class InfobaseExtensions implements Serializable {

    // Свой /Out обязателен: vanessa-runner 2.x читает и удаляет свой файл вывода конфигуратора,
    // а его содержимое печатает только при ошибке
    public static final String LIST_FILE = 'build/tmp/infobase-extensions.txt'

    // Платформа пишет файл списка в UTF-8 с BOM
    private static final String BOM = String.valueOf((char) 0xFEFF)

    /**
     * Имена расширений базы из конфигурации.
     * @param parameters параметры --settings и --uccode для vrunner или пустая строка
     */
    static List<String> list(JobConfiguration config, String parameters) {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        steps.createDir('build/tmp')
        steps.writeFile(LIST_FILE, '', 'UTF-8')

        String command = "${VRunner.getVRunnerPath()} designer" +
            " --additional \"/DumpDBCfgList -AllExtensions /Out $LIST_FILE\"" +
            " --ibconnection \"${config.baseName()}\"$parameters"
        int status = VRunner.exec(command, true)

        String dump = steps.readFile(LIST_FILE, 'UTF-8')
        if (status != 0) {
            // Сообщения конфигуратора попадают в файл списка, а не в файл вывода vanessa-runner:
            // сам vanessa-runner при ошибке пишет "Информации об ошибке нет"
            String output = dump.replace(BOM, '').trim()
            steps.error("Не удалось прочитать список расширений базы ${config.baseName()}, код возврата $status. " +
                "Вывод конфигуратора: ${output ?: 'пусто'}")
        }

        return names(dump)
    }

    /**
     * Команда удаления расширения из базы. Для расширения, которого нет в базе, конфигуратор завершается
     * с ошибкой, поэтому удалять можно только расширения из списка базы.
     */
    static String deleteCommand(JobConfiguration config, String name, String settingsParameter) {
        return "${VRunner.getVRunnerPath()} designer --additional \"/DeleteCfg -Extension $name\"" +
            " --ibconnection \"${config.baseName()}\"$settingsParameter"
    }

    // Имена из файла /DumpDBCfgList: UTF-8 с BOM, по одному имени в строке, для базы без расширений файл пустой
    @NonCPS
    static List<String> names(String dump) {
        return dump.replace(BOM, '').readLines().collect { it.trim() }.findAll { it }
    }

    // Есть ли расширение в списке базы. Имена объектов 1С регистр не различают
    @NonCPS
    static boolean contains(List<String> infobaseExtensions, String name) {
        return infobaseExtensions.any { it.equalsIgnoreCase(name) }
    }
}
