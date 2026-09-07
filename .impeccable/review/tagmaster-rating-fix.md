# Tag Master rating-row remainder

Status: final correction verified by Main. The independent reviewer verdict remains separate. See the closing evidence below.

## Scope

Verified root `/Users/depoll/.local/share/pi-worktrees/20260907005637/tm-refine-worktree-20260907`.

Changed only:

- `iOS/tagmaster/tagmaster/DPTagSummaryController.m`
- `iOS/tagmaster/tagmasterTests/TMRefreshTests.swift`
- `iOS/tagmaster/tagmasterUITests/TMDeskUITests.swift`
- This report.

No attribution changes, unrelated tests, commits, delegates, or screen polishing. Prior report and normal-width screenshots remain untouched.

## Implementation and acceptance checks

Rating group switches to a filled vertical native stack at accessibility categories, putting Rate below stars and numeric text. Standard categories retain the horizontal row. Fonts use uncapped preferred Title3 and Footnote sizes with controller traits. Numeric rating uses native UILabel. Existing minimum 44pt Rate height and action remain.

Added `testSummaryRatingFitsNarrowAccessibilityXXXLWidths`: 320pt and 390pt in light/dark traits, exact five stars and rating text, intrinsic text width, fitted height, containment, no collision, minimum target size, action binding, and return to horizontal at standard text size.

Added Summary-only `testReviewSummaryRatingLightLarge` and `testReviewSummaryRatingDarkLarge` using the existing review-tag opener and capture helper. They scroll to the rating group, check reachability and geometry, and open/cancel the native Rating sheet without submitting a rating. Only the dark method requests a screenshot. Existing attribution probes are unchanged and were not run. Both Swift files remain registered in their existing Xcode Sources phases. Swift edit diagnostics were clean.

## Single focused build/test attempt

From `iOS/`:

```sh
xcodebuild -workspace iOS.xcworkspace -scheme tagmaster -configuration Debug \
  -destination 'platform=iOS Simulator,id=C8B74E44-94F7-4CED-A47F-DFF98E34237B' \
  -derivedDataPath /tmp/tm-dd -parallel-testing-enabled NO \
  -resultBundlePath /tmp/tm-rating-final.xcresult \
  -only-testing:tagmasterTests/TMRefreshTests/testSummaryRatingFitsNarrowAccessibilityXXXLWidths \
  -only-testing:tagmasterUITests/TMDeskUITests/testReviewSummaryRatingLightLarge \
  -only-testing:tagmasterUITests/TMDeskUITests/testReviewSummaryRatingDarkLarge \
  test > /tmp/tm-rating-final.log 2>&1
```

Shell timeout 1200 seconds. App and tests compiled. All three tests failed. Unit test reported four width failures, about 134pt available versus 281pt intrinsic text width. Both UI tests stopped at the stars-reachability assertion before capture or Rate activation.

Inspection found that Rate's existing required horizontal hugging, combined with vertical fill, forced the whole Summary to its intrinsic button width. Corrected the accessibility branch to lower that hugging priority, preserving required hugging in standard layout. This correction has NOT been rebuilt or tested, to honor the one-invocation limit. It must not be presented as passing evidence.

## Capture evidence and remaining work

Exported failed-run attachments with:

```sh
xcrun xcresulttool export attachments --path /tmp/tm-rating-final.xcresult \
  --output-path /tmp/tm-rating-final-attachments
```

Manifest and screen recordings are in that directory. Inspected one 900px JPEG frame at `/tmp/tm-rating-failure.jpg`; it shows the compressed Summary. The nominal dark probe also rendered light: launch argument `-AppleInterfaceStyle Dark` alone did not set simulator appearance. A future dark run must first use `xcrun simctl ui C8B74E44-94F7-4CED-A47F-DFF98E34237B appearance dark`, as the prior report did.

`phone-summary-dark-large-iOS.png` and `fix-previews/phone-summary-dark-large-iOS.jpg` were NOT refreshed. Keeping the old evidence is preferable to replacing it with a failed or mislabeled capture. The old bounded preview was inspected; no final-code image exists yet.

Remaining acceptance gates: build/test the hugging correction; prove full-width five-star/rating rendering and reachable Rate in the native probes; export the passing dark-large Summary attachment and refresh its bounded preview. The implementing subagent stopped here. Main then completed the focused verification recorded below.
