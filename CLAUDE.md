# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Codebase Overview

This is a polyglot monorepo containing several mobile applications and backend services. The main applications share architecture and Firebase services.

### Main Applications

- **PitchPerfect**: A music education app for pitch training (Android & iOS)
- **TagMaster**: A barbershop music tag browsing/teaching app (Android & iOS)

### Project Structure

- `/Android/` - Android applications and libraries (Java/Kotlin)
- `/iOS/` - iOS applications (Mixed Objective-C and Swift)
- `/api/` - Node.js/TypeScript backend service deployed to Google Cloud Run
- `/Firebase/` - Firebase configuration and PitchPerfect account-deletion function
- `/AppEngine/` - Google App Engine services (appears unused)

## Build Commands

### Android Projects

```bash
# From /Android directory
./gradlew clean build              # Build all Android projects
./gradlew :PitchPerfect:assembleDebug    # Build PitchPerfect debug APK
./gradlew :TagMaster:assembleDebug       # Build TagMaster debug APK
./gradlew :PitchPerfectWear:assembleDebug  # Build the Wear OS pitch pipe
./gradlew :TagMaster:recordRoborazziDebug  # Re-record a module's screenshot goldens
```

### iOS Projects

```bash
# From /iOS directory
xcodebuild -resolvePackageDependencies -workspace iOS.xcworkspace -scheme pitchperfect
xcodebuild -workspace iOS.xcworkspace -scheme pitchperfect -configuration Debug
xcodebuild -workspace iOS.xcworkspace -scheme tagmaster -configuration Debug
```

### API Server

```bash
# From /api directory
npm run build                       # Compile TypeScript
npm run docker-build               # Build Docker image
npm run start                      # Run with Docker
npm run clean                      # Clean build artifacts
```

### Firebase Functions

```bash
# From /Firebase/pitchperfect/functions
npm run lint                       # Run Biome
npm run build                      # Compile TypeScript
npm run serve                      # Serve locally
npm run deploy                     # Deploy to Firebase
```

## Architecture

### Android Architecture

- **UI**: Jetpack Compose in Kotlin for every screen of Pitch Perfect, Pitch Perfect for Wear OS and Tag Master (see `docs/android-compose.md`). Custom-drawn surfaces paint through Canvas renderers; the AdMob banner and video use `AndroidView`; the home-screen widget stays on `RemoteViews`
- **State**: models keep observable state in Compose snapshot state via `depollsoft.lib.state` (`StateField`, `StateList`, `ChangeSignal`, `watchState`) in DepollSoftCommon; screen composables take a model and callbacks
- **Shared Libraries**:
  - DepollSoftCommon: Shared utilities across Android apps, including the snapshot-state helpers
  - DepollSoftCompose: Compose code both apps share (list motion and drag reordering, scrollbars, snackbar timing, tooltips, the Menu key, the View pixel rules); Compose UI and foundation only, each app brings its Material version
  - depollsoft.lib.kotlin: Kotlin extensions (also has tests)
  - PitchPerfectLib: Shared components for PitchPerfect
- **Build System**: Gradle with dynamic version codes (YYMMDD *1000 + build* 10 + suffix)

### iOS Architecture

- Mixed Objective-C and Swift codebase with Swift bridging headers
- Swift Package Manager for Firebase, FirebaseUI, and Google Mobile Ads dependencies
- Custom UI components in depolllib (has test coverage)
- Both apps share common Firebase and authentication dependencies

### Backend Architecture

- **API Server**: Node.js/TypeScript service on Google Cloud Run
  - Express.js for HTTP routing
  - Connects to BigQuery for analytics and Pub/Sub for messaging
  - Uses geoip-lite for geolocation services
  - Containerized with multi-stage Docker build
  - CI/CD pipeline via GitHub Actions (`.github/workflows/api.yml`)
- **Firebase Services**:
  - Firestore for data storage
  - Firebase Functions for serverless backend logic
  - Authentication shared across platforms

## Key Dependencies

### Android

- Firebase (Auth, Firestore, Functions)
- Google Play Services (Ads, Wearable)
- Facebook SDK
- AndroidX libraries
- Kotlin coroutines

### iOS  

- Firebase (Auth, Firestore, Functions)
- FirebaseUI for authentication flows
- Google Mobile Ads
- Google Sign-In

### Backend

- Express.js for API server
- Firebase Admin SDK
- Google Cloud SDK (BigQuery, Pub/Sub)

## Development Setup & Configuration

### Prerequisites

