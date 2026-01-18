# UI Testing Setup Summary

## Overview

This document summarizes the comprehensive UI testing infrastructure that has been added to both iOS and Android platforms for the PitchPerfect and TagMaster apps.

## Android UI Tests

### Test Framework
- **Framework**: Espresso (espresso-core, espresso-contrib, espresso-intents)
- **Test Runner**: AndroidJUnit4
- **Dependencies**: Already present in build.gradle files

### PitchPerfect Android Tests
Location: `Android/PitchPerfect/tests/java/depollsoft/pitchperfect/`

| File | Coverage | Test Count |
|------|----------|------------|
| `PitchPerfectActivityTest.kt` | Main activity, navigation, tabs, menus | 12 tests |
| `PitchPipeFragmentTest.kt` | 12 note buttons, toggle modes, range, octave | 15 tests |
| `KeySignatureFragmentTest.kt` | Key list, major/minor toggle, selection | 10 tests |
| `SongListFragmentTest.kt` | CRUD operations, sorting, reordering | 14 tests |
| `SettingsActivityTest.kt` | Wake lock, note mode, range, sound, theme | 12 tests |

**Total Android PitchPerfect Tests**: ~63 tests

### TagMaster Android Tests
Location: `Android/TagMaster/tests/depollsoft/tagmaster/`

| File | Coverage | Test Count |
|------|----------|------------|
| `MeActivityTest.kt` | Main screen, search, favorites, navigation | 12 tests |
| `TagSearchActivityTest.kt` | Search input, filters, results | 14 tests |
| `TagDetailActivityTest.kt` | Tab navigation, favorites toggle, share | 15 tests |
| `FavoritesFlowTest.kt` | End-to-end favorites journey | 8 tests |

**Total Android TagMaster Tests**: ~49 tests

### Running Android Tests
```bash
# PitchPerfect UI Tests
cd Android && ./gradlew :PitchPerfect:connectedDebugAndroidTest

# TagMaster UI Tests
cd Android && ./gradlew :TagMaster:connectedDebugAndroidTest

# All UI Tests
cd Android && ./gradlew connectedDebugAndroidTest
```

## iOS UI Tests

### Test Framework
- **Framework**: XCUITest (XCTest)
- **Language**: Swift 5.0
- **Minimum Deployment**: iOS 15.0

### PitchPerfect iOS Tests
Location: `iOS/pitchperfect/pitchperfectUITests/`

| File | Coverage | Test Count |
|------|----------|------------|
| `pitchperfectUITests.swift` | Tab navigation, note buttons, range toggle | 15 tests |
| `SongManagementUITests.swift` | Add/edit/delete songs, sorting, reordering | 14 tests |
| `SettingsUITests.swift` | Sound mode, volume, display, theme, about | 14 tests |
| `KeySignatureUITests.swift` | Key list, major/minor toggle, selection | 12 tests |

**Total iOS PitchPerfect Tests**: ~55 tests

### TagMaster iOS Tests
Location: `iOS/tagmaster/tagmasterUITests/`

| File | Coverage | Test Count |
|------|----------|------------|
| `tagmasterUITests.swift` | Main screen, search, favorites, navigation | 12 tests |
| `SearchUITests.swift` | Search field, filters, results, keyboard | 14 tests |
| `FavoritesUITests.swift` | Favorites list, remove, refresh | 12 tests |
| `TagDetailUITests.swift` | Tab navigation, favorites, share, audio | 16 tests |

**Total iOS TagMaster Tests**: ~54 tests

### Running iOS Tests
```bash
# Run all iOS tests with coverage
cd iOS && ./scripts/test_coverage.sh

# Run specific scheme
xcodebuild test \
  -workspace iOS.xcworkspace \
  -scheme pitchperfectUITests \
  -destination 'platform=iOS Simulator,name=iPhone 16'
```

## Project Configuration

### iOS Xcode Projects Updated
- `iOS/pitchperfect/pitchperfect.xcodeproj` - Added `pitchperfectUITests` target
- `iOS/tagmaster/tagmaster.xcodeproj` - Added `tagmasterUITests` target

### Files Created
```
iOS/pitchperfect/pitchperfectUITests/
├── Info.plist
├── pitchperfectUITests.swift
├── SongManagementUITests.swift
├── SettingsUITests.swift
└── KeySignatureUITests.swift

iOS/tagmaster/tagmasterUITests/
├── Info.plist
├── tagmasterUITests.swift
├── SearchUITests.swift
├── FavoritesUITests.swift
└── TagDetailUITests.swift

iOS/scripts/
└── add_uitest_target.rb (utility script)
```

## Remaining Setup Steps

### 1. Add UI Test Targets to Xcode Schemes

In Xcode:
1. Open the project's scheme editor (Product → Scheme → Edit Scheme)
2. Select the "Test" action
3. Click "+" to add the UI Test target
4. Select `pitchperfectUITests` or `tagmasterUITests`

### 2. Enable UI Test Target in Workspace

Ensure the UI test targets are visible in the workspace:
1. Open `iOS.xcworkspace`
2. Navigate to each project's test targets
3. Verify the UI test target appears in the scheme list

### 3. Configure Accessibility Identifiers

For more reliable UI tests, add accessibility identifiers to key UI elements in the app:

**Swift Example**:
```swift
button.accessibilityIdentifier = "playNoteButton"
tableView.accessibilityIdentifier = "songList"
```

**Storyboard**: Set the "Accessibility → Identifier" field in Interface Builder.

### 4. Test Data Setup

Consider adding test data injection for UI tests:
```swift
app.launchArguments = ["--uitesting"]
// In AppDelegate, check for this argument to load test data
```

## Coverage Goals

| Platform | App | Current Unit Coverage | Target with UI Tests |
|----------|-----|----------------------|---------------------|
| Android | PitchPerfect | ~40% | 80% |
| Android | TagMaster | ~35% | 80% |
| iOS | PitchPerfect | ~35% | 80% |
| iOS | TagMaster | ~30% | 80% |

## Test Categories

### Functional Tests
- Navigation flows
- Button interactions
- Form inputs and validation
- List operations (scroll, select, delete)

### Integration Tests
- Data persistence
- Settings changes
- State management across screens

### Accessibility Tests
- VoiceOver support
- Accessibility labels
- Touch target sizes

## Maintenance Guidelines

1. **Update tests when UI changes**: If you modify a view controller or activity, update corresponding UI tests
2. **Keep tests independent**: Each test should be able to run in isolation
3. **Use page object pattern**: Consider extracting common interactions into helper classes
4. **Add accessibility identifiers early**: Makes UI testing more reliable
5. **Run tests in CI**: Configure CI to run UI tests on every PR

## Troubleshooting

### iOS Tests Not Running
- Ensure the UI test target is added to the scheme
- Check that the host app target is correctly configured
- Verify the simulator is available

### Android Tests Failing
- Ensure an emulator or device is connected
- Check that Espresso dependencies are correctly configured
- Disable animations on the test device

### Flaky Tests
- Add appropriate `waitForExistence(timeout:)` calls (iOS)
- Use `IdlingResource` for async operations (Android)
- Ensure stable test data
