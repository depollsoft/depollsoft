# Repository Guidelines

Guidance for coding agents (Claude Code, Codex and others) working in this repository.

## Codebase Overview

A polyglot monorepo with two mobile apps that share Firebase services, plus a small backend.

- **Pitch Perfect**: a pitch pipe and pitch-training app (Android, Wear OS and iOS).
- **Tag Master**: a barbershop tag browsing and teaching app (Android and iOS).

## Project Structure & Module Organization

- `Android/`: Gradle multi-module project (Kotlin/Java). Apps `PitchPerfect`, `PitchPerfectWear` and `TagMaster`; libraries `DepollSoftCommon`, `DepollSoftCompose`, `PitchPerfectLib` and `depollsoft.lib.kotlin`.
- `iOS/`: Xcode workspace `iOS.xcworkspace` with the `pitchperfect` and `tagmaster` projects plus `depolllib`, using Swift Package Manager.
- `api/`: TypeScript Express service on Google Cloud Run (GCP Pub/Sub and BigQuery).
- `Firebase/`: Firebase configuration for both apps, and Pitch Perfect's account-deletion function.
- `AppEngine/`: Google App Engine services (appears unused).
- `docs/`: design and process notes (`android-compose.md`, `ios-swiftui.md`, `tag-lists.md`, `pitchperfect-set-lists.md`, `releases.md` and others).

## Build, Test, and Development Commands

### Android (from `Android/`)

```bash
./gradlew assembleDebug                      # Build everything
./gradlew :PitchPerfect:assembleDebug        # One app (also :TagMaster, :PitchPerfectWear)
./gradlew test                               # Unit tests for every module
./gradlew :TagMaster:recordRoborazziDebug    # Re-record a module's screenshot goldens
./gradlew connectedDebugAndroidTest          # Device-only instrumentation tests (needs a device or emulator)
```

### iOS (from `iOS/`)

```bash
xcodebuild -resolvePackageDependencies -workspace iOS.xcworkspace -scheme pitchperfect
xcodebuild -workspace iOS.xcworkspace -scheme pitchperfect -configuration Debug build
xcodebuild -workspace iOS.xcworkspace -scheme tagmaster -configuration Debug build
xcodebuild test -workspace iOS.xcworkspace -scheme <scheme> -destination 'platform=iOS Simulator,name=<device>'
```

### API (from `api/`)

```bash
npm ci
npm run build         # Compile TypeScript to bin/
npm test              # node:test suite in src/test
npm run buildtest     # Type-check everything, tests included
npm run docker-build  # Build the Docker image
npm start             # Run in Docker (PORT=8080, reads .env)
npm run clean         # Remove build output
```

### Firebase Functions (from `Firebase/pitchperfect/functions`)

```bash
npm run lint    # Biome
npm run build   # Compile TypeScript
npm run serve   # Serve locally
npm run deploy  # Deploy to Firebase
```

## Architecture

### Android

- **UI**: Jetpack Compose in Kotlin for every screen of Pitch Perfect, Pitch Perfect for Wear OS and Tag Master (see `docs/android-compose.md`). Custom-drawn surfaces paint through Canvas renderers; the AdMob banner and video use `AndroidView`; the home-screen widget stays on `RemoteViews`.
- **State**: models keep observable state in Compose snapshot state via `depollsoft.lib.state` (`StateField`, `StateList`, `ChangeSignal`) in DepollSoftCommon, whose test fixtures add `watchState` for tests. Screen composables take a model and callbacks.
- **Shared libraries**:
  - DepollSoftCommon: utilities both apps use, including the snapshot-state helpers.
  - DepollSoftCompose: Compose code both apps share (list motion and drag reordering, scrollbars, snackbar timing, tooltips, the Menu key, the View pixel rules). Compose UI and foundation only; each app brings its Material version.
  - depollsoft.lib.kotlin: Kotlin extensions.
  - PitchPerfectLib: Pitch Perfect's notes, keys, songs and sound engine.
- **Build system**: Gradle with dynamic version codes (YYMMDD * 1000 + build * 10 + suffix).

### iOS

