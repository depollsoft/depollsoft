# Privacy choices

Pitch Perfect and Tag Master ask separately for usage analytics and crash reports. Both are off on a new installation and stay off if the screen is dismissed. Saving or declining is remembered on that installation. Settings > Privacy choices reopens the controls.

Analytics consent enables Google Analytics: the screens, feature-use events and user properties listed in [analytics.md](analytics.md), plus what Firebase collects on its own (sessions, app and device information). Declining disables Analytics and resets its local data. Crash reporting uses Firebase's opt-in collection control; declining deletes unsent reports. Disabling crash reporting takes full effect on the next app launch, as stated on the screen. These controls do not delete reports already received by either service.

The review prompt ([analytics.md](analytics.md#review-prompts)) keeps its own counts (days of use, the last ask, the last sound) on the device. They are never sent anywhere and don't depend on either choice.

Telemetry does not enable advertising consent. Firebase ad storage, ad user data, and ad personalization are denied. iOS uses FirebaseAnalyticsCore, which omits Analytics IDFA collection. Facebook automatic app events and advertiser-ID collection are disabled in both apps.

## Pitch Perfect advertising

Pitch Perfect refreshes Google UMP consent information each app session and presents any required message before starting ads. Ad requests require UMP's `canRequestAds`. Settings > Privacy choices includes Ad privacy choices when UMP requires that entry point. iOS requests Apple's tracking permission separately before starting eligible ads. A telemetry choice does not grant tracking permission.

Publish the appropriate messages for **both** Pitch Perfect app IDs in AdMob > Privacy & messaging. Include each distributed production/private app configuration. The SDK cannot create or publish these messages. Validate regional messages with UMP test-device geography overrides, and verify ATT denial on a physical iOS device. Do not ship debug geography overrides.

## Publication work outside this PR

The published policy at https://apps.depoll.com/privacy/ describes the optional collection below; keep it, and the store disclosures, in step with it. (It still says Android may send app-open events to DepollSoft's analytics service, which the apps no longer do.)

The policy and store disclosures need to describe:

- Optional Google Analytics usage events (screens visited and features used, as listed in [analytics.md](analytics.md)), installation identifiers, and app/device information.
- Optional Firebase Crashlytics reports, stack traces, installation identifiers, and app/device information.
- Pitch Perfect's separate Google advertising/consent processing and iOS tracking permission.
- Account and synchronization data, how to change consent, data retention, and how to request deletion of data already received.

Review App Store Connect App Privacy and Google Play Data safety against the actual Firebase/AdMob account settings before submission. SDK privacy manifests ship with the dependencies, but do not update store questionnaires or the website policy. The code does not verify retention settings, publish the website, or change store declarations.

## SDK references

- [Firebase Analytics collection controls](https://firebase.google.com/docs/analytics/configure-data-collection)
- [Crashlytics opt-in reporting on iOS](https://firebase.google.com/docs/crashlytics/ios/customize-crash-reports#enable-opt-in-reporting)
- [Google UMP for Android](https://developers.google.com/admob/android/privacy) and [iOS](https://developers.google.com/admob/ios/privacy)
- [Apple user privacy and data use](https://developer.apple.com/app-store/user-privacy-and-data-use/)

## Verification

Run the shared `PrivacyChoicesTest` Android unit tests and `PrivacyConsentTest` instrumentation tests in both apps. On iOS, run `PrivacyChoicesTests` in Tag Master and `ConsentUITests` in both UI-test targets. These cover default-off choices, independent selection, persistence, Settings access, and revocation. The UI tests attach or save screenshots for visual review.

For a fresh install, confirm Analytics collection is disabled and Crashlytics automatic collection is off before answering. After opting in, check Firebase DebugView and send a test crash from a disposable device build. Relaunch after declining and confirm no further telemetry is sent. Published UMP message behavior, Firebase dashboard delivery, and store declarations require separate account/device verification.
