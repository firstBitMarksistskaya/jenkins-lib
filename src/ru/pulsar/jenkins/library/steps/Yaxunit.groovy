package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

import java.nio.file.Files

class Yaxunit implements Serializable {

    private final JobConfiguration config

    private final String yaxunitPath = 'build/yaxunit.cfe'

    Yaxunit(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.yaxunit) {
            Logger.println("Yaxunit test step is disabled")
            return
        }

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            steps.installLocalDependencies()
        }

        def options = config.yaxunitOptions
        def env = steps.env()

        String vrunnerPath = VRunner.getVRunnerPath()
        String ibConnection = "--ibconnection /F./build/ib"

        // Скачиваем расширение с гитхаба
        String pathToYaxunit = "$env.WORKSPACE/$yaxunitPath"
        FilePath localPathToYaxunit = FileUtils.getFilePath(pathToYaxunit)
        Logger.println("Скачивание Yaxunit в $localPathToYaxunit")
        localPathToYaxunit.copyFrom(new URL('https://github.com/bia-technologies/yaxunit/releases/download/22.11.0/YAXUNIT-22.11.cfe'))

        // Устанавливаем расширение
//        String loadYaxunitCommand = "$vrunnerPath loadext -f $localPathToYaxunit --extension Yaxunit --updatedb $ibConnection"
        String loadYaxunitCommand = vrunnerPath + ' run --command "Путь=' + pathToYaxunit + ';ЗавершитьРаботуСистемы" --execute $runnerRoot/epf/ЗагрузитьРасширениеВРежимеПредприятия.epf ' + ibConnection
        // Устанавливаем тесты
        String loadTestsCommand = "$vrunnerPath  compileext ./src/cfe test --updatedb $ibConnection"

        // Создаем конфиг, т.к. в репо может быть ключ, который не закрывает программу и может повесить конвеер
        // Также путь к отчету в формате junit указывается в конфиге, т.к. мы не знаем на чем стартует агент,
        // поэтому собираем сами. Стоит вынести в отдельный класс
        String junitReport = "build/out/jUnit/yaxunit/yaxunit.xml"
        FilePath pathToJUnitReport = FileUtils.getFilePath("$env.WORKSPACE/$junitReport")
        String junitReportDir = FileUtils.getLocalPath(pathToJUnitReport.getParent())
        String configYaxunit = "test-config.json"
        FilePath pathToConfig = FileUtils.getFilePath("$env.WORKSPACE/$configYaxunit")
//        def data = [
//                'filter' : 'test',
//                'reportPath' : 'ss'
//        ]
//        String data = "{\"filter\": {\"extensions\": [\"test\"]}, \"reportPath\": \"$pathToConfig\"}"
//        def json = new groovy.json.JsonBuilder()
//        json "filter" : "jj", "reportPath" : "ii"
//        def file = new File("$env.WORKSPACE\\$configYaxunit")
//        file.createNewFile()
//        file.write(groovy.json.JsonOutput.prettyPrint(json.toString()))

        // Запускаем тесты
        String command = "$vrunnerPath run --command RunUnitTests=$pathToConfig $ibConnection"

        String vrunnerSettings = options.vrunnerSettings
        if (steps.fileExists(vrunnerSettings)) {
            String vrunnerSettingsCommand = " --settings $vrunnerSettings"

            command += vrunnerSettingsCommand
            loadYaxunitCommand += vrunnerSettingsCommand
            loadTestsCommand += vrunnerSettingsCommand
        }

        steps.withEnv(logosConfig) {
            VRunner.exec(loadYaxunitCommand, true)
            VRunner.exec(loadTestsCommand, true)
            VRunner.exec(command, true)
        }

        // Сохраняем результаты
        steps.junit("$junitReportDir/*.xml", true)
        steps.archiveArtifacts("$junitReportDir/**")

    }
}
