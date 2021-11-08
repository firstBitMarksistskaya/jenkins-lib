import ru.pulsar.jenkins.library.utils.Logger

def call() {
    if (!fileExists("packagedef")) {
        return
    }

    Logger.println("Установка локальных зависимостей OneScript")
    cmd("opm install -l")
}