- Both apps' UI is SwiftUI: a SwiftUI `App` and an `@Observable` model per screen; Tag Master routes through `TMRouter`. See `docs/ios-swiftui.md`.
- Model layers stay mixed Objective-C and Swift (DPTag, DPNote, DPKey, stores) behind Swift bridging headers.
- Swift Package Manager supplies Firebase, FirebaseUI, Google Sign-In, the Facebook SDK and Google Mobile Ads.
- depolllib holds the shared non-UI pieces: JSON serialization, the file cache, NSString helpers, analytics and `DPAppLog`.

### Backend

- **API server**: Express on Cloud Run. Writes analytics to BigQuery and messages to Pub/Sub, and uses geoip-lite for geolocation. Multi-stage Docker build; `.github/workflows/api.yml` deploys it on pushes to `main`.
- **Firebase**: Firestore for data, Auth shared across platforms, and Functions for server-side logic.

## Development Setup & Configuration

- Android minSdk is 24 for the phone apps, 26 for Wear and 23 for the libraries. On iOS the apps and depolllib target 17.0, but pitchperfectlib still targets 15.0, so code there must stay available on iOS 15.
- Node.js 22 for Firebase Functions; the API container uses Node.js 24. Docker is needed for API development.
- Firebase client configs (`google-services.json`, `GoogleService-Info.plist`, including the private preview variants) are tracked. Service credentials belong in environment variables or a secret manager, never in git.
- API: provide `PORT`, `PUBSUB_VERIFICATION_TOKEN` and GCP credentials through `.env` or a secret manager. It needs the GeoIP data first: `gsutil cp gs://depollsoft-build-data/geoip-lite.tar.gz . && tar -xzf geoip-lite.tar.gz`.
- `Android/debug.jks` (password `depollsoft`) signs debug builds. API deploys use the `GCP_PROJECT_ID` and `GCP_SA_KEY` Actions secrets.

## Coding Style & Naming Conventions

- TypeScript: 4-space indent; `camelCase` for variables and functions, `PascalCase` for classes; prefer explicit types and strict compiler settings. Output goes to `api/bin/`.
- Android: Kotlin/Java conventions, 4-space indent; resources use `lowercase_underscore` (e.g. `activity_main.xml`). Build screens as Compose functions of a model plus callbacks, with state in snapshot state (`depollsoft.lib.state`).
- iOS: Swift/Objective-C conventions; filenames match their primary type; `PascalCase` types. Keep shared package versions aligned across both Xcode projects.

## Testing Guidelines

- **API**: `node:test` suite in `api/src/test` covering the analytics router (`npm test`). The router takes injected Pub/Sub, BigQuery, JWT and geoip dependencies through `createAnalyticsRouter`. Cloud Run deploy is still the first integration check.
- **Android**: Pitch Perfect, Wear and Tag Master screen behaviour is tested on the JVM with Robolectric and the Compose test APIs (`src/test`). Roborazzi screenshot goldens in `src/test/screenshots` pin the pixels (`recordRoborazziDebug` to update, `verifyRoborazziDebug` to check; CI verifies them), including right-to-left, 200% font, landscape and tablet variants. Semantics snapshots in `src/test/semantics` pin what a screen reader hears (`RECORD_SEMANTICS=1` to rewrite). `src/androidTest` holds only a device-only residue (drags, IME geometry, PdfRenderer, store screenshots, the FirebaseUI patch check) that CI compiles but does not run. Re-record and review goldens when a UI change is intended; an unexplained golden diff is a regression.
- **iOS**:
  - pitchperfect: models are tested directly, and the real controls are driven in-process through the accessibility tree (`iOS/shared/SwiftUITestDriver.swift`). `PitchPerfectScreenCatalogTests` renders every screen state (set `TEST_RUNNER_SCREEN_CATALOG_DIR` to write captures).
  - tagmaster: the hosted `tagmasterTests` bundle tests models directly and drives the real SwiftUI screens and shell through `UIDriver` (`iOS/shared/SwiftUITestDriver.swift`).
  - Both apps' `*UITests` bundles hold only launch metrics, keyboard/rotation/system-sheet cases and `StoreScreenshotTests`. Test CI runs them only on its weekly extended run; release asset capture (`release-assets.yml` through `scripts/release/capture.py`) also runs `StoreScreenshotTests`.
  - pitchperfectlib's tests run in Pitch Perfect's CI job. depolllib has its own tests (`xcodebuild test -scheme depolllib`), which CI doesn't run.
