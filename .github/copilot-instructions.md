# Jenkins Shared Library for 1C:Enterprise 8

This is a Jenkins shared library that provides standardized CI/CD pipeline functionality for 1C:Enterprise 8 applications. The library is built with Gradle and written primarily in Groovy with Java support classes.

**ALWAYS follow these instructions exactly as written. Only search for additional information or run exploratory bash commands if the information in these instructions is incomplete or found to be incorrect.**

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Environment Requirements
- Java 17 (as specified in GitHub Actions workflow)
- Gradle 7.6.1 (via wrapper)
- Network access to Jenkins and Maven repositories
- Git for version control

### Bootstrap and Build Process
```bash
# Basic validation commands
./gradlew --version
./gradlew clean
./gradlew tasks

# Main build commands with reliable performance
./gradlew build      # Takes ~2 minutes consistently
./gradlew test       # Takes ~25 seconds consistently  
./gradlew check      # Takes ~1 minute when cached
```

### Build Dependencies and Network Access
Jenkins repositories are fully accessible with excellent connectivity:
- `repo.jenkins-ci.org` - Jenkins core and plugins
- `repo.maven.apache.org` - Maven central

Repository connectivity characteristics:
- All dependency downloads work reliably
- No timeout issues with AWS S3 CDN endpoints
- Build times are consistent and reasonable
- All commands execute successfully without network-related failures

### Core Development Commands
```bash
# Clean build artifacts
./gradlew clean

# Compilation and testing commands
./gradlew compileGroovy compileJava    # Compile sources
./gradlew test                         # Run unit tests (~25s)
./gradlew integrationTest             # Run integration tests
./gradlew build                       # Full build (~2min)
./gradlew jacocoTestReport            # Generate test coverage report
./gradlew check                       # Run all verification tasks
```

## Validation and Testing

### Manual Validation Scenarios

1. **Basic Repository Validation**:
   ```bash
   ./gradlew clean    # Verify Gradle setup
   ./gradlew tasks    # List available tasks
   ```

2. **Source Code Compilation**:
   ```bash
   ./gradlew compileGroovy compileJava
   ```

3. **JSON Configuration Validation**:
   ```bash
   # Validate JSON syntax for configuration files
   python3 -m json.tool resources/globalConfiguration.json > /dev/null && echo "Valid JSON" || echo "Invalid JSON"
   python3 -m json.tool resources/schema.json > /dev/null && echo "Valid JSON" || echo "Invalid JSON"
   python3 -m json.tool resources/yaxunit.json > /dev/null && echo "Valid JSON" || echo "Invalid JSON"
   
   # Validate test configuration files
   python3 -m json.tool test/unit/resources/jobConfiguration.json > /dev/null && echo "Valid JSON" || echo "Invalid JSON" 
   python3 -m json.tool test/integration/resources/jobConfiguration.json > /dev/null && echo "Valid JSON" || echo "Invalid JSON"
   ```

4. **Manual Code Review Checklist**:
   - Check Groovy syntax in `/vars/*.groovy` files for obvious errors
   - Verify new pipeline steps follow existing patterns
   - Ensure proper error handling in pipeline steps
   - Validate configuration schema changes match actual usage
   - Review test files for coverage of new functionality

5. **Git-based Validation**:
   ```bash
   # Check what files were modified
   git status
   git diff
   
   # Ensure no large binary files or build artifacts are included
   git ls-files --others --exclude-standard
   ```

## Pipeline Functionality and Validation Scenarios

### Understanding the Pipeline
The main `pipeline1C()` function in `vars/pipeline1C.groovy` orchestrates a comprehensive CI/CD process for 1C:Enterprise 8 projects:

**Key Pipeline Stages**:
1. **Pre-stage**: Environment setup and configuration loading
2. **Подготовка (Preparation)**: Database and extension preparation
   - EDT format transformation (if needed)
   - Database creation and configuration loading
   - Extension building and loading
   - Database initialization and migration
3. **Проверка качества (Quality Control)**: 
   - EDT validation
   - Syntax checking
   - SonarQube analysis
