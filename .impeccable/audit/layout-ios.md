# Tag Master iOS layout pass

## Native-tab follow-up

The later user decision supersedes this report's iOS full-width/custom-tab claims. Browse and Detail now use a visible `UITabBarController`, with four bottom destinations and UIKit-owned Liquid Glass, selection lens, margins and typography. Android remains full width. See [native-ios-glass-tabs.md](native-ios-glass-tabs.md) for the scoped changes, final native journeys, screenshots and restoration evidence. All four production layout-repair files listed below remain byte-identical to the handoff; only their test hooks changed where required by native containment. Earlier full-width measurements and screenshots are historical, not current iOS acceptance targets.

## Handoff status

I1, M1 and I2 are preserved and their focused checks pass. All five handed-off failures now have passing targeted checks, with production fixes only for the sheet key and empty Teachable layout. The historical broad run was not rerun; this is not an all-screens/all-combinations certification. Main review is still required. No commit, push or additional aesthetic review was performed.

The final production diff is limited to:

- `iOS/tagmaster/tagmaster/DPTagDetailController.m`
- `iOS/tagmaster/tagmaster/DPTagPageControllerBase.m`
- `iOS/tagmaster/tagmaster/DPTagSummaryController.m`
- `iOS/tagmaster/tagmaster/DPTeachableTagsController.m`

The repair adds changes only to Summary and Teachable; the first two primary patches are byte-for-byte unchanged. Tests remain in the registered `tagmasterTests/TagmasterAppLogicTests.m` and `tagmasterUITests/TagDetailUITests.swift`. The local Browse button has accessibility identifier `teachable.browse`. No project entry, configuration, signing or shared-library change is required.

## Changes and acceptance ledger

### I1: adaptive Details pairs, fixed

Replaced only this controller's grid with local metadata row stacks. Each row measures its caption and value at the current text size. Fitting rows share the normal caption width. A pair can use its own shorter caption width, or put the caption above the value when it cannot leave useful value space. A long Last Refreshed heading no longer controls the Tag ID value width. Horizontal pairs use 8pt internally and 4pt between rows; stacked pairs use 4pt internally and 16pt after the pair. Title separation is 8pt. The existing 16pt readable-content edges are unchanged.

The local body label invalidates its height when its width changes. Optional rows hide together. Required field ordering, captions, URLs, button link/static-text semantics, date formatting and model values are unchanged. Each pair explicitly exposes caption before value to accessibility.

Before editing, the AX5 393pt probe measured ID `1809` at **40.3pt wide and 249.3pt tall**, versus a 121.4pt whole lexical width. Afterward it measured **223pt wide and 63.3pt tall**, one text line.

`testDetailsPairsFitResizeOmitAndKeepReadingOrder` passed:

- 320, 393 and 834pt controller widths, with wide→narrow→wide reuse.
- Default, XXXL and AX5. Fifteen width/category iterations.
- Long contributor names, dates, title, numeric values and linked/unlinked names.
- All visible values at least 44pt wide; labels and buttons contain their measured text height.
- No ambiguous pair layout, values within content edges, ordered nonoverlapping rows.
- Default layouts remain horizontal. Numeric ID remains one line at all sizes.
- Last line scrolls into the viewport. Five optional rows collapse and restore without stale content height.
- Caption/value accessibility arrays remain in reading order.

Native actual iPad split-context Details and phone portrait/landscape also ran. There were no `Unable to simultaneously satisfy` messages in the main phone batch. This is not VoiceOver-on-device certification.

### M1: width-based filter fallback, fixed after reproduction

Before editing, actual Search at 320pt gave Parts **256pt for seven segments**, with segments still visible. Both assertions failed as expected: seven separate 44pt targets require 308pt.

`TMFilterControl` now measures its stable wrapper bounds, scaled title widths and a 44pt floor per segment. Accessibility categories retain the existing native menu. Narrow non-accessibility layouts also use that menu. Visible segments receive measured widths with remaining space distributed across them. Zero-width initial/hidden controls do not determine the mode. Hidden properties change only when the mode changes.

