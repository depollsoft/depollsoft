# Tag Master iOS review fixes

## Scope

Verified root before work: `/Users/depoll/.local/share/pi-worktrees/20260907005637/tm-refine-worktree-20260907`.

Read `tagmaster-iOS-finish-review.md` and `tagmaster-iOS-report.md`. Addressed only the two recorded findings. Production edits are confined to `iOS/tagmaster/tagmaster/DPTagDetailController.m`. Added tests in `iOS/tagmaster/tagmasterTests/TMRefreshTests.swift` and `iOS/tagmaster/tagmasterUITests/TMDeskUITests.swift`. Other writes are review screenshots, reduced copies, and this report. Existing unrelated worktree changes were left alone. No delegates, commits, storage/sync edits, shared-library edits, or identity changes.

This is implementation evidence for Main to return to the reviewer, not a new review verdict.

## Acceptance ledger

### 1. Stale phone Summary

- Replaced `phone-summary-iOS.png` with final-code native evidence. No Summary or readable-width constraints changed in this fix pass.
- Visually confirmed normal phone content fills the available width with normal margins. All five stars and `3.17 out of 5` appear. `Barbershop` and `Sound the key of Major:G` each fit on one line.
- Added `testPhoneSummaryFillsAvailableWidthAndKeepsRatingAndTypeReadable` at `TMRefreshTests.swift:395`. Passed at 320, 390, and 430pt. It asserts exact stack width minus two 16pt margins, five stars, complete rating text, sufficient label widths, and a single-line pitch title.
- The native phone probe also asserts a full-width pitch action, accounting for its existing internal padding, and single-line Barbershop.
- Final portrait and landscape tablet captures preserve Summary beside Details. Their Summary and Details files are byte-identical within each orientation because selecting Details leaves the already-visible split screen unchanged.

### 2. Disabled attribution

- `DPTagDetailController.m:79-120` routes Posted By, Arranged By, and Sung By through one local renderer.
- No website: a native `UILabel`, semantic `TMTheme.primaryText`, body Dynamic Type, unlimited lines, explicit static-text accessibility, no interaction. No disabled button remains in that value's hierarchy.
- Website present: the existing enabled system-button treatment and URL action remain. Refresh replaces the old value, so removing a URL removes its control and restoring a URL restores the button.
- Used native `UILabel` directly. The inherited body-label factory uses `DPLabel`, whose multiline intrinsic-width override collapsed these leading-aligned values at accessibility sizes. Shared `DPLabel` was not changed.
- `testAttributionWithoutWebsiteIsPrimaryMultilineTextInBothAppearances`, `TMRefreshTests.swift:436`, passed for all three roles. It checks static semantics, no button/link traits, no interaction, semantic color, at least 4.5:1 against resolved system/background/grouped canvases in light and dark, accessibility XXXL font size, and unclipped multiline layout using a long name.
- `testAttributionWebsitesKeepActionsAndRefreshRemovesStaleControls`, `TMRefreshTests.swift:481`, passed. It checks all three URL bindings, enabled controls, `openHyperlink` target/action, scalable multiline titles, URL removal, and URL restoration. It does not launch a browser.
- Native phone/tablet probes assert Bobby Gray, Jr and New Tradition are static text rather than buttons, while Daniel Gillis remains a button. The changed probe also checks arranger geometry. Light and dark/accessibility XXXL Details captures confirm readable names.

## Verification and deviations

**The requested one-build limit was not met.** There was one explicit `build-for-testing` attempt plus three build-capable targeted `test` invocations. `DPTagDetailController.m` compiled twice, first for the initial fix and again for the native-label correction. The other two compile passes changed test bundles only. Later device/state runs used `test-without-building`.

Failures were inspected rather than rerun unchanged:

- Initial build compiled the app but failed to compile the width test because its module could not see `DPPitchPipeButton`. Changed the test to the existing KVC approach.
- First targeted run passed the width regression and phone light capture. Attribution tests exposed the multiline `DPLabel` collapse and a lazy UIKit default-trait assumption. The dark phone probe independently showed missing no-URL names. Replaced only the attribution label factory with `UILabel`; native XCUI assertions cover the button role.
- Corrected attribution unit tests passed. A newly strengthened phone geometry assertion expected 370pt, while the actual pitch button is 366pt because of existing internal padding. Corrected that test-only assertion. The exact stack-width unit check remains unchanged.
- The passing width unit test was not rerun. The phone light test was rerun only after its assertion logic changed. No whole suites or unrelated inventory flows ran.

All commands used `set -o pipefail`, retained full unfiltered output, and had a 1200-second shell timeout. Workspace `iOS/iOS.xcworkspace`, scheme `tagmaster`, Debug, derived data/SPM cache `/tmp/tm-dd`, parallel testing disabled.

Devices:

