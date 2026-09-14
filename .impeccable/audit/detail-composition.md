# Detail composition implementation

## Plan before edits

Scope is Tag Master Summary and Details on Android and iOS, plus app-local layout helpers and regression tests. One owner keeps the same anatomy on both platforms. No commit or push.

Reading order: identity; compact facts and rating unit; aligned Key/Sheet performance lane; full-width labelled prose. Details uses one caption axis with first-baseline alignment and proximity breaks before contributor groups. Every metadata group switches together to caption-above-value when measured captions plus a useful scaled value width no longer fit. Summary only splits complete facts/actions and prose groups when both retain useful width; absent prose takes no column.

Implementation:

1. Preserve existing bindings, IDs, data formats, URL targets, optional-field truth and pitch/loading callbacks. Replace form-shaped prose and orientation-only layout, not native tabs or page insets.
2. Add app-local width-aware metadata/summary containers. Reserve the same trailing accessory rail beside both performance faces, including the iOS pitch widget's internal 2pt inset. No busy-state constraints or renderer changes.
3. Collapse optional action/status/prose groups as units. Use 4pt/dp label-to-prose spacing and 16 between sections.
4. Add production-layout geometry tests, update stale grid assumptions, compile both apps, run affected tests and generator checks. Back up current binaries, private/external data and settings before native tests; validate manifests and restore with byte comparisons.
5. Capture ordinary light/dark Summary and Details using matching data first, then scaled/narrow/wide/missing cases. One combined visual review and at most one correction/confirmation. Captures are JPEG <=800px with the tagmaster-detail-composition prefix.

## Acceptance results

- [x] Ordinary identity/prose leading edges and metadata value axes asserted on production views. iOS Summary and Details first-visible baselines agree within 2pt; Android native text baselines agree exactly in compact rows.
- [x] Rating number, indicator and Rate stay together. Exact `3.49` formatting is checked; platform stars/progress displays remain.
- [x] Actual Key/Sheet faces share leading/trailing edges and height, with 48dp/44pt minimum targets. Both reserve the same external pole rail. Idle/busy/terminal frames survive layout passes. Existing real pitch lifecycle tests now explicitly assert held and released frame equality on both platforms.
- [x] Optional sheet/key/AKA/version/status/prose blocks collapse. Lyrics-only, notes-only and neither-prose combinations remove orphan spacing. Empty iOS contributor strings collapse; no data or version fields were invented.
- [x] Whole-group metadata fallback uses measured captions and useful value width. Summary splits only when two complete groups fit at the current text scale, and removes the second column when prose is absent.
- [x] All original Android layout IDs remain, including landscape-only `tagIdLabel`. Existing field/model bindings, numeric formatting, URL targets and accessibility actions remain. iOS helper sources are registered in the app target.
- [x] Native tabs, page insets, title fonts/colors, pitch feedback, opaque Android sheet key, availability, footer and matched loading/artwork owners are unchanged.
- [x] Both apps build. Targeted native behavior and geometry checks pass. Full-suite qualification and remaining coverage limits are below.
- [x] Original APK/app binaries, private/external data and recorded settings restored and byte-checked. Backups retained. No commit or push.

## What changed

Android:

- `res/layout/tagsummaryview.xml` and `res/layout-land/tagsummaryview.xml` now use the same anatomy. Identity has no 20dp inset; the rating action no longer occupies the far page edge. Key and Sheet share one full-width action lane plus a reserved pole rail. Prose has labels above full-width text.
- `res/layout/tagmiscview.xml` uses one metadata axis with source, posted, arranged and sung groups separated by proximity.
- `TagSummaryFragment.kt` hides the complete sheet wrapper and saved-status group using the existing model truth.
- New `DetailCompositionLayout.kt` owns metadata measurement, first-baseline placement, conditional section gaps and width-driven Summary columns. New `DetailCompositionTest.kt` exercises the actual activity/fragments. `FooterPitchRegressionTest.kt` adds held/released face assertions to its existing silent-player fixture.

All Android paths above are under `Android/TagMaster/src/main` except the two test files under `src/androidTest/java/depollsoft/tagmaster`.