All five Search and four Settings consumers passed wrapper-boundary checks at 256, 329 and 700pt, including shrink/grow repetition and AX5. Tests preserve titles, selected indices, menu ordering and existing `search.*` / `random.*` defaults. Visible configured segment widths remain at least 44pt; repeated same-width updates keep the same mode. Existing index-to-menu mapping regression passed too. Actual iPad sidebar Search rendered the menu fallback.

### I2: one Summary section break, fixed

Added one removable intrinsic 16pt spacer immediately before the first Lyrics/Notes row. Neither prose field leaves no spacer. It is not added to each field or to the materials button. Existing title/key/rating composition and busy accessory slot remain unchanged.

Eight sheet/prose combinations at default and AX5 passed. The first visible prose heading begins at the spacer's bottom, the spacer disappears when neither field exists, and animating the sheet loader does not change button bounds. Both existing `TMSummaryLayoutRegressionTests` pass, including long AX5 text, repeated page switches and hide/restore row-height checks.

## Coverage matrix

Status describes the evidence below, not every device/state combination. Capture names below omit the common `.impeccable/review/tagmaster-layout-after-ios-` prefix. `native` means XCUITest screen capture of the running app; other images are hosted production-controller fixtures. Images are full screens or controller viewports, not contact sheets. Bounds assertions supplement images where whole rows exceed a viewport.

