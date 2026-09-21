# Repository Guidelines

## Project Structure & Module Organization

- `api/`: TypeScript Express service (GCP Pub/Sub + BigQuery).
- `Android/`: Gradle multi-module projects (Kotlin/Java).
- `iOS/`: Xcode workspace with Swift Package Manager (`iOS.xcworkspace`).
- `Firebase/`, `CloudCode/`, `AppEngine/`: Hosting/config and cloud code.

## Build, Test, and Development Commands

- API: `cd api && npm ci && npm run build && npm start` (uses `PORT`, `PUBSUB_VERIFICATION_TOKEN`). Docker: `npm run docker-build`, `npm run docker-run`.
- Android: `cd Android && ./gradlew assembleDebug` (build), `./gradlew test` (unit tests), `./gradlew connectedDebugAndroidTest` (instrumentation).
- iOS: `cd iOS && xcodebuild -resolvePackageDependencies -workspace iOS.xcworkspace -scheme <Scheme>`, then open `iOS/iOS.xcworkspace`. CLI build: `xcodebuild -workspace iOS/iOS.xcworkspace -scheme <Scheme> -configuration Debug build`.

## Coding Style & Naming Conventions

- TypeScript: 4-space indent; `camelCase` for vars/functions, `PascalCase` for classes; prefer explicit types. Output goes to `api/bin/`.
- Android: Follow Kotlin/Java conventions; 4-space indent; Android resources use `lowercase_underscore` (e.g., `activity_main.xml`).
- iOS: Swift/ObjC conventions; filenames match primary type; use `PascalCase` types.

## Testing Guidelines

- API: Tests are minimal today. Place unit tests near sources or under `api/src/test` (e.g., `analytics.spec.ts`). Use `npm run buildtest` for type-checking until a test runner is added.
- Android: Unit tests in `src/test`, instrumentation in `src/androidTest`. Run via Gradle as above.
- iOS: Add tests to a test target and run in Xcode or `xcodebuild -scheme <Scheme> test`.

## Commit & Pull Request Guidelines

- Commits: Short, imperative summaries (≤72 chars). Scope the module prefix when helpful (e.g., `api: add /analytics pubsub handler`). Reference issues (`#123`) when applicable.
- PRs: Clear description, affected modules, testing steps, and screenshots for UI changes. Ensure Android, iOS, and API builds pass. Avoid committing secrets or local config.

## PR Preview Deploys

- `.github/workflows/pr-preview.yml` builds private PR preview artifacts for same-repo mobile PRs and posts a sticky PR comment with APK download details plus `/deploy` instructions. Only the apps and platforms the PR changes are built and distributed; `scripts/ci/changed_apps.py` treats a file under one app's module as that app's and everything else under `Android/` or `iOS/` as shared, which builds both apps. Android CI and iOS CI use the same selection for their test jobs.
- Comment `/deploy` on a same-repo PR to queue `.github/workflows/deploy-pr-preview.yml`, which uploads the private Pitch Perfect and Tag Master builds to TestFlight and Play Internal Testing.
- Required GitHub Actions secrets for preview deploys: `ANDROID_UPLOAD_KEYSTORE`, `ANDROID_UPLOAD_KEYSTORE_PASSWORD`, `ANDROID_UPLOAD_KEY_ALIAS`, `ANDROID_UPLOAD_KEY_PASSWORD`, `PLAY_SERVICE_ACCOUNT_JSON`, `APP_STORE_CONNECT_API_KEY_ID`, `APP_STORE_CONNECT_API_ISSUER_ID`, `APP_STORE_CONNECT_API_KEY_CONTENT`, `MATCH_PASSWORD`, and `MATCH_GIT_SSH_KEY`.
- `PLAY_SERVICE_ACCOUNT_JSON` may be stored as either raw JSON or base64-encoded JSON; the deploy workflow now accepts both formats.
- iOS preview deploys clone the private signing repo at `git@github.com:depollsoft/certificates.git` using `MATCH_GIT_SSH_KEY`; `MATCH_GIT_BASIC_AUTHORIZATION` is still supported as a fallback.
- Private Firebase config files for the mobile preview variants are committed in the repository; keep signing keys and store credentials in GitHub Actions secrets only.

## Security & Configuration Tips

- Keep secrets out of VCS. For API, provide `PUBSUB_VERIFICATION_TOKEN` (and GCP credentials) via environment or secret manager. Do not log PII; prefer IDs over raw data.

## Production releases

- Use `.agents/skills/prepare-release/SKILL.md` and `docs/releases.md` to prepare a release PR.
- `releases/<app>/release.json` is the merge-to-deploy trigger. App and platform versions are independent; never bump the other app implicitly.
- Listing copy lives in `store/<app>/listing.json`. Generate real native screenshots with `scripts/release/capture.py`; generated media stays out of git.
- `.github/workflows/release.yml` captures assets on release PRs and submits selected production apps after merge. The private `/deploy` preview path is separate.
