import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.DebugOverrides

boolean call() {
    ContextRegistry.registerDefaultContext(this)

    String profileKey = DebugOverrides.resolveProfileKey(env.JOB_NAME as String)
    if (!profileKey) {
        echo 'Debug overrides: profile key is unavailable, skip'
        return false
    }

    echo "Debug overrides: resolved profile key = ${profileKey}"

    Map controlConfig = loadControlConfig()
    if (controlConfig == null) {
        return false
    }

    Map profile = controlConfig.profiles?."${profileKey}" as Map
    if (profile == null) {
        echo "Debug overrides: profile ${profileKey} not found, skip"
        return false
    }

    if (!(profile.enabled as boolean)) {
        echo "Debug overrides: profile ${profileKey} is disabled, skip"
        return false
    }

    try {
        DebugOverrides.validateDebugProfile(profile)
    } catch (IllegalArgumentException exception) {
        echo "Debug overrides: invalid profile ${profileKey}, skip (${exception.message})"
        return false
    }

    List replacements = profile.replacements as List
    echo "Debug overrides: applying ${replacements.size()} replacement(s)"

    List<Map<String, String>> entries = DebugOverrides.buildConfigFileProviderEntries(replacements)
    List<String> downstreamTargets = []

    configFileProvider(entries.collect { configFile(fileId: it.fileId, variable: it.variable) }) {
        replacements.eachWithIndex { replacement, index ->
            String target = DebugOverrides.normalizeTarget(replacement.target.toString())
            String sourcePath = env[entries[index].variable]
            String parentPath = DebugOverrides.parentPath(target)

            if (parentPath) {
                createDir(parentPath)
            }

            writeFile file: target, text: readFile(file: sourcePath), encoding: 'UTF-8'
            echo "Debug overrides: wrote ${target} from managed file ${replacement.fileId}"
        }

        downstreamTargets = DebugOverrides.collectDownstreamTargets(replacements)
    }

    if (!downstreamTargets.isEmpty()) {
        stash name: DebugOverrides.STASH_NAME, includes: DebugOverrides.buildStashIncludes(downstreamTargets)
        echo 'Debug overrides: stashed files for downstream agents'
    }

    return true
}

private Map loadControlConfig() {
    try {
        Map controlConfig

        configFileProvider([
            configFile(fileId: DebugOverrides.CONTROL_FILE_ID, variable: DebugOverrides.CONTROL_FILE_VARIABLE)
        ]) {
            String controlPath = env[DebugOverrides.CONTROL_FILE_VARIABLE]
            controlConfig = readJSON(file: controlPath) as Map
        }

        return controlConfig
    } catch (Exception exception) {
        if (DebugOverrides.shouldTreatConfigFileProviderErrorAsMissingPlugin(exception)) {
            echo 'Debug overrides: Config File Provider plugin is unavailable, skip'
            return null
        }

        if (DebugOverrides.shouldTreatConfigFileProviderErrorAsMissingControlFile(exception)) {
            echo 'Debug overrides: control file is unavailable, skip'
            return null
        }

        if (DebugOverrides.shouldTreatConfigFileProviderErrorAsInvalidControlFile(exception)) {
            echo "Debug overrides: control file is invalid, skip (${exception.message})"
            return null
        }

        throw exception
    }
}