iOS:

- `iOS/tagmaster/tagmaster/DPTagSummaryController.m` replaces the three-column form with identity, facts, performance and prose groups. An app-local pitch adapter removes the shared widget's 2pt face inset without changing its sound targets. Rate no longer constrains performance-control height.
- `DPTagDetailController.m` shares the new `TMDetailLayout.h/.m` policy. Each pair contains the full native button hit region while aligning the caption to its first text baseline. `reloadValues` explicitly remeasures late-bound text.
- `TagmasterAppLogicTests.m` replaces old grid/removal assumptions with production geometry and omission checks, adds paired capture fixtures, and strengthens pitch raster/frame synchronization. No fake controls or production test-only branches were added.

## Verification and actual failures

Logs and preserved evidence are under `/tmp/tm-detail-composition/`.

| Check | Result and evidence |
| --- | --- |
| Android Debug app and androidTest APK builds | Pass, `android-final-build.log`, `android-held-frame-build.log`; JDK 17 |
| Android full unit suite | 300 tests, 0 failures, `android-final-unit.log` and Gradle XML results |
| Android existing native subset | 33 detail/pitch tests passed, `android-affected-native.log`; compact pending/terminal/lifecycle test passed, `android-compact-native.log` |
| Android new production geometry | Ordinary light/dark; 320/393dp at font 1.0/1.3/2.0; 834dp-wide at 1.0/2.0; long 320dp/font-2 content; missing combinations; busy-frame layout passes. Each invocation passed. Logs: `android-native-*.log`, `android-w*.log`, `android-wide*.log`, `android-long-final.log`, `android-final-artifact.log` |
| Android held/released frame assertion | Passed on the production pitch button with existing silent-player fixture, `android-held-frame.log` |
| iOS build-for-testing | Pass, including new source registration; later targeted `test` invocations also rebuilt successfully |
| iOS full unit suite | 220 tests ran: 219 passed, one pitch raster timing assertion failed, `ios-full-unit.log`. This is not reported as a full-suite pass |
| iOS final geometry subset | 6 tests, 0 failures, `ios-late-binding-final.log`. Covers Summary metrics/page cycles, omission, Details resize/restore, ordinary geometry and adaptive groups. Final added Summary baseline/axis assertions also pass, `ios-axis-final.log` |
| iOS pitch failure resolution | The isolated test passed before modification. Its assertion now waits for the actual configured-button fill to finish rendering after re-enable, rather than a fixed 120ms delay. Held/released frames and pixel/contrast assertions pass, `ios-held-frame-final.log`. No production pitch behavior was weakened |
| iPad production-hosted geometry | Ordinary and adaptive cases pass on final production code, `ios-ipad-final.log`; Details resize/restore also passed in `ios-ipad-geometry.log` |
| Actual app XCUITests | Phone: 3 passed for native tabs/rotation, Summary prose after page changes and rating scroll/cancel, `ios-native-ui.log`. iPad: native tabs/portrait/landscape passed, `ios-ipad-native-ui.log` |
| Generator checks | Canonical artwork check and 16 generator tests passed, `generator.log`; no artwork or generated definitions changed |
| Static checks | XML parses, project plist parses, all original Android IDs present, `git diff --check` clean. LSP tools were not exposed; native compilers provided diagnostics |

Failures were inspected rather than marked expected or skipped:

1. Android's first unit run had 7 failures from a baseline child index pointing at an omitted rating number. The app-local rating baseline adapter fixed the crash. The failing subset and subsequent full 300-test run passed.
2. An oversized project-file read initially produced a truncated rewrite. Restored the untouched HEAD project snapshot, applied only six registration lines with bounded edits, and verified plist parsing and compilation.
3. iOS resize/restore exposed baseline-aligned UIStackView rows that did not contain the full button bounds. Explicit measured pairs fixed row containment and repeatable content height. Old tests now budget above-text captions and matched action titles instead of the removed grid topology.
4. Combined visual review exposed a zero-width iOS rating number and captures taken during native tab transitions. The rating now retains its lexical width; capture fixtures wait for presentation to settle. Confirmation then exposed a late-loaded Type label with zero height. Explicit metadata invalidation and a new nonzero text-height regression fixed that functional defect before handoff.
5. A final Android install was refused because the cached date-based Debug version was older than the installed test artifact. `adb install -r -d` installed the debuggable build without changing configuration or clearing data; the final-artifact geometry test passed.

