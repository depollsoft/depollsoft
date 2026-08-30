# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Codebase Overview

This is a polyglot monorepo containing several mobile applications and backend services. The main applications share architecture and Firebase services.

### Main Applications
- **PitchPerfect**: A music education app for pitch training (Android & iOS)
- **TagMaster**: A barbershop music tag browsing/teaching app (Android & iOS)
- **Bindroid**: An open-source Android data binding library that implements MVVM pattern

### Project Structure
- `/Android/` - Android applications and libraries (Java/Kotlin)
- `/iOS/` - iOS applications (Mixed Objective-C and Swift)
- `/api/` - Node.js/TypeScript backend service deployed to Google Cloud Run
- `/Firebase/` - Firebase configuration and PitchPerfect account-deletion function
- `/DotNet/` - Legacy Silverlight/Windows Phone applications (not actively maintained)
- `/AppEngine/` - Google App Engine services (appears unused)

## Build Commands

### Android Projects
```bash
# From /Android directory
./gradlew clean build              # Build all Android projects
./gradlew :PitchPerfect:assembleDebug    # Build PitchPerfect debug APK
./gradlew :TagMaster:assembleDebug       # Build TagMaster debug APK
./gradlew :bindroid:build               # Build Bindroid library
```

### iOS Projects
```bash
# From /iOS directory
pod install                         # Install dependencies
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
# From /Firebase/pitchperfect/functions or /Firebase/tagmaster/functions
npm run lint                       # Run TSLint
npm run build                      # Compile TypeScript
npm run serve                      # Serve locally
npm run deploy                     # Deploy to Firebase
```

## Architecture

### Android Architecture
- **Bindroid Library**: Custom data binding framework implementing MVVM pattern
  - TrackableField classes for reactive properties
  - UiBinder for UI-to-model bindings
  - Support for converters and two-way bindings
  - Well-tested with dedicated test suite in `bindroid-test`
- **Shared Libraries**: 
  - DepollSoftCommon: Shared utilities across Android apps
  - depollsoft.lib.kotlin: Kotlin extensions (also has tests)
  - PitchPerfectLib: Shared components for PitchPerfect
- **Build System**: Gradle with dynamic version codes (YYMMDD * 1000 + build * 10 + suffix)

### iOS Architecture
- Mixed Objective-C and Swift codebase with Swift bridging headers
- CocoaPods for dependency management with shared pod configurations
- Custom UI components in depolllib (has test coverage)
- Both apps share common Firebase and authentication dependencies
- Post-install scripts handle deployment target configurations

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
- **Legacy Systems**:
  - Silverlight/.NET projects (no longer maintained)

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
- iOS deployment target: 15.0
- Node.js version: 22 for Firebase Functions; the API container uses Node.js 24
- Docker (for API development)

### Critical Configuration Files (NOT in source control)
- **Android**: `google-services.json` - Firebase configuration
- **iOS**: `GoogleService-Info.plist` - Firebase configuration
- **API**: `.env` file with PORT and Google Cloud credentials
- **API**: GeoIP data downloaded from `gs://depollsoft-build-data/geoip-lite.tar.gz`

### Security & Secrets
- Android debug keystore: `/Android/debug.jks` (password: "depollsoft")
- GitHub Actions uses secrets: `GCP_PROJECT_ID`, `GCP_SA_KEY` for API deployment
- Firebase client configuration is restored in CI from the `PITCHPERFECT_GOOGLE_*` and `TAGMASTER_GOOGLE_*` repository secrets

## Testing

### Test Coverage by Platform
- **API**: ⚠️ **NO AUTOMATED TESTS** - Major gap, only placeholder test script
- **Android**:
  - Bindroid: Well-tested with dedicated test suite
  - depollsoft.lib.kotlin: Has test coverage
- **iOS**:
  - depolllib: Has tests
  - pitchperfectlib: Has tests
  - tagmaster: Has tests
- **.NET**: Legacy projects have test coverage

### Running Tests
```bash
# Android
./gradlew test

# iOS (from project directory)
xcodebuild test -workspace ../iOS.xcworkspace -scheme <scheme-name>

# API - NO TESTS AVAILABLE
```

## Common Development Workflows

### API Development
1. Set up `.env` file with required environment variables
2. Download GeoIP data: `gsutil cp gs://depollsoft-build-data/geoip-lite.tar.gz . && tar -xzf geoip-lite.tar.gz`
3. Run locally: `npm run start` (uses Docker)
4. Deploy: Push to `main` branch triggers automatic deployment via GitHub Actions

### Mobile Development
1. **Android**: Import project in Android Studio, sync Gradle
2. **iOS**: Run `pod install` first, then open `.xcworkspace` in Xcode
3. Configure Firebase files (google-services.json / GoogleService-Info.plist)
4. Build and run on device/simulator

## Developer Gotchas & Important Notes

### Critical Issues
1. **API has NO automated tests** - Be extremely careful with changes
2. **GeoIP data dependency** - API won't work without downloading this data first
3. **Manual mobile deployments** - No CI/CD for Android/iOS apps
4. **Sensitive files in repo** - `.jks` files should be removed

### Common Pitfalls
1. **Configuration files**: Many required files are not in source control
2. **Pod install**: Always run after pulling iOS changes
3. **Firebase setup**: Both platforms need proper Firebase configuration
4. **Legacy code**: Ignore .NET/Silverlight projects unless specifically needed
5. **API Node version**: Uses older Node 16.3.0, may have compatibility issues

### Best Practices
1. Follow MVVM pattern on Android using Bindroid
2. Use shared pod configurations for iOS apps
3. Test Bindroid changes thoroughly - it's a core dependency
4. Check CI/CD logs if API deployment fails
5. Use strict TypeScript settings for API development