- Android minimum SDK: 23 (Android 6.0)
- iOS deployment target: 17.0
- Node.js version: 22 for Firebase Functions; the API container uses Node.js 24
- Docker (for API development)

### Configuration files

- **Android**: `google-services.json` - Firebase configuration
- **iOS**: `GoogleService-Info.plist` - Firebase configuration
- **API**: `.env` file with PORT and Google Cloud credentials
- **API**: GeoIP data downloaded from `gs://depollsoft-build-data/geoip-lite.tar.gz`

### Security & Secrets

- Android debug keystore: `/Android/debug.jks` (password: "depollsoft")
- GitHub Actions uses secrets: `GCP_PROJECT_ID`, `GCP_SA_KEY` for API deployment

## Testing

### Test Coverage by Platform

- **API**: node:test suite in `api/src/test` covering the analytics router (`npm test`); the router takes injected Pub/Sub, BigQuery, JWT and geoip dependencies via `createAnalyticsRouter`
- **Tag Master list sync**: `TagListSyncEmulatorTest` (Android, run the class on its own) and `TMListSyncEmulatorTests` (iOS) exercise the real Firestore sync against the local emulators started by `scripts/firestore-emulator.sh tagmaster`, and skip when none is running; see `docs/tag-lists.md`
- **Android**:
  - depollsoft.lib.kotlin: Has test coverage
  - TagMaster / PitchPerfect / PitchPerfectWear: screen behaviour is tested on the JVM with Robolectric and the Compose test APIs (`src/test`); pixels are pinned by Roborazzi screenshot goldens in `src/test/screenshots` (`recordRoborazziDebug` to update, `verifyRoborazziDebug` to check); `src/androidTest` holds only a small device-only residue (drags, IME geometry, PdfRenderer, store screenshots, FirebaseUI patch check) that CI does not run
- **Pitch Perfect set list sync**: `SongListSyncEmulatorTest` (Android) and `DPSongListSyncEmulatorTests` (iOS, needs a signed build) exercise the real Firestore sync against the local emulators started by `scripts/firestore-emulator.sh pitchperfect`, and skip when none is running; see `docs/pitchperfect-set-lists.md`
- **iOS**:
  - depolllib: Has tests
  - pitchperfectlib: Has tests
  - pitchperfect / tagmaster: behaviour is tested in-process in the hosted `*Tests` bundles (real view controllers in a test `UIWindow`); the `*UITests` bundles hold only launch metrics, keyboard/rotation/system-sheet cases and `StoreScreenshotTests` (used by `scripts/release/capture.py`), and run only on the weekly extended iOS CI run

### Running Tests

```bash
# Android
./gradlew test

# iOS (from project directory)
xcodebuild test -workspace ../iOS.xcworkspace -scheme <scheme-name>

# API (from /api)
npm test
```

## Common Development Workflows

### API Development

1. Set up `.env` file with required environment variables
2. Download GeoIP data: `gsutil cp gs://depollsoft-build-data/geoip-lite.tar.gz . && tar -xzf geoip-lite.tar.gz`
3. Run locally: `npm run start` (uses Docker)
4. Deploy: Push to `main` branch triggers automatic deployment via GitHub Actions

### Mobile Development

1. **Android**: Import project in Android Studio, sync Gradle
2. **iOS**: Open `iOS.xcworkspace`; Xcode resolves Swift packages automatically
3. Configure Firebase files (google-services.json / GoogleService-Info.plist)
4. Build and run on device/simulator

## Developer Gotchas & Important Notes

### Critical Issues

1. **API tests are unit-level only** - the router is covered, but Cloud Run deploy is still the first integration check
2. **GeoIP data dependency** - API won't work without downloading this data first
3. **Manual mobile deployments** - No CI/CD for Android/iOS apps
4. **Sensitive files in repo** - `.jks` files should be removed

### Common Pitfalls

1. **Configuration files**: Firebase client configs are tracked; service credentials still belong in environment variables or secret managers
2. **SPM resolution**: Run `xcodebuild -resolvePackageDependencies` after package changes
3. **Firebase setup**: Both platforms need proper Firebase configuration
4. **Node versions**: Firebase Functions use Node 22 and the API container uses Node 24

### Best Practices

1. Build Android screens as Compose functions of a model plus callbacks, with state in snapshot state (`depollsoft.lib.state`)
2. Keep shared iOS package versions aligned across both Xcode projects
3. Re-record and review screenshot goldens when a UI change is intended; an unexplained golden diff is a regression
4. Check CI/CD logs if API deployment fails
5. Use strict TypeScript settings for API development
