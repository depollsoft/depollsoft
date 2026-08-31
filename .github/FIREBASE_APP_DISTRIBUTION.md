# Firebase App Distribution for pull requests

PR updates run the secretless `PR Preview` workflow. It builds private Android
APKs and unsigned iOS archives with the shared epoch-based preview build number.
After that workflow succeeds, `.github/workflows/firebase-app-distribution.yml`
runs from the trusted default branch, downloads those artifacts, signs the iOS
archives, and uploads all four apps to Firebase App Distribution.

Contributor-controlled Gradle, CocoaPods, and Fastlane code never runs in a job
that has signing or Firebase credentials. Distribution only consumes the APKs
and unsigned archives produced by the PR workflow.

## Firebase configuration

The following Actions secrets contain service-account JSON keys with the
**Firebase App Distribution Admin** role:

- `FIREBASE_PITCHPERFECT_SERVICE_ACCOUNT_JSON`
- `FIREBASE_TAGMASTER_SERVICE_ACCOUNT_JSON`

The service accounts are scoped to `pitch-perfect-94415` and `tag-master`,
respectively.

Both Firebase projects contain a `pr-testers` group. The repository variable
`FIREBASE_APP_DISTRIBUTION_GROUPS` selects that group for each upload.

## Existing signing configuration

Android preview APKs are built without repository secrets and uploaded directly
to the private Firebase apps.

iOS archives are signed after the PR build completes. The trusted signing job
restores the encrypted Match assets into a temporary readonly bare repository.
It uses these existing secrets:

- `APP_STORE_CONNECT_API_KEY_CONTENT`
- `APP_STORE_CONNECT_API_KEY_ID`
- `APP_STORE_CONNECT_API_ISSUER_ID`
- `MATCH_PASSWORD`
- `MATCH_REPOSITORY_ARCHIVE_PART_1`
- `MATCH_REPOSITORY_ARCHIVE_PART_2`

The Match archive contains Ad Hoc profiles for the private bundle identifiers.
Those profiles must contain the device UDIDs for everyone installing an iOS
build. Add new tester devices to the Apple Developer account before the next
workflow run.
