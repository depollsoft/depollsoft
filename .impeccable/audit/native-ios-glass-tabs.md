# Native iOS glass tabs

## Result and decision

The custom `UIButton`/`UIStackView` strip is gone. A visible `UITabBarController` now owns the tab bar, items, selection, accessibility and material. UIKit supplies Liquid Glass on iOS 26 and the native fallback on earlier releases. Four destinations remain at the bottom of Browse and Detail. iOS accepts system capsule margins, selection-lens geometry and typography instead of the old equal edge-to-edge targets. Android remains full width and was not edited.

The pending layout repairs are preserved. No commit or push. Main owns review and publication.

## Acceptance ledger

- [x] Real native tab bar and four expected titles/icons. Actual XCUITest taps change the native selected accessibility state, exactly one item at a time.
- [x] Bottom placement and separate 44pt activation targets on phone and both iPad split columns. Rotation preserves selection. Hosted tests cover return from a pushed screen, all four visible pages, retained native parents and appearance/disappearance.
- [x] Pending/error hides the entire native container. Programmatic selection while pending cannot expose empty fields. Pre-mount success, delayed success, retained refresh, stale completion, retry, pending Back/deallocation and motion-policy checks pass.
- [x] Critical Details/filter/Summary/Teachable/preview-key checks pass. The four prior production repair files remain byte-identical to the handoff.
- [x] Actual app Browse and Detail in phone/iPad light and dark, plus alternate selected items. Sixteen JPEGs retained, all at most 800px.
- [x] Scoped compiler/tests, source registration, diff checks and simulator restoration verified.

## Changed files

- `iOS/tagmaster/tagmaster/TMPageViewController.h/.m`: app-local host for a real contained `UITabBarController`; forwards `viewControllers` and `selectedIndex`; exposes native `tabBar` and `pageTabController`. `rootView` is the whole native container, not a selected page that can become stale or remain hidden after changing tabs.
- `iOS/tagmaster/tagmaster/DPTagQueryViewController.m`: its two parent checks recognize the native tab controller inside the app-local host. Query, persistence and data contracts are unchanged.
- `iOS/tagmaster/tagmasterTests/TagmasterAppLogicTests.m`: replace custom button/font/effect internals and direct native-bar mutations with native selection, visible-page, lifecycle, loading and geometry assertions. Existing layout fixtures remain. The recent pending-Back readiness/weak-reference repair is intact.
- `iOS/tagmaster/tagmasterUITests/TagDetailUITests.swift`: `testNativeGlassTabsInPortraitAndLandscape` replaces the misleading full-width journey name. Tests native tab bars, four items, selected state, hittability, rotation and column containment. Helpers moved to a same-file extension to satisfy the existing class-length check.
- `.impeccable/audit/layout-ios.md`: marks old iOS custom/full-width claims historical.
- This report and `.impeccable/review/tagmaster-ios-native-glass-*.jpg`.

`DPBrowseViewController.h/.m` and `DPTagViewController.h/.m` were read and need no edits. The existing Detail loading state hides `rootView` and the bar, then reveals the same container after data arrives. Fetch generation, busy accounting, artwork, loader geometry, lifecycle gates and action contracts are unchanged.

## Public UIKit wiring

Read the actual installed iPhoneSimulator26.5 SDK headers for `UITabBarController`, `UITabBar`, `UITab` and trait overrides. On iOS 18+, only the native tab container gets compact horizontal size class and `UITabBarControllerModeTabBar` to keep bottom placement. Each content page receives the host's actual horizontal size class; the host, navigation controller and `UISplitViewController` remain adaptive. On 26, `UITabBarMinimizeBehaviorNever` keeps all four destinations available.

The first iPad geometry probe found that split navigation views extend beneath the adjacent column. Pinning the native container's horizontal edges to the host's safe area fixed the bar crossing that column. Its vertical edges remain native, with page safe-area clearance above the bar. Both primary-column Browse and secondary-column Detail now pass actual-app hit tests.

No native tab-bar delegate is reassigned. No opaque appearance, background image, selection image, font override, custom tab material, private UIKit traversal or compatibility flag is added. System blue is the bar's interactive tint. Navigation styling and toolbar fonts outside the tab bar are unchanged, including the pre-existing iPad secondary-navigation treatment.

Existing Sources registrations for `TMPageViewController.m` and `DPTagQueryViewController.m`, and the two test files, remain in place. No new source file, bridge, build setting or project registration was required.

