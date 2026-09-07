# Tag Master refresh acceptance ledger

## Confirmed brief

Both native apps, every screen considered. Singing together leads. Navigation may change; preserve Tag Master's charcoal/blue character, handwriting wordmark, icon and barber pole. The user selected the singing desk: Find a tag, Random Tag, Open Tag ID and repertoire on Home; Home/Browse/Search destinations; Settings as a utility. Operate mode, code-led, surface seed `a858fb14`, candidate 6. Pitch Perfect is out of scope.

## Implemented checks

- [x] Inventory and refresh iOS home, browse, search/filter, results/pagination, favorites, teachable lists, summary/details, tracks, videos, sheet music entry, ratings, settings/account/about, loading/empty/error states.
- [x] Corresponding Android coverage, preserving its native controls and conventions. Screen-by-screen details and evidence gaps are in the platform reports.
- [x] Immediate search, random selection, ID opening and saved repertoire without an account.
- [x] App-scoped semantic light/dark themes, scalable native typography, consistent spacing, labeled actions, larger targets and meaningful saved/selection feedback.
- [x] Structural tablet adaptation: iOS native tabs/sidebar and width-aware split detail; Android width-qualified navigation rails and readable content widths. Narrow windows and large text use appropriate single-column layouts.
- [x] Preserve local storage and sync keys/migrations. `DPAppDelegate.swift` and Android `ListModel.kt` remain unchanged. Preserve chart/track/video/share/rating actions and attribution.
- [x] Harden media cancellation/stale callbacks, child Back/insets, ID validation, query recovery and multi-page PDF ordering.
- [x] Targeted unit and native UI checks, actual phone/tablet-size captures, and independent Astra finish reviews. No web detector was run on native code.
- [x] Review fixes scored resolved: iOS full-width Summary and readable non-link attribution, including accessibility-sized ratings; Android distinct PDF pages in order.

## Verified evidence

### Android

- Debug app and instrumentation APK builds passed.
- 195 targeted JVM cases passed across a first selection and failure-only rerun. The latest five-suite XML independently confirms 130 passing cases, zero errors/failures; earlier passing selection counts are preserved in the report/logs.
- API36: 22 phone checks plus four selected tablet-width checks passed across runs. One additional native two-page PDF regression passed, verifying red first page, blue second page and zero remaining fixture PDF descriptors.
- JVM tests use SDK35/TestDeskApplication to work with Java17 and avoid starting Firebase. Production compile/target SDKs are unchanged.
- Tablet captures use an explicitly documented 800dp runtime configuration on the phone emulator, not a hardware tablet or separate tablet AVD.

### iOS

- Simulator build passed. New layout/navigation/data-notification/media tests and native phone/iPad flows passed through targeted correction runs. This is accumulated evidence, not a claim that the entire legacy suite was rerun after every edit.
- Final attribution tests verify actual static labels without URLs, enabled links with URLs, multiline accessibility text and at least 4.5:1 color contrast.
- Final rating unit test passes at 320/390pt in light/dark accessibility XXXL. The native dark XXXL test passes full-star/numeric-rating geometry, Rate reachability, anchored sheet presentation and actual dismissal. UIKit's native popover dismissal region is asserted when it omits Cancel.
- Final iPad pane result bundle: three passing tests, zero failures. Original phone/iPad screen inventory and dark-large runs are indexed in the report.
- Source registrations and UI-test scheme entries mechanically checked. Final touched Swift/Kotlin diagnostic batch: four files, zero findings. Legacy ObjC clang configuration references absent CocoaPods module maps; the SPM-backed Xcode build is the compile authority.

### Independent verdicts

Both verdict passes say ship at their listed-fix scope. They do not certify all devices, services or every unsampled state. The initial reviews and final verdicts are separate files.

## Local evidence index

Reports and captures are local review artifacts under `.impeccable/review/`, which may be ignored by Git:

- `tagmaster-iOS-report.md`
- `tagmaster-android-report.md`
- `tagmaster-iOS-finish-review.md`, `tagmaster-iOS-verdict.md`
- `tagmaster-android-finish-review.md`, `tagmaster-android-verdict.md`
- `tagmaster-iOS-review-fixes.md`
- `tagmaster-rating-main-verification.md`
- `tagmaster-chart-fix.md`

Android full test/build logs remain under `Android/TagMaster/build/`. iOS full logs and xcresult bundles remain under `/tmp/tm-*.log` and `/tmp/tm-*.xcresult`. Targeted regression tests are committed source candidates in each native test directory; no commit was created by this task.

## Remaining release checks

- [ ] Physical-device audio, Bluetooth/audio-focus interruptions, haptics and sustained performance.
- [ ] Real signed-in provider flows, account deletion and cross-device cloud synchronization. No test account was supplied.
- [ ] Full VoiceOver/TalkBack/Switch Access traversal, hardware tablet/multiwindow/rotation matrix and older supported OS versions.
- [ ] External playback/share targets, live rating submission and every remote chart/track format. Simulator captures do not establish these behaviors.
- [ ] Investigate existing Android catalog dates rendered as `12/31/69`; catalog date parsing/data was not changed in this design refresh.

## Design records

Built rules live in `iOS/tagmaster/DESIGN.md` and `Android/TagMaster/DESIGN.md`, each with a native-scoped sidecar. The shared surface brief links both. Root `DESIGN.md` and `.impeccable/design.json` still belong to Pitch Perfect and remain unchanged. Their pre-existing stale-sidecar notice was not repaired as a side effect.
