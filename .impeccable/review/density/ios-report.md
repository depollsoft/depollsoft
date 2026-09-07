# iOS density completion

Scope: `iOS/tagmaster` and `ios-*` evidence in this directory. Exact worktree root verified. Existing edits preserved; no commits, push, delegates, model changes, migrations, storage keys or labels feature. Main owns review and push.

## Result

Home has four discovery rows and two peer Your lists entries with live counts. Favorites and Teachable Tags push separate native lists. Shared compact rows keep title, distinct alternate title and ID/material status; Summary retains rating and Details retain downloads/date. Original page-scale barber pole, transparent rows, readable tablet margins, automatic heights and native push/Back remain.

## Failures resolved

- Initial fixture build assigned a readonly tracks property. Prior correction supplies `allPartsTrackUri`; the resumed native build passes.
- The route unit test previously checked titles before the pushed controller appeared. Prior correction settles the transition; `ios-unit-fix.log` records its pass.
- The off-window large-type sizing probe previously measured 142pt in both categories. The pending correction explicitly supplies category-compatible semantic fonts and disables label auto-rescaling in that isolated probe. Added font-size assertions prove 17pt versus 53pt. It now measures 142pt versus 974.7pt for a 320pt-wide long title plus alternate. No height expectation was relaxed. Native OS-scale evidence independently verifies production labels below.
- Favorites visibly changed Edit to Done but retained the custom accessibility label. `setEditing:animated:` now updates the label. The test uses stable `favorites.edit`, checks both labels, and requires reorder controls to appear and disappear. Removal, membership independence and Back/scroll-position checks now complete.
- Interrupted fixtures now restore on the next ordinary Debug launch, before Firebase setup. Backup creation fails closed before preferences change if the backup cannot be written.

## Tests

No whole-suite rerun. Existing logs were inspected first. Commands use `xcodebuild -workspace iOS/iOS.xcworkspace -scheme tagmaster -configuration Debug -derivedDataPath /tmp/tm-dd -disableAutomaticPackageResolution -parallel-testing-enabled NO` plus a device destination and the following selectors.

| Log | Selectors and operation | Result |
| --- | --- | --- |
| `ios-resume-phone.log` | `test`: density unit `testLongTitlesLargeTypeAndAlternateGrowWithoutClipping`, `testNormalDebugLaunchRestoresInterruptedFixtureBeforeStartup`; UI `testPopulatedRoutesMutationBackAndCaptures` | 3 passed |
| `ios-resume-dark.log` | `test-without-building`: UI `testDarkLargeListWrappingAndCaptures`, phone dark, accessibility XXXL | 1 passed |
| `ios-resume-tablet.log` | `test-without-building`: UI `testPopulatedRoutesMutationBackAndCaptures`, landscape iPad | 1 passed |
| `ios-resume-confirm.log` | `test`: UI `testLongTitleGeometryConfirmationCapture`, phone dark | 1 passed |

This continuation ran **6 test executions, 0 failures**. Across retained incremental results there are **14 distinct passing unit tests and 4 distinct passing phone UI tests**, plus the populated UI flow passing on iPad. This is not a claim that a full suite passed in one run. Earlier passing evidence is in `ios-unit-build2.log`, `ios-unit-fix.log` and `ios-phone-light.log`. The interrupted `ios-phone-light-fix.log` has no completed test result and is not counted.

The unit coverage includes Home 0/1/100, empty destinations, native routes, independent memberships and ordering/removal, both lists' scroll restoration, density, alternate titles, failed-load recovery, cache/preference restoration and the normal-launch recovery addition. Xcode source registrations for Favorites, fixtures and both test files were checked. Swift edit diagnostics were clean; standalone lens tools were unavailable in this session. `git diff --check -- iOS/tagmaster` passes.

## Native evidence

All screenshots use synthetic local fixtures, IDs 900001–900100, not real catalog/account data. Capture files are at most 900px on the long edge and below 300KB.

- `ios-phone-{home,favorites,teachable,browse}.png`: iPhone 17 Pro, iOS 26.5, light/default text.
- `ios-phone-{home,favorites,teachable,browse}-dark-large.png`: same phone, dark/accessibility XXXL.
- `ios-tablet-{home,favorites,teachable,browse}.png`: iPad Pro 11-inch M5, iOS 26.5, landscape/light/default text.
- `ios-phone-long-title-dark-large-confirm.png`: one necessary confirmation. The original `ios-phone-long-title-dark-large.png` caught only the start of the row at the viewport edge and is not full-title visual proof.
- `ios-phone-native-geometry-synthetic.txt`: actual XCUI hierarchy. The first saved row is **61pt default, 303pt XXXL**. The complete long-title row is **561pt**, at y≈177 on a 402pt-wide phone. Both its top and bottom are asserted inside the visible viewport.

All 14 captures were inspected individually through native image reads. Short rows show roughly eleven complete saved entries on the default phone; long/alternate titles grow rather than truncate. Home at XXXL scrolls to its list entries. The pole remains page-scale without opaque cards. Landscape iPad keeps one readable-width list and native Back.

## Fixture safety and cleanup

`TMDensityFixture` implementation/declarations and launch behavior are Debug-only. Fixture/cleanup launches return before Firebase configuration and `extraInit`; the private static `userDoc` remains nil, so existing mutation methods cannot issue account writes. Initial fixture setters also use `doSave:NO`. Catalog loading/query methods use in-memory synthetic tags and do not write the tag cache. Normal product models and sync code are unchanged.

UI teardown launches `--density-cleanup` and asserts `density.restored`. Unit teardown restores preferences and swizzles. A focused unit test verifies that ordinary next Debug startup restores the interrupted backup before proceeding. Final direct simulator checks found zero synthetic memberships and no pending `tagmaster-density-defaults.plist` on either device. Both had empty Favorites/Teachable lists after restoration; appearance is restored to light and UI teardown restores portrait. No ordinary signed-in app launch was used for cleanup.

## Limits

Simulator evidence only. No hardware, VoiceOver listening pass, Release build, fresh live catalog/network run, tablet dark/large-text or complete orientation matrix was run in this continuation. Dark/light status and large-type layout are native evidence, not a measured contrast audit. Reordering is verified via existing controller methods and native editing controls, not a drag-reorder UI gesture. Future labels/custom lists remain documentation only.

Scoped built-truth updates: `iOS/tagmaster/DESIGN.md` and `iOS/tagmaster/.impeccable/design.json`. Root design-sidecar drift was reported by context setup and left outside this iOS scope.