## Tests and failures

All commands use `iOS/iOS.xcworkspace`, scheme `tagmaster`, derived data `/tmp/tm-dd`, SDK 26.5, explicit simulator UUIDs, `-parallel-testing-enabled NO`, and the existing `/tmp/tm-layout-ios-test.sh` restoration wrapper. Shell commands use `set -euo pipefail`; nonzero results are retained. Raw logs, result bundles and status files use `/tmp/tm-layout-ios-native-tabs-<run>`.

| Run | Actual result |
| --- | --- |
| `core` | Exit 65. Eleven unit methods and one UI journey; nine methods pass, three fail. Native asynchronous selection exposed two instantaneous custom-container assumptions. The UI slot test rejected the native lens's enlarged accessibility frame. Loading, cache, retry, refresh, stale completions and pending Back already pass. |
| `phone-dark` | Exit 65. Ten unit methods and three UI journeys; nine pass, four fail. Native phone journey, Teachable Browse and sheet key pass. Five preserved layout checks and Browse/rotation pass. Remaining failures are final-selection/removal timing and Summary fixtures directly calling `setSelectedItem:` on a controller-owned bar. |
| `ipad-light` | Exit 65. Eight unit methods and one UI journey; six pass, three fail. Native material, pending success/Back, Details and both migrated Summary cases pass. Split-column bounds/hittability and empty-destination removal assertions fail. |
| `ipad-column` | Exit 65. Two unit methods and one UI journey; two pass, one fails. Safe-area correction makes real iPad tabs and hosted split/rotation pass. An attempted nil native selection is rejected by UIKit; that experiment was removed. |
| `phone-light` | Exit 65. Four unit methods and one UI journey; four pass, one fails. Material, both Summary cases and light native journey pass. Last fixture assumed UIKit chooses index zero and emits the old custom detachment callback when removing the selected destination. |
| `ipad-dark` | Exit 0. One lifecycle method and one real native journey pass. Dark AX5 captures; removed page loses its parent and visible view, and UIKit selects a surviving destination. |
| `phone-final` | Exit 0. Seven unit methods and one real native journey pass. Dark AX5, lifecycle, pending selection/Back, retained refresh, sheet failure/retry, filter mapping and sheet-key playback/release checks. |

Latest results across the scoped runs: **22 distinct unit methods and three distinct UI methods pass; no named failure remains unresolved.** This is not a claim that every method ran on both devices. All four device/theme native-tab journeys pass. No tests were skipped or marked expected failure; no broad 65-case rerun was performed.

Native selection tests await actual page visibility or appearance completion rather than stopping animations. UIKit may enlarge the selected item's AX frame into its neighbor's frame. The real journey therefore checks nonoverlapping 44pt activation areas around item centers and taps each native item, rather than forcing custom equal rectangular slots. Removing a selected destination is tested with surviving destinations, checking actual disappearance, parent removal and visible replacement rather than old custom-only child counts or a forced replacement index.

Preserved critical checks include Details pair resize/omission/reading order, all filter widths/mapping, the conditional Summary prose break, zero/one/many Teachable layout and reorder geometry, both Summary page-switch regressions, hosted and native preview-key bounds, and native empty Teachable Browse. No `Unable to simultaneously satisfy` message appears in any of the seven logs.

No usable lens/LSP tools were exposed by discovery. Xcode compiled the app and both test targets. A separate `clang -fsyntax-only -fobjc-arc -fmodules -target arm64-apple-ios17.0-simulator -Werror=unguarded-availability` check of `TMPageViewController.m` passes with the installed SDK; log `/tmp/tm-native-tabs-ios17-compile.log`. Only iOS 26.5 is installed, so the iOS 17–25 native fallback is compile-checked, not runtime-certified.

## Visual review and screenshot paths

One combined review of the capture batch compared the native phone/iPad light/dark tabs with `tagmaster-ios-glass-tabs-phone-light-summary.jpg` and the earlier custom iPad Details capture. The new bar has the system inset capsule, native selection lens and native type scale. It no longer has the old outlined rectangular custom selection or oversized wrapped tab labels. No visual correction was needed after this review.

All files below are under `.impeccable/review/` with prefix `tagmaster-ios-native-glass-` and suffix `.jpg`:

