import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.DebugOverrides

boolean call() {
    ContextRegistry.registerDefaultContext(this)

    try {
        unstash DebugOverrides.STASH_NAME
        echo 'Debug overrides: restored files from stash'
        return true
    } catch (Exception exception) {
        if (DebugOverrides.shouldTreatUnstashErrorAsMissingStash(exception)) {
            echo 'Debug overrides: downstream stash is absent, skip restore'
            return false
        }

        throw exception
    }
}
