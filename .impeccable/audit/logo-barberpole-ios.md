# iOS logo barber pole

## Scope

Refined only `TMBarberPoleLoadingView` inside `iOS/tagmaster/tagmaster/DPTagQueryViewController.m` and its tests in `iOS/tagmaster/tagmasterTests/TagmasterAppLogicTests.m`. The surrounding query controller, beginning at its private interface, is byte-identical to HEAD. Footer placement, query arguments, Search/Browse behavior, pitch controls, navigation, the quartet loader and shared libraries were not changed. No commits or pushes.

## Acceptance ledger

- **Canonical geometry:** embedded native CGPath cubics converted offline from `Android/TagMaster/src/main/res/drawable/ic_barberpole.xml`. Mechanical comparison verified all 87 move/curve commands in subpaths 0, 1, 2, 7 and 8. The silhouette has 48 original cubic segments. Four original highlight subpaths retain both round finials and flared collars. No runtime SVG parser, dependency or bitmap asset was added. The watermark was read and left unchanged.
- **Size and angle:** viewport 299.75076 × 513.52234, aspect 0.5837151311. Uniform scaling produces 33.85548 × 58pt artwork, centered with 5pt vertical padding in the existing 68pt footer. The shaft axis follows the canonical edge, 25.70870° from vertical. Native tests check bounding-box dimensions, equal scale components, centering, circular finial interiors/excluded square corners, collar points and curve counts.
- **Motion:** the stationary logo contains a separately masked shaft. Curved red/white/blue bands translate 96 local shaft units over the existing two-second linear loop. The parent shaft transform projects movement along the diagonal axis. Only stripe phase animates. Canvas coverage includes extra repeats at both ends. Signed repeat arithmetic prevents negative offsets from overflowing.
- **Pixels:** each device checks light and dark at phases 0, .25, .5, 1 and still Reduce Motion. These are native renders of the production layer tree while the actual provider query is held. All 20 final device/theme/phase samples have zero changes outside the shaft comparison region, zero silhouette-alpha changes and zero colored pixels outside the canonical silhouette. Loop endpoint and reduced-motion pixels equal phase zero byte-for-byte.
- **Color and movement:** at 150 × 257 native comparison resolution, each phase contains 1,665–1,907 red, 1,703–1,955 blue and 3,296–3,420 white shaft pixels. Quarter-phase changes 7,676 light / 7,677 dark pixels; half-phase changes 4,171. Metal path and transform stay unchanged. Live presentation offsets also change: phone 11.78 → 48.12, iPad 10.79 → 46.67 local units.
- **Lifecycle/accessibility:** retained pending-before-view, successful completion, failure/retry, Reduce Motion toggle, hidden, background/foreground, controller appearance, detach/reattach and query-argument assertions. Added explicit offscreen stop/resume coverage. `Loading tags` remains the single accessible element. No sound or artificial delay. Existing query failure, exhausted-results and refresh regression passes. No unrelated cancellation, pagination or loader tests were removed or weakened.
- **Registration:** existing Sources entries verified at `project.pbxproj` lines 722 and 747; existing watermark resource entry at line 680. No project-file changes or new registrations needed. `git diff --check -- iOS/tagmaster` passes. LSP tools were advertised but unavailable in the active Fabric registry; native compiler/XCTest supplied diagnostics.

## Tests and logs

Three distinct test methods pass across five device/test combinations:

1. `TMFooterPitchTests/testBarberPoleLogoGeometry`: phone and iPad.
2. `TMFooterPitchTests/testBarberPolePendingQueryLifecycle`: phone and iPad.
3. `TMPolishRegressionTests/testQueryFailureRetryAndExhaustedRefresh`: phone.

All commands ran from `iOS`, with `set -euo pipefail`, tool `settle:true`, bounded output and logs under `/tmp`:

