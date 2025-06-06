# Android Testing and CI Setup

This document describes the testing and CI setup for Android projects in this repository.

## Overview

The Android projects in this repository now have comprehensive testing and CI setup including:

- Unit tests for all modules
- Instrumented tests for Android-specific functionality  
- Automated CI/CD pipeline with GitHub Actions
- Test result artifacts and APK builds

## Project Structure

The following Android modules have test configurations:

- `depollsoft.lib.kotlin` - Main Kotlin library
- `PitchPerfect` - Main PitchPerfect application
- `PitchPerfectLib` - Core PitchPerfect library
- `PitchPerfectWear` - Wear OS application
- `TagMaster` - TagMaster application
- `DepollSoftCommon` - Common utilities library
- `DepollSoftCommon.Compat` - Compatibility layer

## Running Tests Locally

### Unit Tests
```bash
cd Android
./gradlew testDebugUnitTest
```

### Instrumented Tests (requires Android device/emulator)
```bash
cd Android
./gradlew connectedDebugAndroidTest
```

### Lint Checks
```bash
cd Android
./gradlew lintDebug
```

### Build All Projects
```bash
cd Android
./gradlew build
```

## CI/CD Pipeline

The GitHub Actions workflow (`.github/workflows/android.yml`) automatically:

1. **Unit Testing Job (Ubuntu)**:
   - Runs unit tests for all modules
   - Performs lint checks
   - Builds debug APKs
   - Uploads test results and build artifacts

2. **Instrumented Testing Job (macOS)**:
   - Sets up Android emulator
   - Runs instrumented tests on emulator
   - Uploads instrumented test results

### Trigger Conditions

CI runs on:
- Push to `main` or `develop` branches
- Pull requests targeting `main` or `develop` branches
- Only when Android-related files are changed

### Artifacts

The CI pipeline uploads:
- Test results (retained for 30 days)
- Lint reports (retained for 30 days)
- Debug APKs (retained for 7 days)
- Instrumented test results (retained for 30 days)

## Test Framework

- **Unit Tests**: JUnit 4
- **Instrumented Tests**: AndroidX Test with Espresso
- **Test Runner**: AndroidJUnitRunner

## Adding New Tests

### Unit Tests
Create test files in `src/test/java/` directory of your module:

```java
package your.package.name;

import org.junit.Test;
import static org.junit.Assert.*;

public class YourTest {
    @Test
    public void yourTestMethod() {
        // Test implementation
        assertEquals(expected, actual);
    }
}
```

### Instrumented Tests
Create test files in `src/androidTest/java/` directory of your module:

```java
package your.package.name;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class YourInstrumentedTest {
    @Test
    public void yourTestMethod() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Test implementation
    }
}
```

## Requirements

- **Java**: JDK 11
- **Android SDK**: Latest (managed by CI)
- **Gradle**: 7.3.3 (via wrapper)
- **Android Gradle Plugin**: 7.2.2
- **Kotlin**: 1.7.10

## Troubleshooting

### Local Build Issues
1. Ensure Android SDK is properly installed
2. Check that `ANDROID_HOME` environment variable is set
3. Verify internet connection for dependency downloads

### CI Issues
1. Check GitHub Actions logs for detailed error messages
2. Verify that changes don't break existing functionality
3. Ensure test dependencies are properly configured

## Future Improvements

Potential enhancements for the testing setup:

- [ ] Code coverage reporting
- [ ] Integration with SonarQube
- [ ] Performance testing
- [ ] UI testing with Espresso
- [ ] Screenshot testing
- [ ] Parallel test execution