- **Sync against the Firestore emulators**: `TagListSyncEmulatorTest` (Android, run the class on its own) and `TMListSyncEmulatorTests` (iOS) cover Tag Master's lists against `scripts/firestore-emulator.sh tagmaster` (see `docs/tag-lists.md`). `SongListSyncEmulatorTest` (Android) and `DPSongListSyncEmulatorTests` (iOS; needs a signed build, and an ad-hoc `CODE_SIGN_IDENTITY=-` simulator build is enough) cover Pitch Perfect's set lists against `scripts/firestore-emulator.sh pitchperfect` (see `docs/pitchperfect-set-lists.md`). All of them skip when no emulator is running. The suites and the script honour `FIRESTORE_EMULATOR_HOST` / `FIREBASE_AUTH_EMULATOR_HOST` (iOS test runs take them as `TEST_RUNNER_FIRESTORE_EMULATOR_PORT` / `TEST_RUNNER_AUTH_EMULATOR_PORT`), so the emulators can run on free ports such as `localhost:8180` / `localhost:9199` when 8080 is taken.

## Commit & Pull Request Guidelines

- Commits: short, imperative summaries (≤72 chars), with a module or app prefix when it helps (e.g. `api: add /analytics pubsub handler`). Reference issues (`#123`) when applicable.
- PRs: clear description, affected modules, testing steps, and screenshots for UI changes. Android, iOS and API builds must pass. Never commit secrets or local config.

## PR Preview Deploys

- `.github/workflows/pr-preview.yml` builds private PR preview artifacts for same-repo mobile PRs and posts a sticky PR comment with APK download details plus `/deploy` instructions. Only the apps and platforms the PR changes are built and distributed: `scripts/ci/changed_apps.py` treats a file under one app's module as that app's, and everything else under `Android/` or `iOS/` as shared, which builds both apps. Android CI and iOS CI use the same selection for their test jobs.
- Comment `/deploy` on a same-repo PR to queue `.github/workflows/deploy-pr-preview.yml`, which uploads the private Pitch Perfect and Tag Master builds to TestFlight and Play Internal Testing.
- Required Actions secrets for preview deploys: `ANDROID_UPLOAD_KEYSTORE`, `ANDROID_UPLOAD_KEYSTORE_PASSWORD`, `ANDROID_UPLOAD_KEY_ALIAS`, `ANDROID_UPLOAD_KEY_PASSWORD`, `PLAY_SERVICE_ACCOUNT_JSON`, `APP_STORE_CONNECT_API_KEY_ID`, `APP_STORE_CONNECT_API_ISSUER_ID`, `APP_STORE_CONNECT_API_KEY_CONTENT`, `MATCH_PASSWORD` and `MATCH_GIT_SSH_KEY`.
- `PLAY_SERVICE_ACCOUNT_JSON` may be raw or base64-encoded JSON; the deploy workflow accepts both.
- iOS preview deploys clone the private signing repo `git@github.com:depollsoft/certificates.git` with `MATCH_GIT_SSH_KEY`; `MATCH_GIT_BASIC_AUTHORIZATION` still works as a fallback.

## Production Releases

- Use `.agents/skills/prepare-release/SKILL.md` and `docs/releases.md` to prepare a release PR.
- `releases/<app>/release.json` is the merge-to-deploy trigger. App and platform versions are independent; never bump the other app implicitly.
- Listing copy lives in `store/<app>/listing.json`. Generate real native screenshots with `scripts/release/capture.py`; generated media stays out of git.
- `.github/workflows/release.yml` captures assets on release PRs and submits the selected production apps after merge. The private `/deploy` preview path is separate.

## Gotchas

- Run `xcodebuild -resolvePackageDependencies` after changing Swift packages.
- **FirebaseUI-iOS pin**: both Xcode projects pin FirebaseUI to revision `4d218a69` (16.1.0 plus the fix for firebase/FirebaseUI-iOS#1397). Without it, the sign-in picker crashes as soon as it opens when the iPad apps run on a Mac. Move back to a version requirement once a FirebaseUI release after 16.1.0 includes that fix.
- Don't log PII; prefer IDs over raw data.
- If an API deployment fails, start with the `api.yml` workflow logs.
