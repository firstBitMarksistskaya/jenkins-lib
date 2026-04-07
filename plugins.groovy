import jenkins.model.*

def plugins = [
    "allure-jenkins-plugin",
    "blueocean",
    "bouncycastle-api",
    "cloudbees-folder",
    "command-launcher",
    "copyartifact",
    "credentials",
    "docker-commons",
    "docker-java-api",
    "docker-workflow",
    "email-ext",
    "file-operations",
    "git",
    "git-client",
    "http_request",
    "jackson2-api",
    "jdk-tool",
    "junit",
    "kubernetes",
    "lockable-resources",
    "matrix-project",
    "nodelabelparameter",
    "pipeline-model-definition",
    "pipeline-stage-view",
    "pipeline-utility-steps",
    "scm-api",
    "script-security",
    "sonar",
    "structs",
    "swarm-agents-cloud",
    "timestamper",
    "workflow-aggregator",
    "workflow-api",
    "workflow-durable-task-step",
    "workflow-cps",
    "workflow-job",
    "workflow-multibranch",
    "workflow-step-api",
    "workflow-support"
    ]

def instance = Jenkins.getInstance()
def pm = instance.getPluginManager()
def uc = instance.getUpdateCenter()

uc.updateAllSites()  // Обновить список плагинов

plugins.each { pluginName ->
    if (!pm.getPlugin(pluginName)) {
        def plugin = uc.getPlugin(pluginName)
        if (plugin) {
            def installFuture = plugin.deploy()
            while(!installFuture.isDone()) {
                sleep(3000)
            }
            println "Установлен: ${pluginName}"
        } else {
            println "Плагин не найден: ${pluginName}"
        }
    } else {
        println "Уже установлен: ${pluginName}"
    }
}