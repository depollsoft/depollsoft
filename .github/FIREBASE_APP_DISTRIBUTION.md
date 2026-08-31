# Firebase App Distribution for pull requests

The workflow in `.github/workflows/firebase-app-distribution.yml` builds and
uploads the private PitchPerfect and TagMaster beta apps when a commit is pushed
to a pull request targeting `main`. It only runs for branches in this
repository,
so credentials are not exposed to forks.

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

Android private APKs use these existing repository secrets:

- `ANDROID_UPLOAD_KEYSTORE`
- `ANDROID_UPLOAD_KEYSTORE_PASSWORD`
- `ANDROID_UPLOAD_KEY_ALIAS`
- `ANDROID_UPLOAD_KEY_PASSWORD`

The iOS workflow restores the existing encrypted Match archive and creates Ad
Hoc profiles for the private bundle identifiers through App Store Connect. It
uses these existing secrets:

- `APP_STORE_CONNECT_API_KEY_CONTENT`
- `APP_STORE_CONNECT_API_KEY_ID`
- `APP_STORE_CONNECT_API_ISSUER_ID`
- `MATCH_PASSWORD`
- `MATCH_REPOSITORY_ARCHIVE_PART_1`
- `MATCH_REPOSITORY_ARCHIVE_PART_2`

The Ad Hoc profiles must contain the device UDIDs for everyone installing an
iOS build. Add new tester devices to the Apple Developer account before the next
workflow run.