4. **Testing Stages**:
   - BDD scenario testing
   - YAXUnit unit testing  
   - Smoke testing
5. **Notifications**: Email and Telegram notifications

### Manual Validation Scenarios for Pipeline Changes

**After modifying pipeline steps** (`vars/*.groovy`):

1. **Configuration Loading Test**:
   - Modify `test/integration/resources/jobConfiguration.json` 
   - Verify your changes work with different configuration options
   - Test configuration inheritance from `resources/globalConfiguration.json`

2. **Pipeline Step Logic Review**:
   - For changes to `vars/bdd.groovy`: Review BDD test execution logic
   - For changes to `vars/syntaxCheck.groovy`: Review 1C syntax validation
   - For changes to `vars/initInfobase.groovy`: Review database initialization
   - For changes to `vars/sonarScanner.groovy`: Review SonarQube integration

3. **Timeout and Agent Configuration**:
   - Verify timeout values in `resources/globalConfiguration.json` -> `timeout` section
   - Check agent label configuration in pipeline steps
   - Validate stage conditional logic (when clauses)

4. **Error Handling Validation**:
   - Review try/catch blocks in modified pipeline steps
   - Ensure proper error messages and notifications
   - Check cleanup procedures in finally blocks

5. **Integration Points**:
   - After modifying configuration classes: Check `src/ru/pulsar/jenkins/library/configuration/`
   - After adding new pipeline steps: Update integration tests in `test/integration/groovy/`
   - After schema changes: Update `resources/schema.json`

### Pre-commit Validation

```bash
# Optimal validation commands with reliable build times
./gradlew check      # Takes ~1-2 minutes

# Individual commands for iterative development:
./gradlew clean      # Always works
./gradlew build      # Takes ~2 minutes  
./gradlew test       # Takes ~25 seconds
```

## Project Structure and Navigation

### Key Directories
```
/vars/                  # Jenkins pipeline steps (Groovy DSL)
  ├── pipeline1C.groovy       # Main pipeline entry point
  ├── initInfobase.groovy     # Database initialization
  ├── syntaxCheck.groovy      # 1C syntax checking
  ├── bdd.groovy              # BDD test execution
  ├── yaxunit.groovy          # Unit test execution
  └── *.groovy                # Other pipeline steps

/src/                   # Java support classes
  └── ru/pulsar/jenkins/library/
    ├── configuration/        # Configuration handling
    ├── steps/               # Step implementations
    └── utils/               # Utility classes

/resources/             # Configuration and schemas
  ├── globalConfiguration.json # Default pipeline configuration
  ├── schema.json             # JSON schema for validation
  └── yaxunit.json            # YAXUnit test configuration

/test/                  # Test suites
  ├── unit/                   # Unit tests (Java)
  └── integration/            # Integration tests (Groovy)
```

### Important Files to Check After Changes
- Always validate `resources/globalConfiguration.json` syntax after configuration changes
- Check `vars/pipeline1C.groovy` when modifying main pipeline flow
- Update `resources/schema.json` when adding new configuration options
- Review test files in `/test/integration/groovy/` when adding new pipeline steps

## Configuration and Customization

### Pipeline Configuration
- Default configuration: `resources/globalConfiguration.json`
- Project-specific overrides: `jobConfiguration.json` in repository root
- Schema validation: `resources/schema.json`

### Common Configuration Tasks
- Modify timeout values in `globalConfiguration.json` -> `timeout` section
- Add new pipeline stages in `stages` section
- Configure notification settings in `notifications` section
- Set up 1C version and agent requirements

## Common Issues and Troubleshooting

### Build Failures
1. **Network/Repository Issues**: 
   - Error: "repo.jenkins-ci.org: No address associated with hostname"
   - Solution: Check network connectivity, retry later

2. **Dependency Resolution**:
   - Error: "Could not resolve org.jenkins-ci.main:jenkins-core"
   - Solution: Ensure network access to Jenkins repositories

3. **Test Failures**:
   - Check `build/reports/tests/` for detailed test results
   - Review integration test logs for Jenkins-specific issues