| Device/theme | Files between prefix and suffix |
| --- | --- |
| Phone light, large | `phone-detail-light-large`, `phone-detail-details-light-large`, `phone-browse-light-large`, `phone-browse-classic-light-large` |
| iPad light, large | `ipad-detail-light-large`, `ipad-detail-details-light-large`, `ipad-browse-light-large`, `ipad-browse-classic-light-large` |
| Phone dark, AX5 | `phone-detail-dark-ax5`, `phone-detail-details-dark-ax5`, `phone-browse-dark-ax5`, `phone-browse-classic-dark-ax5` |
| iPad dark, AX5 | `ipad-detail-dark-ax5`, `ipad-detail-details-dark-ax5`, `ipad-browse-dark-ax5`, `ipad-browse-classic-dark-ax5` |

`detail` selects Summary; `detail-details` selects Details; `browse` selects Latest; `browse-classic` selects Classic. The iPad light Classic capture caught its real pending query; use the adjacent loaded Latest capture for populated Browse evidence. Native tests also select Tracks, Videos, Rating and Downloads. Review opened representative Browse and Detail images for both devices and themes individually through Pi read, without contact sheets or reconstructed UI. Source attachment manifests remain under `/tmp/tm-native-tabs-{phone-light,ipad-column,ipad-dark,phone-final}-export`.

## Preservation and restoration

Before any installation, revalidated the retained original apps/private archives and restored-device bytes for both simulators. Saved the pending diff, modified-file SHA-256 list and both test snapshots under `/tmp/tm-native-tabs-baseline`. `/tmp/tm-native-tabs-preservation-before.log` exits 0. Originals remain at `/tmp/tm-layout-ios-preserved`, including inventories, device preference copies and complete private containers, 94 phone files and 62 iPad files. Prior scoped entitlement inspection found no app group. No originals were deleted, device reset or app uninstalled.

Every test invocation restored the original app and private container while stopped and compared their complete hashes. Final `/tmp/tm-native-tabs-preservation-final.log` exits 0 and records the current app/data paths, post-boot byte comparison and restored settings. Only OS-owned SplashBoard snapshots and container-manager metadata are excluded from the post-boot private comparison. The original archive hashes are rechecked without exclusions.

- iPhone `C8B74E44-94F7-4CED-A47F-DFF98E34237B`: restored **dark / large**.
- iPad `55A9555A-9714-4FE0-8F0D-1035652526F9`: restored **light / large**.

No motion or transparency preference was changed. The pending Details, filter/base, Summary and Teachable production files compare byte-identical to the starting manifest. All five already-modified Android files also compare unchanged. Only the two intentionally migrated test files differ from that pending-file manifest. The app/query/tab source changes were previously clean files. `git diff --check` passes.

No live favorite/rating/share/account mutations were submitted. UI journeys read tag 1809, browse, rotate and return; existing regression tests use their mock boundaries. No shared library, signing, configuration, palette, art, loader geometry or account contract was edited.

## Limits

Simulator evidence only, not physical-device VoiceOver or gesture/GPU certification. Native accessibility policies now belong to UIKit; app-specific glass-effect swizzle tests are obsolete. The old full-width report and captures remain historical. Unrelated screens and the broad suite were not re-audited. Main must review all pending layout work before publication.

## Coordinator final integration

The first complete final iOS unit run executed 218 tests and found six assertions across three inherited cases. Two instances of the Teachable guidance test still expected the retired UIContentUnavailableConfiguration; those now assert the actual scrollable header, guidance text and wired Browse button. The quartet pending/loaded case exposed stale native content insets after directly changing UITabBar.hidden.

`DPTagViewController.updateLoadingState` now uses `UITabBarController.setTabBarHidden:animated:` on iOS 18+, with the existing native-view fallback on iOS 17, so UIKit updates child safe areas as the bar returns. The exact three failed cases then passed. A second full `tagmasterTests` run passed **218/218**, with zero failures. Logs: `/tmp/tm-layout-ios-main-final-regression-repair.log` and `/tmp/tm-layout-ios-main-native-layout-verified.log`. Both runs used the inspected preservation wrapper and restored original simulator app/data/settings afterward.

This supersedes the earlier statement that DPTagViewController needed no edits. Quartet art/timing, loading/retry state contracts and native glass appearance remain unchanged. Final layout detector output is `[]`; native coverage is unproven, so it supplements rather than replaces the native test/capture evidence. Shared artwork generator checks and source whitespace checks pass.