No unchanged full suite was rerun after the iOS full-suite failure. Subsequent checks target changed behavior and failures. No full app reassessment or final detector scan was run. The prior mechanical result remains zero findings with native rule coverage unproven; main owns the final scan.

## Captures and review

Eight selected JPEGs, each at most 800px, under `.impeccable/review/`:

- `tagmaster-detail-composition-paired-light-summary.jpg`
- `tagmaster-detail-composition-paired-dark-summary.jpg`
- `tagmaster-detail-composition-paired-light-details.jpg`
- `tagmaster-detail-composition-paired-dark-details.jpg`
- `tagmaster-detail-composition-paired-large-summary.jpg`
- `tagmaster-detail-composition-paired-wide-summary.jpg`
- `tagmaster-detail-composition-paired-wide-missing.jpg`
- `tagmaster-detail-composition-paired-ipad-details.jpg`

Ordinary light/dark Summary and Details were captured first. Android is left/top; iOS is right/bottom. The last pair is iOS-only. The representative fixture is Lost, tag 1809, AKA In Your Eyes, rating 3.49, four parts, Minor:G, matching lyrics and contributor names. Platform-specific fields, date formats and styles remain different intentionally.

Source qualification: Android screenshots are the real activity with its production fragments and a process-local model loader. iOS paired screenshots are native hosted windows containing the production navigation controller, detail controller and real UIKit tab controller, with model data assigned locally. They are not browser mockups or live catalog claims. Separate XCUITests verify the actual launched app, including native iPad navigation. Originals are retained in `/tmp/tm-detail-composition/captures/`.

One combined review and one correction/confirmation cycle were used. The late-bound Type failure received an additional focused functional verification, not another styling pass. No polishing changes followed. Wide screenshots show the initial short viewport; face equality there is measured by native tests rather than fully visible in that crop. Large screenshots focus on the action lane and final provenance rows.

Coverage limits: the tested matrix is not every theme × font × width × omission permutation. Hosted iPad widths test split-pane-sized containers; actual iPad XCUITests cover native portrait/landscape navigation, not every interactive divider position. Long text is exercised, but no exhaustive locale, RTL or hardware-device matrix is claimed. The complete iOS unit suite has not been rerun after its fixed raster timing failure.

## Device preservation

Fresh backups were made before installation and validated: Android tar manifests rejected unsafe paths and were extracted; stopped iOS app/data copies compared before reboot. Earlier passes' backups were not reused as current data.

- Android original app/test APKs and all private/external file contents compare byte-for-byte after restoration. Font scale 1.0, night yes, physical size/density without overrides, motion/rotation settings and recorded animation settings match. Final evidence: `restore-android-final.log` and its `android/readback.*` comparisons.
- Both iOS original app bundles and stopped private-data copies compare equal. Post-boot user-data checksum comparisons also pass. Only current container-manager identity metadata and OS-regenerable SplashBoard snapshots are excluded from that running comparison, matching the validated restore procedure. Recorded appearance/content size/motion settings match. Final evidence: `restore-ios-final.log` and per-device diff files.
- Both final restore logs contain `RESTORE_VERIFIED`. Backups and logs remain under `/tmp/tm-detail-composition/{android,ios}`. The new fixtures never submit ratings, mutate live saved lists or perform sync writes.

Ready for main's scoped code review and final mechanical scan. No commit or push.

## Final iOS integration repair supplement

This supersedes the earlier raster-timing diagnosis and incomplete full-suite qualification above.

Root cause: `playKeyNote` scheduled an unconditional stop against a shared `DPNote`. TouchCancel stopped playback but left that callback pending. It subsequently stopped the next test's held C note. The ordered two-method reproduction showed Summary selected, window attached, no appearance transition, and a valid polling timer before the gesture. Playback changed from true to false while still held; the clear configuration then yielded zero fill pixels. Native mounting and raster color space were not the cause.