- Phone: `C8B74E44-94F7-4CED-A47F-DFF98E34237B`, iPhone 17 Pro, iOS 26.5.
- Tablet: `55A9555A-9714-4FE0-8F0D-1035652526F9`, iPad Pro 11-inch, iOS 26.5.

All basenames below are under `/tmp/`, with both `.log` and `.xcresult` files:

| Basename | Result |
| --- | --- |
| `tm-ios-review-fixes-build` | App compiled; new test compile failed as described above. |
| `tm-ios-review-fixes-phone-light` | Width unit and original phone light probe passed; two attribution tests failed. |
| `tm-ios-review-fixes-phone-dark` | Initial dark probe failed on missing arranger static text. |
| `tm-ios-review-fixes-confirm` | Both corrected attribution unit tests passed; new phone inset assertion failed. Final production code compiled here. |
| `tm-ios-review-fixes-phone-final` | Corrected phone light probe passed. Only UI-test logic recompiled. |
| `tm-ios-review-fixes-phone-dark-final` | Phone dark/accessibility XXXL probe passed without building. |
| `tm-ios-review-fixes-tablet-light` | Tablet portrait and landscape probes passed without building. |
| `tm-ios-review-fixes-tablet-dark` | Tablet dark/accessibility XXXL probe passed without building. |

Accumulated passing evidence: three targeted unit tests and five device/state UI executions. No claim of a full-suite run.

Reproduction pattern, from `iOS/`:

```sh
set -o pipefail
xcodebuild -workspace iOS.xcworkspace -scheme tagmaster -configuration Debug \
  -destination 'platform=iOS Simulator,id=DEVICE_ID' \
  -derivedDataPath /tmp/tm-dd -parallel-testing-enabled NO \
  -resultBundlePath /tmp/RESULT.xcresult \
  -only-testing:TARGET/CLASS/METHOD test-without-building > /tmp/RESULT.log 2>&1
```

UI methods in `tagmasterUITests/TMDeskUITests`:

- `testReviewSummaryAndAttributionLight`
- `testReviewTabletLandscapeSummaryAndAttributionLight`
- `testReviewSummaryAndAttributionDarkLarge`

Appearance was explicitly set with `xcrun simctl ui DEVICE_ID appearance light|dark`. Large-text tests launch with `-UIPreferredContentSizeCategoryName UICTContentSizeCategoryAccessibilityXXXL`.

`git diff --check -- iOS/tagmaster` passed. Both test files remain registered in their existing Xcode source phases. Swift edit diagnostics were clean after placing the new tests in an extension. Lens navigation/activation tools were unavailable in this session; Xcode compilation and tests supplied type/runtime checks.

## Refreshed native screenshots

All paths are relative to `.impeccable/review/`:

- `phone-summary-iOS.png`
- `phone-details-iOS.png`
- `tablet-summary-iOS.png`
- `tablet-details-iOS.png`
- `tablet-landscape-summary-iOS.png`
- `tablet-landscape-details-iOS.png`
- `phone-summary-dark-large-iOS.png`
- `phone-details-dark-large-iOS.png`
- `phone-details-attribution-dark-large-iOS.png`, scrolled to the performer
- `tablet-summary-dark-large-iOS.png`
- `tablet-details-dark-large-iOS.png`

Exported with `xcrun xcresulttool export attachments`. Corresponding manifests and original exported attachments are in `/tmp/tm-ios-review-fixes-{phone-final,phone-dark-final,tablet-light,tablet-dark}-attachments/`.

Phone PNGs are 1206 × 2622. Tablet PNGs store 1668 × 2420 with orientation metadata. Both old and newly exported landscape previews were independently inspected and already rendered upright and wide. **No rotation was applied.** No compositing or retouching.

Reduced JPEG copies are in `fix-previews/`, maximum edge 900px, all below 68KB. Read one image per tool call. Refreshed the six corresponding light Summary/Details copies in `reviewer-ios/` so those named supplementary copies are not stale. Tablet Summary/Details duplicates were confirmed by SHA-256.

Requested-file SHA-256:

```text
phone-summary-iOS.png  0fbae563a9eca282b2c4cad6c04b56642d7ef4268f28bf63948437f0cc2221e6
phone-details-iOS.png  c8474b75ca3d04a85ca14807a7ee4ba09c7be074ec8c06a0b8c26efa07e9dc1b
tablet-details-iOS.png 6a075a68b11920c4c958c8e9a20b90a74f21a858f2abe23b3db659d3e24a24a5
```

## Limits for the reviewer

The light phone Summary meets the stale-capture finding's visible acceptance criteria. The supplementary phone Summary at accessibility XXXL still truncates its star row despite occupying full width. This pass does not claim all five stars fit at that extreme setting. No additional Summary redesign was made under the instruction to change constraints only if the container remained collapsed. The dark Details captures and long-name unit regression establish the requested attribution behavior.

Simulator tests do not certify physical VoiceOver gestures or external website loading. No account, storage, sync, media, or broader QA claims were added. Main owns the return to the finish reviewer.
