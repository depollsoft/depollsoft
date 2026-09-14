# iOS footer, pitch and barber pole

Scope: four existing files under `iOS/tagmaster`, this report and 29 task JPEGs. No commits or pushes. Android changes belong to the other agent. Shared libraries, configuration, credentials and other screens were not edited.

## Acceptance ledger and results

| Native Home measurement | Before | After |
| --- | ---: | ---: |
| Phone, 402pt, default | 200pt | 112pt |
| Phone, AX5 | 573.3pt | 430.7pt |
| iPad, 320pt sidebar width, default | 210.5pt | 112pt |
| iPad sidebar width, AX5 | 754pt | 484pt |

- Home has one attribution link, then developer/copyright, Terms and Donate sharing a row. Native stacks wrap whole groups when needed. Configured buttons now actually use Dynamic Type footnote fonts rather than UIKit's overriding body font. Tests verify all four original URLs, link traits, 44pt targets, hit testing, containment, no clipping/overlap and Terms/Donate sharing a default-size row. Original year calculation and business copy remain unchanged. No version label was invented.
- Each Home capture uses one in-memory favorite, Lost. Favorites/cache read boundaries are restored after every test; no favorite was persisted. Invalid light baseline fixtures were discarded because their transparent test window rendered black. Dark baselines remain; final light/dark captures use the correct native window background.
- Summary and sheet Key controls fill blue during actual bound-note playback and restore the outline afterward. Geometry remains unchanged. Native rendered fill, title and icon pixels pass, with 4.57:1 light and 6.49:1 dark contrast for Summary, and 4.57:1 for the sheet fixture. Tests cover held/released/cancelled, disabled accessibility activation, timed completion, toggle, replacement and detach. Shared sound targets/synth were not edited. DPNote writes playback directly to an ivar, so the local button checks its bound state at 30Hz while attached and only restyles on changes.
- Browse/Search query loading now uses a 28x52pt vector barber pole centered in a 68pt full-width footer. Only its clipped red/white/blue stripes translate, linearly over two seconds. Stationary caps/frame never rotate. Native presentation-layer samples moved from 2.86 to 11.54pt on phone and 2.90 to 11.64pt on iPad, with an unchanged frame. Tests cover a real provider request begun before view creation, completion, failure/retry, Reduce Motion, hiding, detach, appearance and app inactivity. The loading label is one stable accessible element. Native pull-to-refresh and the separate quartet loader remain unchanged.

## Verification

Eight distinct focused tests passed across targeted runs:

- `TMFooterPitchTests`: `testFooterNativeSizes`, `testPitchRenderedLifecycle`, `testSheetKeyRenderedLifecycle`, `testBarberPolePendingQueryLifecycle`.
- Existing `TMPolishRegressionTests`: brand/tint and query failure/retry regressions.
- Both existing `TMSummaryLayoutRegressionTests`, including AX5 and repeated page changes.

iPad footer/query cases also passed. All three production files and the test file remain registered in the existing Xcode Sources phases. `git diff --check` passes. Final composition test log contains no unsatisfiable-layout messages.

Commands used workspace `iOS/iOS.xcworkspace`, scheme `tagmaster`, `/tmp/tm-dd`, specified simulator IDs, `-parallel-testing-enabled NO` and explicit `-only-testing` selections. Builds/tests used `set -euo pipefail` with captured exit status. Initial failures were inspected rather than repeating passing suites. Final logs:

- `/tmp/tm-footer-pitch-confirm.log`: footer, sheet and query, exit 0.
- `/tmp/tm-footer-pitch-confirm-ipad.log`: sidebar/footer and query, exit 0.
- `/tmp/tm-footer-pitch-composition.log`: final affected pitch/query checks, exit 0.
- `/tmp/tm-barberpole-composition-ipad.log`: final iPad query check, exit 0.
- Baselines and initial review remain in `/tmp/tm-footer-baseline*.log` and `/tmp/tm-footer-pitch-review.log`.

One combined visual review and one confirmation were performed. Confirmation exposed a stripe-canvas coverage gap and navigation-bar icon retinting. Two final compositing corrections were verified with affected pixel/lifecycle tests and explicit canvas/rendering-mode assertions, without another visual review. Final captures were refreshed by those tests. No additional aesthetic iteration was performed.

## Evidence and restoration

Captures: `.impeccable/review/tagmaster-ios-footer-pitch-*.jpg` and `tagmaster-ios-barberpole-*.jpg`. All are native simulator renders, longest edge 800px. Held pitch uses real native sound targets with deterministic held control events. The sheet fixture mounts the actual production-created Key control in a native navigation bar; it does not validate Quick Look document rendering.

Both private app containers were backed up before testing, restored with the simulators stopped, and compared byte-for-byte. Both comparison files in `/tmp/tm-footer-pitch-backup` are empty. Simulators were rebooted and original settings confirmed: phone dark/large, iPad light/large. No sandbox reset or generated favorites remain.

LSP was advertised but unavailable in Fabric's active registry. Native compiler/XCTest supplied diagnostics. No hardware audio, VoiceOver gesture automation or full-suite run was performed. The iPad footer fixture uses a native window constrained to sidebar width, not the complete split-view shell.
