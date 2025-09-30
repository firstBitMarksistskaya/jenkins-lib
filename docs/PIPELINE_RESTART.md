# Pipeline Restart Support

This Jenkins shared library now supports pipeline restarts from any stage, resolving the `NullPointerException` error that previously occurred when restarting pipelines.

## Problem Solved

Previously, when restarting a Jenkins pipeline from a specific stage (e.g., after a failure), the pipeline would fail with:

```
java.lang.NullPointerException: Cannot get property 'stageFlags' on null object
```

This occurred because the configuration was loaded only in the `pre-stage` and stored in `@Field` variables that don't persist across pipeline restarts.

## Solution

The library now implements a hybrid configuration loading approach that ensures configuration is available even when restarting from later stages:

### 1. Configuration Stashing
The `pre-stage` now automatically stashes the configuration:
- Serializes configuration to JSON using `writeJSON`
- Stashes the configuration file for use by subsequent stages
- Maintains compatibility with existing pipeline execution

### 2. Safe Configuration Loading
A new `safeLoadConfig()` function provides robust configuration access with three-tier fallback:

```groovy
JobConfiguration safeLoadConfig() {
    // 1. Use cached config if available (normal execution)
    if (config != null) {
        return config
    }
    
    // 2. Try to unstash from pre-stage (restart scenario)
    try {
        unstash 'pipeline-config'
        config = jobConfiguration('pipeline-config.json') as JobConfiguration
        // Set agent variables
        return config
    } catch (Exception e) {
        // 3. Fallback to fresh load (ultimate fallback)
        config = jobConfiguration() as JobConfiguration
        // Set agent variables
        return config
    }
}
```

### 3. Updated Stage Conditions
All stage `when` expressions now use `safeLoadConfig()`:

```groovy
when {
    beforeAgent true
    expression { safeLoadConfig().stageFlags.bdd }  // Instead of config.stageFlags.bdd
}
```

## Benefits

- **✅ Pipeline Restart Support**: Restart from any stage without configuration errors
- **✅ Zero Performance Impact**: No overhead during normal pipeline execution
- **✅ Backward Compatible**: Existing configurations work unchanged
- **✅ Robust Fallbacks**: Multiple layers of configuration loading ensure reliability
- **✅ Minimal Changes**: Surgical modifications preserve existing functionality

## Usage

No changes are required in your pipeline configurations. The restart support is automatically enabled for all pipelines using the `pipeline1C()` function.

### Restarting a Pipeline

1. In Jenkins, navigate to the failed build
2. Click "Restart from Stage" 
3. Select the desired stage to restart from
4. The pipeline will resume with proper configuration loading

## Technical Details

### Configuration Persistence
- Configuration is stashed as `pipeline-config` in the pre-stage
- JSON serialization ensures complete configuration preservation
- Agent labels are automatically restored on configuration load

### Error Handling
- Graceful fallback to fresh configuration loading if stash is unavailable
- Warning messages logged when fallback mechanisms are used
- No pipeline failures due to configuration loading issues

### Tested Scenarios
- Normal pipeline execution (no change in behavior)
- Pipeline restart from any stage
- Configuration loading with missing stash data
- Agent variable restoration after restart

## Migration

Existing pipelines automatically benefit from restart support with no migration required. The changes are transparent and maintain full backward compatibility.