| Requested family | Status and evidence | Limits / remaining findings |
| --- | --- | --- |
| Home, zero favorites | Verified unchanged. Native phone default dark and iPad light AX5; `phone-native-home.jpg`, `ipad-native-home-ax5-confirmed.jpg`. Wordmark, action order, empty copy and credits remain. | No live favorites changed for captures. |
| Home, one favorite | Verified unchanged in in-memory getter/cache fixture. Automatic row and text bounds checked. Existing footer native-size test passed. | Not every font/orientation combination captured. |
| Home, many favorites | Verified unchanged at default. Thirty IDs supplied through readonly getters; last row reached. `phone-home-many-default.jpg`. | The related Teachable edit/resize check now passes after fixture readiness was corrected; see the repair ledger. |
| Browse Latest/Rating/Downloads/Classic | Verified unchanged. Existing native portrait/landscape journey selected all four modes and checked equal full-width tabs. Existing hosted split/resize/large-text test passed. | Every selected mode's entire dataset was not visually reviewed. No query semantics changed. |
| Search form/options | Fixed M1. Actual native default phone and AX5 sidebar; five consumer checks and existing mapping regression pass. | All seven menu options are mechanically checked, not seven separate tap captures. |
| Search keyboard | Verified unchanged. Final phone AX5 portrait and landscape captures show the actual keyboard and typed `harmony quartet`; Search remains hittable. Actual iPad AX5 portrait/landscape journey passed. | First default phone captures named `search-keyboard` did **not** visibly show a keyboard. They are form evidence only; use `*-ax5-confirmed.jpg` for keyboard evidence. |
| Search/query results | Verified unchanged for long single-result production query fixture; text bounds checked. Browse native results also rendered. | No large result dataset/pagination scroll-position stress. |
| Query loading/pagination | Verified unchanged by existing compact loading, query lifecycle, refresh and retry tests. | No new full-screen pagination capture. The pending-Back failure is resolved as a navigation/retention fixture error below. |
| Query empty/error/retry | Verified unchanged in deterministic fetch-boundary fixtures. Empty/result row counts and full status height checked. Error Retry reaches a 852×393pt AX5 viewport and remains at least 44pt tall. `phone-query-error-landscape-ax5.jpg`. | Retry execution is covered by existing tests, not by a live failing network. |
| Teachable empty | Fixed with a scrollable, accessible table header, shown only for an empty list. Full guidance and Browse bounds pass at 320→834→320pt AX5, including a 568pt-high viewport. Native phone and iPad AX5 Browse taps reach Latest. | The old `phone-teachable-empty-ax5.jpg` predates this repair. Empty→populated→empty transitions are checked with readonly getter fixtures, not live list changes. |
| Teachable populated/reorder | Production cell unchanged. One/thirty-item AX5 rows pass full-label bounds and last-row reach across 320→834→320pt, each with edit on/off/on. Default rows remain dense. | The old failure sampled an uncommitted layout transaction. No manual row refresh remains in the test. No reorder/delete callback or saved-list write was invoked. |
| Summary ordinary/long/missing | Fixed I2, verified other behavior unchanged. `phone-summary-light-default.jpg`, `phone-summary-long-dark-ax5.jpg`; all sixteen sheet/prose combinations and prior cache-height tests pass. | Full long prose is checked by measurement and first/last-line scroll reach, not one screenshot. |
| Details responsive | Fixed I1. `phone-details-light-default.jpg`, `phone-details-dark-ax5.jpg`, `phone-details-final-row-dark-ax5.jpg`, actual `ipad-native-details-landscape.jpg`. | Hardware/VoiceOver traversal and localization beyond the fixture are not tested. |
| Tracks empty/populated/long notes | Verified unchanged. Actual controller fixtures have zero or six part choices including Other 1, long recording notes, measured header text, and a reachable final part. `phone-tracks-empty-ax5.jpg`, `phone-tracks-long-ax5.jpg`. | No new real playback, seek/balance or external-player capture. Existing controlled readiness/failure/retry and pitch tests pass. |
| Videos empty/populated/long/missing | Verified unchanged by actual controller rows and measured text bounds. Teaching video plus long user attribution and missing metadata supplied. Thumbnail fetch boundary returns nil; no YouTube action. `phone-videos-empty-ax5.jpg`, `phone-videos-long-ax5.jpg`. | Long AX5 attribution wraps heavily beside the unchanged 80×60 thumbnail. Full row text is not simultaneously visible. Native table empty-footer scaling was not separately certified. |
| Sheet preview/key/rotation | Fixed app-local key fitting. Native phone portrait/landscape target is 93×44pt; iPad AX5 is 107.5×44pt. Both hosted windows pass every sampled point in a 44×44pt target. Actual read-only tag 1809 score renders in four targeted native JPEGs listed below. | Hosted PDF tests still certify chrome/hits, not the remote document surface. Native images show visible score portions; the iPad sidebar overlays part of it. Full-document pan/zoom is not certified. No app cropping/stretching or Quick Look linkage changes. |
| Rating chooser | Production unchanged. Native phone and iPad AX5 checks reach the ≥44pt `1 star` action and dismiss without submitting. | The fixture now scrolls the actions list, not the message scroll view, and does not require every action to be visible simultaneously. Original iPad popover anchor is unchanged. |
| Open Tag alert | Verified unchanged on native phone/iPad AX5, including visible entry and Open/Cancel targets; `*-native-open-tag-ax5-confirmed.jpg`. Open/Cancel and read-only tag 1809 navigation exercised. | Invalid-ID/error variants not newly captured. |
| Settings four filters/account entry | M1 fixed; account/list sections verified unchanged. `phone-settings-narrow-default.jpg`, `ipad-native-settings-ax5-confirmed.jpg`. | No clearing lists, signing out, cache/theme or private-build configuration changes. iOS does not expose the Android cache/theme sections here. |
| App-owned login wrapper | Verified unchanged on actual iPad AX5: opened signed-out Log In, captured, closed with native `Close`; `ipad-native-login-entry-ax5-confirmed.jpg`. | Initial test incorrectly expected `Cancel`. Third-party FirebaseUI contents, truncated AX5 sign-in heading and lower provider controls are outside source scope and not certified. No credentials/providers submitted. |
| Private-build Settings | Not rendered. Normal installed build has no private section. Source registration unchanged. | No signing/config changes to force it. |
| Changelog | Not rendered. No iOS counterpart established in the scoped Settings implementation. | No invented cross-platform UI. |

## Verification commands and actual outcomes

Every test invocation used `/tmp/tm-layout-ios-test.sh`, with `set -euo pipefail`, a true exit-status file, raw log, result bundle, and EXIT/INT/TERM restoration trap. Base command:

```sh
xcodebuild -workspace iOS/iOS.xcworkspace -scheme tagmaster \
  -derivedDataPath /tmp/tm-dd \
  -destination 'platform=iOS Simulator,id=<explicit-UDID>' \
  -parallel-testing-enabled NO \
  -resultBundlePath /tmp/tm-layout-ios-<run>.xcresult \
  <only-testing selectors> test
```

Phone: `C8B74E44-94F7-4CED-A47F-DFF98E34237B`. iPad: `55A9555A-9714-4FE0-8F0D-1035652526F9`. Both iOS 26.5 simulators. `TM_LAYOUT_SIZE=accessibility-extra-extra-extra-large` selects native AX5 for the final phone and both iPad journeys, with restoration on exit. Controller fixtures use trait overrides, not persisted data changes.