```sh
xcodebuild -workspace iOS.xcworkspace -scheme tagmaster \
  -derivedDataPath /tmp/tm-dd \
  -destination 'platform=iOS Simulator,id=DEVICE' \
  -parallel-testing-enabled NO \
  -only-testing:tagmasterTests/TMFooterPitchTests/testBarberPolePendingQueryLifecycle \
  test > /tmp/LOG 2>&1
```

Selections and exact run totals:

| Log under `/tmp` | Selection | Result |
| --- | --- | --- |
| `tm-logo-phone.log` | All three methods | 3 tests; 2 pass, lifecycle fails with 24 assertions |
| `tm-logo-ipad.log` | Lifecycle, `test-without-building` | 1 test; 24 assertions fail |
| `tm-logo-phone-confirm.log` | Lifecycle | 1 test; 24 assertions fail |
| `tm-logo-phone-final.log` | Geometry + lifecycle | 2 tests; geometry passes, 14 pixel-coordinate assertions fail |
| `tm-logo-phone-pixels.log` | Lifecycle | 1 test; 0 failures, exit 0 |
| `tm-logo-ipad-final.log` | Geometry + lifecycle, `test-without-building` | 2 tests; 0 failures, exit 0 |

The first native inspection exposed missing bands. Explicit layer bounds alone did not fix them; negative stripe rows had been promoted to unsigned values. Signed offsets fixed production rendering, and a new bounds assertion guards that failure. The remaining 14 failures came from an unnecessary bitmap y-flip in the new pixel test. Correcting its coordinate mapping made the unchanged clipping assertions pass. No failing assertion was deleted or threshold weakened. These were targeted diagnostic iterations, not full-suite reruns. Only one combined visual inspection and one combined visual confirmation were performed.

## Evidence

Eleven JPEGs under `.impeccable/review/tagmaster-ios-logo-pole-*.jpg`:

- `review.jpg`: single 780 × 720 phone/iPad light/dark comparison inspected through `pi.read`.
- `phone-comparison.jpg`, `ipad-comparison.jpg`: 780 × 360 native small/enlarged phase/end/still comparisons with the unchanged watermark and pending-query context.
- `light-phase1`, `light-phase2`, `dark-pending`, `dark-reduced-motion`, each with `-phone.jpg` and `-ipad.jpg`: native pending-window captures, longest edge ≤800px.

Fixtures use AX5 window traits without changing device text settings. The native query controller is mounted in a test navigation window, not a reconstructed drawing-only controller. No physical-device, VoiceOver gesture automation or full-suite certification is claimed.

## Restoration and provenance

Devices:

- Phone `C8B74E44-94F7-4CED-A47F-DFF98E34237B`: dark, large, ReduceMotionEnabled=0.
- iPad `55A9555A-9714-4FE0-8F0D-1035652526F9`: light, large, ReduceMotionEnabled absent.

Original private `depollsoft.tagmaster` containers were backed up to `/tmp/tm-logo-backup` before tests. Xcode replaced container UUIDs during installation. Restoration used the current registered container paths, with both simulators stopped. Both `*-restored.diff` files are empty after byte comparisons. Both devices were rebooted; original appearance, content size and motion defaults confirmed in `restored-settings.log`. Test motion overrides and query swizzles restore in `@finally`. Capture files were copied out before restoration. No test app data remains in the restored containers.

Canonical SHA-256: `94945f08f42627974860086ea78f27f5cd00d0b56e47590bd75191b7891b0c54`.
Watermark SHA-256: `9c1d524ee9fd1a60b66381c574342f5f409f58f6aa4600ef674cfbbc01081763`.
Geometry comparison measurements: `/tmp/tm-logo-provenance.json`.

Harness side effect: Pi lens automatically ran ktlint on the other agent's `Android/TagMaster/src/androidTest/java/depollsoft/tagmaster/BarberPoleQueryRegressionTest.kt` after an iOS bash command. The Android owner was notified through the project mesh. No Android source was intentionally edited or reverted by this worker, and no tool configuration was changed.
