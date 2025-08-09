# EdtToDesignerFormatTransformation Test Suite

## Testing Framework
This test suite uses **JUnit 5 (Jupiter)** with **Mockito** for mocking and **AssertJ** for assertions, following the existing patterns in this Jenkins shared library project.

## Test Coverage

### Unit Tests (`EdtToDesignerFormatTransformationTest.java`)

#### Happy Path Tests
- ✅ Basic transformation with EDT source format (no extensions)
- ✅ Full transformation with extensions enabled
- ✅ Parameterized tests for different EDT versions (2021.1, 2022.1, 2023.1, 2024.1)

#### Edge Cases
- ✅ Non-EDT source formats (DESIGNER, XML) - should skip transformation
- ✅ Empty workspace environment variable
- ✅ Custom workspace paths
- ✅ Null configuration handling

#### Error Scenarios
- ✅ Engine creation failure (IllegalArgumentException)
- ✅ Transformation failure during configuration conversion
- ✅ ZIP operation failure
- ✅ Stash operation failure
- ✅ Extension transformation failure (separate from config transformation)

#### Verification Tests
- ✅ Constant values validation
- ✅ Operation order verification
- ✅ Proper usage of static utilities (Logger, FileUtils, EdtCliEngineFactory)

### Integration Tests (`edtToDesignerFormatTransformationTest.groovy`)
- ✅ Pipeline execution with non-EDT format (should skip)
- ✅ Pipeline execution with EDT format and extensions (handles expected failures gracefully)

## Running Tests

### Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests EdtToDesignerFormatTransformationTest

# Run with coverage
./gradlew test jacocoTestReport
```

### Integration Tests
```bash
# Run integration tests
./gradlew integrationTest
```

## Key Testing Patterns

### Mocking Strategy
- Uses `TestUtils.getMockedStepExecutor()` for consistent step executor mocking
- Mocks all external dependencies (JobConfiguration, EdtCliEngine)
- Uses Mockito's `MockedStatic` for static method mocking with proper try-with-resources
- Proper setup in `@BeforeEach` with context registration

### Assertions
- Uses AssertJ for fluent and readable assertions
- Verifies method invocations with Mockito's `verify()`
- Checks exception types and messages
- Validates operation order with custom tracking

### Test Organization
- `@DisplayName` annotations for clear test descriptions
- `@ParameterizedTest` for testing multiple inputs
- `@ValueSource` and `@EnumSource` for parameterized data
- Follows Given-When-Then structure

## Test Data Coverage

### EDT Versions Tested
- 2021.1
- 2022.1
- 2023.1
- 2024.1

### Source Formats Tested
- EDT (should transform)
- DESIGNER (should skip)
- XML (should skip)

### Workspace Paths
- Standard: `/workspace`
- Custom: `/custom/workspace`
- Empty: `` (empty string)

## Important Implementation Details

### Static Method Mocking
All static method mocks are properly scoped using try-with-resources:
- `Logger` - for logging verification
- `FileUtils` - for file path resolution
- `EdtCliEngineFactory` - for engine creation

### FilePath Usage
Tests use `hudson.FilePath` to match the actual implementation's usage of Jenkins file utilities.

### Error Scenarios
Each error test verifies:
1. The specific exception type is thrown
2. The exception message contains expected text
3. Partial operations complete before failure (where applicable)

## Maintenance Notes

1. **Adding new EDT versions**: Update the `@ValueSource` in `testDifferentEDTVersions`
2. **New source formats**: The `@EnumSource` automatically includes all enum values
3. **Mock updates**: If `TestUtils` changes, review the `setUp()` method
4. **New constants**: Add validation in `testConstantValues()`
5. **Static mocks**: Always use try-with-resources for `MockedStatic` to prevent test pollution