| Run, `/tmp/tm-layout-ios-` prefix | Selected work | Outcome |
| --- | --- | --- |
| `before` | Two new I1/M1 native geometry probes before production edits | Exit 65; two failed tests, four assertions. Both suspected defects reproduced. |
| `fit` | Layout matrix plus prior Summary tests | Exit 65. Six methods logged passes; Settings fixture crashed because hosted XCTest deliberately skips Firebase setup. Fixed using a test-only signed-out boundary, not Auth configuration. |
| `batch-phone` | First broad command | Exit 65 at link. Direct `QLPreviewController.class` referenced a framework not linked by the test target. Replaced with the existing tests' `NSClassFromString` pattern; no project linkage edit. |
| `native-phone` | TMLayoutRegressionTests, TMPolishRegressionTests, TMLoadingRegressionTests, TMFooterPitchTests, TMSummaryLayoutRegressionTests; native full-width journey and new keyboard/alert journey | Exit 65. **65 unit tests, ten failing assertions across three cases; two UI tests, one failed.** All 24 Footer/Pitch, 15 Polish and two Summary tests passed. Thirteen of fourteen Loading tests passed. Eight of ten new Layout tests passed. Native full-width journey passed; new journey reached login but expected Cancel instead of Close. |
| `native-ipad` | Two hosted layout/filter methods and native AX5 split journey | Exit 65. Two unit methods passed; UI failed only on incorrect Cancel name. |
| `confirmation-phone` | Ten Layout tests, two Summary tests, three impacted footer/key tests, Browse/split and pending-Back loading cases, AX5 native journey | Exit 65. **17 unit tests, ten failing assertions across three cases; one UI test failed.** Details, filters, Summary, media/query and Browse/split pass. One requested selector was misspelled and selected zero Polish tests; its correctly named existing self-sizing test had passed in `native-phone`. |
| `confirmation-ipad` | Two hosted methods and corrected native AX5 split/keyboard/alert/login journey | **Exit 0. Two unit methods and one UI journey passed.** |

The first handoff ended after that correction/confirmation cycle, with ineffective label/key experiments and the unsupported hosted screenshot call removed. The repair below used only named failures and impacted tests, not another broad suite or aesthetic matrix. The original I1/M1/I2 checks passed again.

The standalone Swift indexer reported `No such module XCTest`. The configured Xcode target compiled and executed `TagDetailUITests.swift`; this was not a target module failure. LSP/ast-grep discovery returned no usable tool in this worker; compiler/XCTest were the fallback. Existing deprecation/linker warnings remain in raw logs. `git diff --check -- iOS/tagmaster` passes. Both edited test files remain mechanically present in their existing Sources build phases.

## Outstanding failures for Main

No named failure remains unresolved in the targeted checks. Historical exit-65 results remain above and below; no assertions were waived or expected failures added. Remaining coverage limits follow this repair ledger.

