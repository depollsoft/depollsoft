# Pitch Perfect 3.0.0 for iOS and 5.0.0 for Android

## Verified store baseline and source-review boundary

Authenticated [App Store Connect and Google Play history](https://github.com/depollsoft/depollsoft/actions/runs/35408894261) was checked on September 18, 2026. The report lists all uploaded production-app builds, including unpublished builds. No newer upload exists beyond the shipped builds below. Repository commits are used as conservative review boundaries because the old uploads do not record a Git SHA; release-time version edits were sometimes committed after upload.

- iOS: the live store listing is 2.0.1, attached to binary version 2.0.3 / build 2.0.4, uploaded September 19, 2022. This resolves the public/source version mismatch: source 2.0.3 did ship under the 2.0.1 listing. `74740add`, committed September 21, records the matching binary version/build and a pitch-button title fix. Since it postdates the upload, the review conservatively starts at the earlier `b258d967` account-deletion change, retaining all potentially unshipped changes rather than assuming the whole later commit was in the binary.
- Android: Play confirms published phone version 4.0.0 / build 220925000 and Wear build 220922001. `25f49332` is the September 25, 2022 phone source baseline. The last substantive 4.0.0 changelog edit was `a383c41a` on August 24, 2022; the January 2026 move preserved it. The production lane retains published Wear build 220922001 while replacing the phone build. The old draft also references 220922001; there is no newer uploaded code.
- The planned build number exceeds every authenticated upload: the latest are iOS build 2.0.4 and Android build 220925000. All four store checks passed; no production-app upload from the 2026 preview work appears in these records.
- [Tag Master signing provisioning](https://github.com/depollsoft/depollsoft/actions/runs/35379275553) passed after the repository credential was updated. Verified the production Tag Master, Pitch Perfect, and Pitch Perfect widget profile filenames in the signing repository.

Public listings: [App Store](https://apps.apple.com/us/app/pitch-perfect-pitch-pipe/id539417298), [Google Play](https://play.google.com/store/apps/details?id=depollsoft.pitchperfect).

The commit evidence below includes each app's paths and the shared paths selected by `release.py`. Commit subjects describe source history, not customer-facing release notes. The Wear companion binary is not part of this production lane.

## ios: b258d9677b8d37a5ac305f9490a3723c39251f84..HEAD

0de11c60 ios: support both simulator architectures
1c16afb8 test: stabilize native UI checks on Android 11
176d05b9 test: repair mobile UI fixtures and isolate network handlers
7941d036 release: update major-version changelogs and preparation skill
cfdae998 security: remove private data and prepare public repository checks
e7b473bf Automate release assets and independent mobile app releases (#50)
b634f324 Wear: bound the ring's hit band and cancel stale accessibility stops
5637ab07 Icons: beta badge on the watch, store icon derived from the launcher
47dcb8f1 Settings: install Pitch Perfect on a paired watch
d21cab7a Wear: full-bleed adaptive launcher icon
87f16400 Wear: declare the watch app standalone
c4c4ae50 Wear: draw the Laboratory Instrument on the watch
9a3435a5 Tag Master: polish Android and iOS UX
05432b81 Android widget: light cells on the tap that sounds them
4b9447fc Song editor: choose keys silently
a3b8f7bf Android: keep the chosen key lit through a second tap
1d571023 Song editor: pick keys from the signature list
2aa4e504 Song editor: choose keys on a dial instead of a spinner
c1c3f52b iOS: finish the list typography pass
037f46ab iOS: paint the widget glow beneath the ring like the app
67fd2da1 iOS: give the widget the app's pitch pipe voice
3d191c4c iOS: give song rows the Android typography and lit state
4df4b630 Android: keep song list subscribed to live model updates
e9da2b8b iOS: share the barbershop Easter egg with the widget
ff772455 iOS: toggle widget notes independently and remove debug trace
4a519a60 test: require widget selection to persist beyond reload
278ec6cd iOS: let the sounding note decide the widget toggle
4789c69f iOS: share the widget app group in preview builds
b105fb29 iOS: trace widget renders in beta builds
035dd654 iOS: light widget cells the instant they are tapped
f1ba4a7c iOS: lay out the pitch pipe widget with a radial Layout
1107c3bf iOS: toggle widget tones on tap
abf9f659 iOS: fix interactive pitch pipe widget state
235257ad iOS: fix pitch pipe widget playback
02827b73 Pitch pipe: name a barbershop seventh in the readout
e74898e9 Android: initialize ads on the main thread again
fef01385 Android: render widget updates off the main thread
780428b9 Android: stop rebuilding the toolbar menu on tab change
c57ff9ef Android: collapse shared screen background
828c0f4f Android: coordinate tab animations
3572b142 Android: eliminate tab and sync jank
a6056fe3 Android: patch FirebaseUI null email and stop sync churn
dec0c075 Android: restore FirebaseUI and Crashlytics
1cade4d5 Android: authenticate Facebook directly and fix widget glow
a51fe1ca Android: prevent duplicate sync listeners after login
282e8f4a Android: harden FirebaseUI login and Facebook recovery
03b3b4df Android: couple widget visuals to targets and restore dialog actions
5cb5e9f7 PitchPerfect: align octave tops and keep widget playback in place
b90bbdfa PitchPerfect: add inclusive octaves and iOS widget
4622032d Android: rebuild pitch pipe widget and guard note swipes
e7b0a7c5 PitchPerfect: theme-following bars on Android and screen titles on iOS
9cf563f8 PitchPerfect: align song row margins and title iOS Songs
363f3063 PitchPerfect: refine song rows and instrument caption
c66ae744 PitchPerfect: polish settings and adaptive navigation
17e51d69 Android: keep selected song rows legible
2b1aa9ce PitchPerfect: add tactile instrument delight
8c4e449e iOS: run score beneath Liquid Glass chrome
83cecd5a PitchPerfect: restore Liquid Glass and surface song actions in app bar
e44f1fc0 PitchPerfect: reveal score through lists and unify iOS ads
49da0fe0 PitchPerfect: make score background full bleed
8fcc3f1c PitchPerfect: repair heritage background layering
2abd76dc PitchPerfect: restore chords and refine song editing
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
e4d80771 Fix iOS login crash with stale auth state
a61b0456 Fix Android content insets on Android 16
e6f8490a Fix private preview builds after dependency updates
9b1bf37b Remove obsolete Gradle 2 wrapper
b4ecf9d4 Document updated local development setup
0264faa6 Keep Firebase client configs available locally
ca00dcf3 iOS: harden controller edge cases
973ec706 Update dependencies and retire legacy services
214644e5 Restore Pitch Perfect and Tag Master mobile builds (#19)
f3ce2b29 ci: add PR build distribution to TestFlight and Play Internal Testing (#17)
3f0d0bb7 ios: restore auth app branding
a956e3b1 ios: Restore Google, Facebook, and Apple sign-in providers
7bcd13f3 Address PR review feedback from Copilot
1b41932a Migrate iOS projects from CocoaPods to Swift Package Manager
09e336a9 Modernize Android dependencies (#13)
a1bd3542 Add CI workflows for Android and iOS testing (#9)
71d22a69 Update build.gradle
4f4b0112 Update bindroid
25f49332 Fix a few bugs
0919ebf6 Fix license and widget
74740add Fix license checker
e694bd98 Bump version number
a75a320d Update Bindroid

## android: 25f4933267001e10997c4b4b9f19d57b94f70039..HEAD

0de11c60 ios: support both simulator architectures
1c16afb8 test: stabilize native UI checks on Android 11
176d05b9 test: repair mobile UI fixtures and isolate network handlers
7941d036 release: update major-version changelogs and preparation skill
cfdae998 security: remove private data and prepare public repository checks
e7b473bf Automate release assets and independent mobile app releases (#50)
b634f324 Wear: bound the ring's hit band and cancel stale accessibility stops
5637ab07 Icons: beta badge on the watch, store icon derived from the launcher
47dcb8f1 Settings: install Pitch Perfect on a paired watch
d21cab7a Wear: full-bleed adaptive launcher icon
87f16400 Wear: declare the watch app standalone
c4c4ae50 Wear: draw the Laboratory Instrument on the watch
9a3435a5 Tag Master: polish Android and iOS UX
05432b81 Android widget: light cells on the tap that sounds them
4b9447fc Song editor: choose keys silently
a3b8f7bf Android: keep the chosen key lit through a second tap
1d571023 Song editor: pick keys from the signature list
2aa4e504 Song editor: choose keys on a dial instead of a spinner
c1c3f52b iOS: finish the list typography pass
037f46ab iOS: paint the widget glow beneath the ring like the app
67fd2da1 iOS: give the widget the app's pitch pipe voice
3d191c4c iOS: give song rows the Android typography and lit state
4df4b630 Android: keep song list subscribed to live model updates
e9da2b8b iOS: share the barbershop Easter egg with the widget
ff772455 iOS: toggle widget notes independently and remove debug trace
4a519a60 test: require widget selection to persist beyond reload
278ec6cd iOS: let the sounding note decide the widget toggle
4789c69f iOS: share the widget app group in preview builds
b105fb29 iOS: trace widget renders in beta builds
035dd654 iOS: light widget cells the instant they are tapped
f1ba4a7c iOS: lay out the pitch pipe widget with a radial Layout
1107c3bf iOS: toggle widget tones on tap
abf9f659 iOS: fix interactive pitch pipe widget state
235257ad iOS: fix pitch pipe widget playback
02827b73 Pitch pipe: name a barbershop seventh in the readout
e74898e9 Android: initialize ads on the main thread again
fef01385 Android: render widget updates off the main thread
780428b9 Android: stop rebuilding the toolbar menu on tab change
c57ff9ef Android: collapse shared screen background
828c0f4f Android: coordinate tab animations
3572b142 Android: eliminate tab and sync jank
a6056fe3 Android: patch FirebaseUI null email and stop sync churn
dec0c075 Android: restore FirebaseUI and Crashlytics
1cade4d5 Android: authenticate Facebook directly and fix widget glow
a51fe1ca Android: prevent duplicate sync listeners after login
282e8f4a Android: harden FirebaseUI login and Facebook recovery
03b3b4df Android: couple widget visuals to targets and restore dialog actions
5cb5e9f7 PitchPerfect: align octave tops and keep widget playback in place
b90bbdfa PitchPerfect: add inclusive octaves and iOS widget
4622032d Android: rebuild pitch pipe widget and guard note swipes
e7b0a7c5 PitchPerfect: theme-following bars on Android and screen titles on iOS
9cf563f8 PitchPerfect: align song row margins and title iOS Songs
363f3063 PitchPerfect: refine song rows and instrument caption
c66ae744 PitchPerfect: polish settings and adaptive navigation
17e51d69 Android: keep selected song rows legible
2b1aa9ce PitchPerfect: add tactile instrument delight
8c4e449e iOS: run score beneath Liquid Glass chrome
83cecd5a PitchPerfect: restore Liquid Glass and surface song actions in app bar
e44f1fc0 PitchPerfect: reveal score through lists and unify iOS ads
49da0fe0 PitchPerfect: make score background full bleed
8fcc3f1c PitchPerfect: repair heritage background layering
2abd76dc PitchPerfect: restore chords and refine song editing
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
e4d80771 Fix iOS login crash with stale auth state
a61b0456 Fix Android content insets on Android 16
e6f8490a Fix private preview builds after dependency updates
9b1bf37b Remove obsolete Gradle 2 wrapper
b4ecf9d4 Document updated local development setup
0264faa6 Keep Firebase client configs available locally
ca00dcf3 iOS: harden controller edge cases
973ec706 Update dependencies and retire legacy services
214644e5 Restore Pitch Perfect and Tag Master mobile builds (#19)
f3ce2b29 ci: add PR build distribution to TestFlight and Play Internal Testing (#17)
3f0d0bb7 ios: restore auth app branding
a956e3b1 ios: Restore Google, Facebook, and Apple sign-in providers
7bcd13f3 Address PR review feedback from Copilot
1b41932a Migrate iOS projects from CocoaPods to Swift Package Manager
09e336a9 Modernize Android dependencies (#13)
a1bd3542 Add CI workflows for Android and iOS testing (#9)
71d22a69 Update build.gradle
4f4b0112 Update bindroid
