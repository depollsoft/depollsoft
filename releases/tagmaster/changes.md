# Tag Master 3.0.0 for iOS and 6.0.0 for Android

## Baseline evidence and remaining checks

These first-release baselines are inferred from repository changes and public store history; they are not verified against archived store binaries.

- iOS: `75d8daf8` set version 2.0.2 on August 3, 2021. The public App Store listing is 2.0.2, released August 4, 2021. Subsequent commits before that release affected Android, not the iOS app or its shared library.
- Android: `8ee81c37` is the last substantive changelog edit, dated August 22, 2022. It adds the 5.2.1 email-sign-in fix named in the public Play listing's notes. Play reports August 21, 2022 as its update date, so the version and content match but the dates differ by one day. The January 2026 changelog move preserved the contents.
- Confirm these inferred baselines and the new build number against store records, including unpublished uploads, before submission. Store console access was unavailable during preparation.
- The production profile `AppStore_depollsoft.tagmaster.mobileprovision` is missing. [Signing provisioning](https://github.com/depollsoft/depollsoft/actions/runs/35367449149) failed cloning `depollsoft/certificates` with `Repository not found`. Correct the workflow credential's read/write access and rerun provisioning before opening the release PR.

Public listings: [App Store](https://apps.apple.com/us/app/tag-master/id721186126), [Google Play](https://play.google.com/store/apps/details?id=depollsoft.tagmaster).

The commit evidence below includes each app's paths and the shared paths selected by `release.py`. Commit subjects describe source history, not customer-facing release notes.

## ios: 75d8daf8f0e45caa68d5bb26ad31c768346d750a..HEAD

7941d036 release: update major-version changelogs and preparation skill
cfdae998 security: remove private data and prepare public repository checks
e7b473bf Automate release assets and independent mobile app releases (#50)
c4c4ae50 Wear: draw the Laboratory Instrument on the watch
fb255120 Tag Master iOS: sheet-music key button is the glyph and the written key
726b0fac Tag Master Android: one barber-pole watermark behind both tablet panes
e7607aad Tag Master iOS: one barber-pole watermark behind the whole iPad split
6d4d7aee Tag Master iOS: keep sheet music in the detail column with a full-screen toggle
c7f147c6 Tag Master iOS: reconcile Details tests with main after merge
27911c21 Tag Master iOS: match Android's summary, rows and captions
5187659f Tag Master iOS: one accent shared with Android
ec6b1823 Tag Master: tablet list-detail on Android and iPad
cf2a3a3c TagMaster iOS: address review of the inline track player
7686b4d1 TagMaster iOS: inline track player with balance slider
9a3435a5 Tag Master: polish Android and iOS UX
dec0c075 Android: restore FirebaseUI and Crashlytics
3737447d Android: keep beta icon badges inside round masks
edb01d01 PitchPerfect: redesign as grayscale Laboratory Instrument (Android+iOS)
23ec1825 feat: add private build diagnostics (#30)
35ba20fe Embed iOS screens in navigation controllers
16ece139 Use system navigation bars for iOS top actions
5e2d3683 Give iOS glass toolbar buttons more room
cf8b42f5 Refine iOS glass bars and vertical spacing
67386e5e Match Apple sign in to provider buttons
7459801e Refine iOS glass controls and Apple sign in
fa0b2802 Pad iOS title icons and match Apple button style
b0f9ae56 Polish iOS auth and title bar controls
f777fc3d Fix iOS auth providers and beta ads
a61b0456 Fix Android content insets on Android 16
e6f8490a Fix private preview builds after dependency updates
9b1bf37b Remove obsolete Gradle 2 wrapper
b4ecf9d4 Document updated local development setup
0264faa6 Keep Firebase client configs available locally
db7673a3 iOS: isolate Tag Master controller tests
ca00dcf3 iOS: harden controller edge cases
973ec706 Update dependencies and retire legacy services
214644e5 Restore Pitch Perfect and Tag Master mobile builds (#19)
f3ce2b29 ci: add PR build distribution to TestFlight and Play Internal Testing (#17)
3f0d0bb7 ios: restore auth app branding
a956e3b1 ios: Restore Google, Facebook, and Apple sign-in providers
7bcd13f3 Address PR review feedback from Copilot
5421bb8f Fix flaky tests: attach window to active scene for proper VC hierarchy
7c07c830 Fix flaky test: ensure view controller hierarchy is attached before presenting
1b41932a Migrate iOS projects from CocoaPods to Swift Package Manager
09e336a9 Modernize Android dependencies (#13)
a1bd3542 Add CI workflows for Android and iOS testing (#9)
71d22a69 Update build.gradle
4f4b0112 Update bindroid
0919ebf6 Fix license and widget
74740add Fix license checker
a75a320d Update Bindroid
b8721586 Hopefully fix subscription issues
25cac81c Fix binding bug
7bef333e Update bindroid reference
8ee81c37 Fixes to Pitch Perfect
1d5222ca Tag Master Android 5.2.0
c7bd8ece Add monochrome icons
43a3f5f8 Checkpoint
52939cfb Moves songsmodel to Firestore
7f1c4f65 Subscriptions working again
eff30397 Update tag master
72680ad6 Get ads working again and fix some crashers
517d3332 Pitch Perfect dark theme
535384db Fix query issue
e801da27 Fix tag loading
e76b4ab0 More resilient backup code

## android: 8ee81c37101780bd59ef29547c3011849018198e..HEAD

7941d036 release: update major-version changelogs and preparation skill
cfdae998 security: remove private data and prepare public repository checks
e7b473bf Automate release assets and independent mobile app releases (#50)
c4c4ae50 Wear: draw the Laboratory Instrument on the watch
fb255120 Tag Master iOS: sheet-music key button is the glyph and the written key
726b0fac Tag Master Android: one barber-pole watermark behind both tablet panes
e7607aad Tag Master iOS: one barber-pole watermark behind the whole iPad split
6d4d7aee Tag Master iOS: keep sheet music in the detail column with a full-screen toggle
c7f147c6 Tag Master iOS: reconcile Details tests with main after merge
27911c21 Tag Master iOS: match Android's summary, rows and captions
5187659f Tag Master iOS: one accent shared with Android
ec6b1823 Tag Master: tablet list-detail on Android and iPad
cf2a3a3c TagMaster iOS: address review of the inline track player
7686b4d1 TagMaster iOS: inline track player with balance slider
9a3435a5 Tag Master: polish Android and iOS UX
dec0c075 Android: restore FirebaseUI and Crashlytics
3737447d Android: keep beta icon badges inside round masks
edb01d01 PitchPerfect: redesign as grayscale Laboratory Instrument (Android+iOS)
23ec1825 feat: add private build diagnostics (#30)
35ba20fe Embed iOS screens in navigation controllers
16ece139 Use system navigation bars for iOS top actions
5e2d3683 Give iOS glass toolbar buttons more room
cf8b42f5 Refine iOS glass bars and vertical spacing
67386e5e Match Apple sign in to provider buttons
7459801e Refine iOS glass controls and Apple sign in
fa0b2802 Pad iOS title icons and match Apple button style
b0f9ae56 Polish iOS auth and title bar controls
f777fc3d Fix iOS auth providers and beta ads
a61b0456 Fix Android content insets on Android 16
e6f8490a Fix private preview builds after dependency updates
9b1bf37b Remove obsolete Gradle 2 wrapper
b4ecf9d4 Document updated local development setup
0264faa6 Keep Firebase client configs available locally
db7673a3 iOS: isolate Tag Master controller tests
ca00dcf3 iOS: harden controller edge cases
973ec706 Update dependencies and retire legacy services
214644e5 Restore Pitch Perfect and Tag Master mobile builds (#19)
f3ce2b29 ci: add PR build distribution to TestFlight and Play Internal Testing (#17)
3f0d0bb7 ios: restore auth app branding
a956e3b1 ios: Restore Google, Facebook, and Apple sign-in providers
7bcd13f3 Address PR review feedback from Copilot
5421bb8f Fix flaky tests: attach window to active scene for proper VC hierarchy
7c07c830 Fix flaky test: ensure view controller hierarchy is attached before presenting
1b41932a Migrate iOS projects from CocoaPods to Swift Package Manager
09e336a9 Modernize Android dependencies (#13)
a1bd3542 Add CI workflows for Android and iOS testing (#9)
71d22a69 Update build.gradle
4f4b0112 Update bindroid
0919ebf6 Fix license and widget
74740add Fix license checker
a75a320d Update Bindroid
b8721586 Hopefully fix subscription issues
25cac81c Fix binding bug
7bef333e Update bindroid reference
