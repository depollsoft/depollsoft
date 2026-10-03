# Pitch Perfect release evidence

- iOS: Mac sign-in picker fix and GoogleUtilities stability update since 3.2.1.
- Android phone and Wear OS: shared audio-track failure handling, note retry/highlight fixes, and the phone frame-reporting crash fix since 5.2.1.
- Release validation: prefetch all Robolectric runtimes before Android test workers start; split iOS privacy persistence and revocation checks into focused cases with the existing duration limits.

## ios: cbd249e71e343381854ecc74935c944e544bfab6..HEAD

6f16a77f iOS: keep privacy persistence and revocation UI cases focused
a21693df Android: prefetch the runtime used by shared Java tests
8e9d6a7b Android: fetch Robolectric runtimes before starting tests
de444493 release: prepare both apps for crash-fix updates
f73ac5da iOS: pin FirebaseUI to the fix for the picker crashing on a Mac (#91)
2e8e5cea Fix Tag Master's crash at login on Mac, and this month's Crashlytics crashes (#90)

## android: cbd249e71e343381854ecc74935c944e544bfab6..HEAD

6f16a77f iOS: keep privacy persistence and revocation UI cases focused
a21693df Android: prefetch the runtime used by shared Java tests
8e9d6a7b Android: fetch Robolectric runtimes before starting tests
de444493 release: prepare both apps for crash-fix updates
f73ac5da iOS: pin FirebaseUI to the fix for the picker crashing on a Mac (#91)
2e8e5cea Fix Tag Master's crash at login on Mac, and this month's Crashlytics crashes (#90)