Changes are limited to:

- `iOS/tagmaster/tagmaster/DPTagSummaryController.m`: weak-owner, controller-local activation generation guards delayed cleanup. Summary and sheet callbacks invalidate it on later touch/release/cancel, note rebinding, detachment and repeated accessibility activation. Shared sound targets, local pitch adapter geometry and all composition styles remain unchanged.
- `iOS/tagmaster/tagmasterTests/TagmasterAppLogicTests.m`: retain state diagnostics; assert native readiness before the first gesture and actual playback after the existing rendered-fill wait. Add regressions for a retained cancelled owner followed by a same-note hold, touch replacement, note replacement, detachment, and quick repeated activation across the old deadline. Existing pixels, original-rendering symbols, contrast, frame equality, toggle, visibility and cancellation assertions remain. No longer raster wait or assertion waiver.

Results, with raw logs under `/tmp/tm-detail-composition/`:

| Check | Result | Log |
| --- | --- | --- |
| Original ordered pair before fix | 2 tests, 1 failed test, 2 assertion failures | `ios-repair-two-diagnostic.log` |
| Two new ownership regressions before fix | 2 tests, both failed, 8 assertion failures | `ios-repair-regressions-red.log` |
| Ordered pair + regressions + sheet raster lifecycle after fix | 5 tests passed, 0 failures | `ios-repair-targeted-green.log` |
| Full `tagmasterTests`, run once after focused success | 222 tests passed, 0 failures | `ios-repair-full-unit-final.log` |

Full-suite first light held phase now has 13,302 fill pixels and 4.57 contrast; dark has 14,737 and 6.49. The inherited activation test still immediately precedes the rendered lifecycle test.

Used `iOS/iOS.xcworkspace`, scheme `tagmaster`, `/tmp/tm-dd`, parallel testing disabled, phone `C8B74E44-94F7-4CED-A47F-DFF98E34237B`. Existing backups matched both simulator app bundles, private data and appearance/content-size/motion settings before testing. Afterward `restore-ios.sh` restored both devices; stopped/running app and data comparisons are empty and settings match. Only container-manager metadata and running SplashBoard snapshots are excluded. Evidence: `restore-ios-integration-repair.log`, both `IOS_RESTORE_VERIFIED` markers, and retained per-device diff files.

No Android, other iOS, shared-library, layout-helper, artwork or configuration edits in this repair. No further visual review, live rating/share/account action, reset, commit or push. This pass adds iPhone simulator unit evidence, not a new hardware, iPad or Android verification claim. Lens tools were unavailable; Xcode compilation and native behavioral tests supplied source validation. Scoped `git diff --check` passes.

## Coordinator final review and validation

Reviewed paired ordinary Summary/Details and large-text captures, then corrected Android action contents: `CenteredPitchPipeButton` keeps icon and note text centered as one group; Material Sheet Music uses text-start icon gravity. The new native-font regression covers multiple widths and late-bound note strings, with unchanged padding/geometry during playback. Fixed-size TextView can skip measurement when text changes, so the local subclass explicitly requests layout. Shared pitch code is untouched.

Final Android full unit run passed **302/302**. Production/test APK builds and two native composition/pitch tests passed. The final Android confirmation screenshots are `tagmaster-detail-composition-android-final-{summary,details,missing}.jpg`; the Summary image supersedes the earlier Android icon/text alignment in paired captures. The layout itself otherwise matches those comparisons. Logs: `/tmp/tm-detail-composition/android-final-main-build.log` and `android-main-native.log`.

The coordinator reproduced the iOS sequence-dependent pitch failure in a complete run, then commissioned the ownership repair documented above. Final iOS full unit suite passed **222/222**, including both new ownership regressions. No assertion was waived. Existing native geometry/navigation checks and app/data restoration are recorded in the report and repair logs.

Final scoped layout detector returned `[]`; native coverage remains unproven, so actual geometry/raster/native tests carry the verification. Both generated-artwork checks and source whitespace checks pass. No navigation, shared artwork, compact-footer, floating Android sheet surface, account/persistence or shared-library changes.
