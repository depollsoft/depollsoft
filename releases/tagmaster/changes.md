# Tag Master release evidence

- iOS: Mac sign-in picker fix, synchronized saved-tag cache, and GoogleUtilities stability update since 3.1.1.
- Android: shared PitchPerfectLib and DepollSoftCommon audio fixes used by the tag starting-pitch button since 6.1.1. Pitch Perfect sound, tuning, and Classic Pitch Pipe UI changes do not add Tag Master features.
- Release validation: prefetch all Robolectric runtimes before Android test workers start; split iOS privacy persistence and revocation checks into focused cases with the existing duration limits and ad prompts suppressed using the existing debug switch.
- Tag Master iOS validation: check light and dark appearances in separate native cases, retaining all page, backdrop, keyboard, and rotation assertions under the existing 90-second limit.

## ios: c36d60085a9a02c669a5a480c9308153796114f6..HEAD

0ed871f1 Tag Master: test each native appearance within its case budget
74717e60 iOS: isolate telemetry UI tests from ad consent prompts
6f16a77f iOS: keep privacy persistence and revocation UI cases focused
a21693df Android: prefetch the runtime used by shared Java tests
8e9d6a7b Android: fetch Robolectric runtimes before starting tests
de444493 release: prepare both apps for crash-fix updates
f73ac5da iOS: pin FirebaseUI to the fix for the picker crashing on a Mac (#91)
2e8e5cea Fix Tag Master's crash at login on Mac, and this month's Crashlytics crashes (#90)

## android: c36d60085a9a02c669a5a480c9308153796114f6..HEAD

0ed871f1 Tag Master: test each native appearance within its case budget
74717e60 iOS: isolate telemetry UI tests from ad consent prompts
6f16a77f iOS: keep privacy persistence and revocation UI cases focused
a21693df Android: prefetch the runtime used by shared Java tests
8e9d6a7b Android: fetch Robolectric runtimes before starting tests
de444493 release: prepare both apps for crash-fix updates
f73ac5da iOS: pin FirebaseUI to the fix for the picker crashing on a Mac (#91)
2e8e5cea Fix Tag Master's crash at login on Mac, and this month's Crashlytics crashes (#90)