1. **Teachable edit-row: fixture readiness, resolved.** The unchanged production baseline reproduced metadata 178.3pt versus 416.7pt required and availability labels 20pt versus 102.3/153.3pt. Waiting for a committed Core Animation layout transaction replaces the fixed 80ms wait. With no label invalidation, row refresh or cell source change, one/thirty-item rows now contain every measured label and the last row remains reachable after width/edit changes. At 320pt AX5 the phone edit row is 1268.7pt high with 186.7pt content width; iPad-hosted equivalent is 1269.5/187.5pt. Default rows stay below the test's 260pt dense-row bound. Existing self-sizing/long-title/aka/spoken-availability regression also passes.
2. **Sheet key: production fitting defect, resolved.** Native phone initially reported a 93×30.7pt key. Window hit testing rejected the outer edges of a 44pt target. Intrinsic size, frame, constraints and local hit expansion alone did not overcome iOS 26 shared-background fitting; those failed diagnostic results were retained. `TMSheetKeyView` now owns the public custom-view sizing contract, and this already outlined control opts out of the shared bar-item background via `hidesSharedBackground` on iOS 26. The navigation bar stays 54pt high; no navigation color or shared pitch code changes. Final window hit tests sample all 1,936 points in the 44pt square, with native phone/iPad rotation checks too. The ineffective expanded accessibility-frame/hit-test overrides were removed. Existing note activation, blue pressed/idle appearance, cancel and timed accessibility playback pass; explicit release-inside/outside assertions were added and pass. Quick Look adds Share/Markup items on iPad, so the fixture retains the actual key item and awaits attachment/current preview rather than assuming it stays rightmost.
3. **Pending Back: fixture lifecycle/retention, resolved.** Against unchanged `DPTagViewController.m`, the old test reproduced four continuing animations and retention. The fixture now mounts the previous screen, awaits native `didShow`, pushes, awaits completed appearance, and observes the real pop completion. The readiness predicate captures the target weakly. Stop/deallocation assertions and held-request completion/busy balancing all pass. Pending success/single-announcement and motion/background/detachment/completion tests pass separately. Quartet geometry, timing and production lifecycle code are unchanged; no animations were disabled.
4. **Phone AX5 rating: fixture scroll/visibility assumptions, resolved.** The native sheet has separate message and action scroll views. The first diagnostic scrolled the message, not the actions. The corrected selector reaches `1 star`, checks its ≥44pt frame, then scrolls to Cancel or dismisses outside a native popover that omits Cancel. Phone and iPad AX5 checks pass, without rating submission or popover-anchor changes. The existing larger journey uses this helper; that full keyboard/settings/login tour was not repeated.
5. **Empty Teachable: production scroll/accessibility defect, resolved.** The original native AX5 screen drew the guidance but omitted its contents from the accessibility tree. At narrow widths Browse also lacked a proven scroll escape. `DPTeachableTagsController.m` now uses a self-fitting table header with the same guidance and Browse action, only when the real list is empty. At 320pt AX5/568pt viewport, content is 1241pt high and the full 256×76pt Browse target scrolls into view and receives window hits. Width reuse and empty/populated transitions pass. Native phone and iPad AX5 tap Browse and reach Latest. The iPad test explicitly scrolls the Teachable table instead of the unrelated split-view pane.

### Repair commands and outcomes

All runs use the base workspace command above, `/tmp/tm-dd`, SDK 26.5, explicit simulator IDs, `-parallel-testing-enabled NO`, a 600-second command timeout, and the existing restoration wrapper. Selectors use `tagmasterTests/<class>/<method>` and `tagmasterUITests/TagMasterPolishUITests/<method>`. Logs, exact command lines, status files and result bundles remain under `/tmp/tm-layout-ios-<run>*`.

| Run | Actual result |
| --- | --- |
| `repair-baseline` | Exit 65 before execution: incorrect target name `TagmasterAppLogicTests`. Corrected to registered `tagmasterTests`; no zero-test pass claimed. |
| `repair-before` | Exit 65: exactly the three handed-off unit cases reproduced ten assertions, before production edits. |
| `repair-diagnose` | Exit 65: corrected lifecycle passes; committed-layout row diagnostic passes; two key-hit assertions remain. Diagnostic row refresh was subsequently removed. |
| `repair-native-reach`, `repair-native-sheet-before` | Exit 65: native empty accessibility/incorrect rating-scroll selector, then native 30.7pt sheet target reproduced. The latter already rendered a real score. |
| `repair-fit` | Exit 65: empty scroll and all row reuse checks pass; intrinsic-only key still fails. |
| `repair-targeted` | Exit 65: sheet playback and native phone Browse pass; key fitting and an incorrect phone Cancel/idiom assumption fail. |
| `repair-hit-routing` | Exit 65: native phone AX5 rating reach/cancel passes; key edges remain rejected. |
| `repair-key-owner`, `repair-key-frame`, `repair-key-constraint` | Each exit 65: one changed diagnostic/key-sizing case, two remaining edge-hit failures. No unchanged broad suites. |
| `repair-key-native-fitting` | Exit 0: one hosted key test, both orientations/resized widths hit all target points; bar height remains 54pt. |
| `repair-final-phone` | Exit 0: nine unit methods and one native sheet portrait/landscape method. Includes I1/M1/I2, pending Back, pending success, motion policy, sheet request failure/retry, cell sizing/spoken availability, and sheet playback/release paths. |
| `repair-final-ipad` | Exit 65: row geometry and native rating/sheet methods pass. Hosted key assumed rightmost item after Quick Look inserted tools; native Browse test swiped the wrong pane. These were fixture errors, not waived cases. |
| `repair-ipad-fixtures` | Exit 0: the two failed iPad cases pass after item-ownership/readiness and pane-selector corrections. |
| `repair-phone-fixtures` | Exit 0: those final readiness/selector changes also pass on phone AX5. |

