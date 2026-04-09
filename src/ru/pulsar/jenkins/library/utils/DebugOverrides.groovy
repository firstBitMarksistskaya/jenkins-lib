package ru.pulsar.jenkins.library.utils

class DebugOverrides implements Serializable {

    static final String CONTROL_FILE_ID = 'jenkins-debug-overrides-control'
    static final String CONTROL_FILE_VARIABLE = 'DEBUG_OVERRIDES_CONTROL_FILE'
    static final String STASH_NAME = 'debug-overrides-files'

    static final List<String> DOWNSTREAM_TARGETS = [
        'sonar-project.properties',
        'tools/vrunner.json',
        'tools/VAParams.json'
    ].asImmutable()

    static String resolveProfileKey(String jobName) {
        if (jobName == null) {
            return null
        }

        List<String> segments = jobName
            .split('/')
            .collect { it?.trim() }
            .findAll { it }

        if (segments.size() < 2) {
            return null
        }

        return segments[-2]
    }

    static String normalizeTarget(String target) {
        if (target == null) {
            return null
        }

        String normalized = target.trim().replace('\\', '/')

        while (normalized.startsWith('./')) {
            normalized = normalized.substring(2)
        }

        return normalized
    }

    static void validateDebugProfile(Map profile) {
        if (profile == null) {
            throw new IllegalArgumentException('Debug overrides profile is null')
        }

        def replacements = profile.replacements
        if (!(replacements instanceof List)) {
            throw new IllegalArgumentException('Debug overrides profile must contain a replacements array')
        }

        replacements.eachWithIndex { replacement, index ->
            if (!(replacement instanceof Map)) {
                throw new IllegalArgumentException("Replacement at index ${index} must be an object")
            }

            if (!replacement.fileId?.toString()?.trim()) {
                throw new IllegalArgumentException("Replacement at index ${index} must contain non-empty fileId")
            }

            validateTargetPath(replacement.target?.toString())
        }
    }

    static void validateTargetPath(String target) {
        String normalized = normalizeTarget(target)

        if (!normalized) {
            throw new IllegalArgumentException('Replacement target must be non-empty')
        }

        if (normalized.startsWith('/')) {
            throw new IllegalArgumentException("Absolute target paths are not allowed: ${target}")
        }

        if (normalized ==~ /^[A-Za-z]:\/.*/) {
            throw new IllegalArgumentException("Windows absolute target paths are not allowed: ${target}")
        }

        if (normalized.startsWith('//')) {
            throw new IllegalArgumentException("UNC target paths are not allowed: ${target}")
        }

        List<String> segments = normalized.split('/') as List<String>
        if (segments.any { it == '..' }) {
            throw new IllegalArgumentException("Target path traversal is not allowed: ${target}")
        }
    }

    static List<Map<String, String>> buildConfigFileProviderEntries(List replacements) {
        List<Map<String, String>> entries = []

        replacements.eachWithIndex { replacement, index ->
            entries.add([
                fileId  : replacement.fileId.toString(),
                variable: "DEBUG_OVERRIDE_FILE_${index}"
            ])
        }

        return entries
    }

    static List<String> collectDownstreamTargets(List replacements) {
        replacements
            .collect { normalizeTarget(it.target?.toString()) }
            .findAll { it in DOWNSTREAM_TARGETS }
            .unique()
    }

    static String buildStashIncludes(List<String> targets) {
        targets.join(',')
    }

    static String parentPath(String target) {
        String normalized = normalizeTarget(target)
        int lastSlash = normalized.lastIndexOf('/')

        if (lastSlash <= 0) {
            return ''
        }

        return normalized.substring(0, lastSlash)
    }

    static boolean shouldTreatConfigFileProviderErrorAsMissingPlugin(Exception exception) {
        String message = exception?.message ?: ''

        return exception instanceof MissingMethodException ||
            exception instanceof NoSuchMethodError ||
            message.contains("No such DSL method 'configFileProvider'") ||
            message.contains("No such DSL method 'configFile'") ||
            (
                message.contains('configFileProvider') &&
                message.toLowerCase().contains('no such dsl method')
            )
    }

    static boolean shouldTreatConfigFileProviderErrorAsMissingControlFile(Exception exception) {
        String message = exception?.message ?: ''
        String lowerCaseMessage = message.toLowerCase()

        return message.contains(CONTROL_FILE_ID) && (
            lowerCaseMessage.contains('not found') ||
            lowerCaseMessage.contains('no such file') ||
            lowerCaseMessage.contains('can\'t be resolved') ||
            lowerCaseMessage.contains('cannot be resolved') ||
            lowerCaseMessage.contains('unable to find') ||
            lowerCaseMessage.contains('managed file')
        )
    }

    static boolean shouldTreatUnstashErrorAsMissingStash(Exception exception) {
        String message = exception?.message ?: ''

        return message.contains("No such saved stash '${STASH_NAME}'") ||
            message.contains("No such saved stash ‘${STASH_NAME}’") ||
            (message.contains('No such saved stash') && message.contains(STASH_NAME))
    }
}