### Development Workflow
1. Make changes to Groovy files in `/vars/` or Java files in `/src/`
2. Run `./gradlew compileGroovy` for quick syntax validation
3. Run `./gradlew test` for unit tests (~25s)
4. Run `./gradlew integrationTest` for full pipeline testing
5. Run `./gradlew check` before committing (~1min)

## Performance and Timing Expectations

- Basic tasks (`clean`, `tasks`): < 1 minute
- Compilation (`compileGroovy`): < 1 minute  
- Unit tests (`test`): ~25 seconds
- Integration tests (`integrationTest`): ~1-2 minutes
- Full build (`build`): ~2 minutes
- Full verification (`check`): ~1 minute (when artifacts cached)

All timings are consistent and reliable with excellent network access to dependencies.

## Common Development Tasks

### Adding a New Pipeline Step
1. Create new Groovy file in `/vars/` directory (e.g., `vars/myNewStep.groovy`)
2. Follow existing patterns from other pipeline steps
3. Add configuration options to `resources/globalConfiguration.json` if needed
4. Update `resources/schema.json` for new configuration fields
5. Add unit tests in `/test/unit/groovy/ru/pulsar/jenkins/library/`
6. Add integration test in `/test/integration/groovy/`
7. Update timeout configurations if step is long-running

### Modifying Configuration Schema
1. Edit `resources/globalConfiguration.json` for default values
2. Update `resources/schema.json` for validation rules
3. Modify Java configuration classes in `src/ru/pulsar/jenkins/library/configuration/`
4. Test with sample configurations in test directories
5. Validate JSON syntax: `python3 -m json.tool resources/globalConfiguration.json`

### Updating Pipeline Logic
1. Focus on main pipeline: `vars/pipeline1C.groovy`
2. Individual stages: `vars/initInfobase.groovy`, `vars/bdd.groovy`, etc.
3. Always check conditional logic (`when` expressions)
4. Verify agent label assignments
5. Update timeout values in configuration if needed

### Working with 1C-Specific Features
- Database initialization: `vars/initInfobase.groovy`
- Extension management: `vars/loadExtensions.groovy`  
- Syntax checking: `vars/syntaxCheck.groovy`
- Format transformation: `vars/edtToDesignerFormatTransformation.groovy`
- BDD testing: `vars/bdd.groovy`
- Unit testing: `vars/yaxunit.groovy`

## CI/CD Integration

The project uses GitHub Actions (`.github/workflows/gradle.yml`):
- Runs on Ubuntu with Java 17  
- Executes `./gradlew check`
- Requires network access to Jenkins repositories
- Validates on every push and pull request

### GitHub Actions Workflow
```yaml
# From .github/workflows/gradle.yml
- uses: actions/setup-java@v4
  with:
    java-version: '17'  # Java 17 is required
    distribution: 'temurin'
- uses: gradle/actions/setup-gradle@v4
- run: ./gradlew check  # Main validation command
```

### Expected Behavior in CI
- **Success**: All builds, tests, and checks pass
- **Network Status**: Jenkins repositories are accessible
- **Build Time**: 5-10 minutes for full `./gradlew check`
- **Artifacts**: Test reports and coverage reports in `build/` directory

## Validated Commands Reference

All build, test, and validation commands work reliably. Jenkins repository access is excellent and all artifacts download successfully.

### Core Commands
```bash
./gradlew --version          # Version information
./gradlew clean             # Clean build artifacts  
./gradlew tasks             # List available tasks
./gradlew compileGroovy     # Compile Groovy sources
./gradlew compileJava       # Compile Java sources
./gradlew dependencies      # Show dependency tree
./gradlew test              # Run unit tests (~25s)
./gradlew build             # Full build (~2min)
./gradlew check             # All verification tasks (~1min when cached)
./gradlew integrationTest   # Run integration tests

# JSON validation commands
python3 -m json.tool resources/globalConfiguration.json
python3 -m json.tool test/unit/resources/jobConfiguration.json  
python3 -m json.tool test/integration/resources/jobConfiguration.json
```