`repair-final-phone` and later test invocations built the production app and both test targets successfully. LSP/ast-grep discovery again exposed no usable registered tools, so Xcode compilation and XCTest supplied diagnostics. No `Unable to simultaneously satisfy` messages occur in the final phone/iPad/fixture logs. Sources registrations for Summary, Teachable and both test files are mechanically present. `DPTagCell.m`, `DPTagViewController.m`, the Xcode project and entitlements have no diff. The Details/filter patch matches the retained pre-repair patch exactly; the preserved Summary prose-gap acceptance passes.

### Remaining coverage limits

The historical 65-test batch and full native keyboard/settings/login journey were not rerun. Targeted checks resolve their named failures, not every possible interaction. Physical-device VoiceOver traversal, full Quick Look pan/zoom, private-build Settings and the unestablished iOS changelog remain uncertified. No new claim is made for previously unrendered media/playback screens.

Four targeted native JPEGs are retained under `.impeccable/review/tagmaster-layout-repair-ios-<phone|ipad>-sheet-<portrait|landscape>.jpg`. Each is ≤800px, exported from authorized XCUITest attachments and read individually. They show an actual score and the key after rotation, not the entire document at once. The old hosted chrome-only images remain explicitly limited evidence.

## Data preservation and scope

Original simulator apps, complete private containers, app inventories and device preference copies are retained at `/tmp/tm-layout-ios-preserved`. Backups were copied while stopped and their relative-path SHA-256 manifests compared to the originals **before any test install**: phone 94 private files, iPad 62. No originals were deleted. iOS app documents, cache and preferences are included; no app-group entitlement was found in scoped entitlements. No uninstall or device reset was used.

Every run restored the original app bundle and private container while stopped and byte-compared them. Final post-boot private comparisons also pass, excluding only OS-owned SplashBoard snapshots and container-manager metadata. Final phone is dark/large, iPad light/large, matching the recorded settings. Motion/transparency preferences were not changed. Backup scripts, manifests, comparisons and raw logs remain in `/tmp`, not the repository.

Captures use readonly getter/cache/fetch boundaries for synthetic favorites, teachables, media and query states. Native navigation opens existing tag 1809 read-only. Existing regression tests exercise their own local/mock data. No real rating, share destination, provider sign-in, user-list reorder or cloud write was submitted for capture.

Historical layout-pass contracts included four equal full-width custom tabs. That iOS-only target is superseded by `native-ios-glass-tabs.md`; UIKit now owns capsule margins, type and selection. Still preserved: shared vectors/palette/geometry/motion, charcoal production navigation, blue actions/sustained pitch feedback, green availability, compact footer and link/date/model semantics. The sole native-background exception is the custom sheet key's iOS 26 shared-background opt-out described above; the bar and footer colors are unchanged. Scoped pitch and pending-Back/motion tests pass. No shared library, Pitch Perfect, third-party auth, backend, persistence, API, signing or configuration edits. The harness reported concurrent pi-lens autofixes to the other worker's Android test file; this repair did not manually edit or revert Android files.

Before the repair's first install, both original app/private archives were rehashed and compared with their original manifests. `/tmp/tm-layout-ios-verify-restored.sh` then revalidated retained archives, restored app bytes, post-boot private bytes with only the documented OS exclusions, and exact appearance/content-size settings for both devices. Final evidence is `/tmp/tm-layout-ios-repair-preservation.log`, exit 0. iOS Documents and other app-owned files are inside the preserved private containers; there is no scoped app-group entitlement. No credential contents were read, originals deleted, app uninstalled or device reset. No simulator motion/transparency settings changed.

The 53 prior designated iOS JPEGs remain, with four targeted native sheet JPEGs added. All four additions have maximum dimension 800px. This report does not claim every image certifies all text. Main owns the final mechanical scan and commit decision.
