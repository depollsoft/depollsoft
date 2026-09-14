# Quartet and glass

## Result and acceptance ledger

One writer implemented both platforms and the shared definition. Existing uncommitted unified-barber-pole and compact-loading work remains. Nothing was committed or pushed.

- [x] One shared quartet definition, deterministic generator, checked-in native paths/constants/samples and consumer drift tests.
- [x] Native sRGB comparisons at phases 0, .125, .25, .5, .75, 1 and still, light/dark, at 216×96 and 2160×960. Android versus iPhone and iPad passes solid-interior and one-pixel-boundary checks.
- [x] Real pending detail contexts on both platforms. Retained cache, completion, refresh, error/retry, request identity, lifecycle and announcement assertions pass.
- [x] Actual public iOS 26 regular `UIGlassEffect`; reactive Reduce Transparency and Reduce Motion policies. The iOS 17–25 system-material branch compiles, but no older runtime is installed.
- [x] Four equal full-safe-width targets, edge/center hit testing, selected accessibility traits, AX5 wrapping, rotation-sized layouts, child containment, preserved selection, iPad sidebar and content clearance.
- [x] One combined inspection and one correction/confirmation batch. No unrelated full suite or extra polishing cycle.
- [x] Original binaries, private/external data and recorded settings restored and byte-checked on all three devices. All backups retained.

## Design and source

The quartet remains the initial detail-loading moment. Four ascending notes make the existing short gathering gesture on a 2.8-second loop. Completion never waits for animation. Reduced motion shows the settled staff. No production timing delay, fake progress, new visibility timer or per-frame path creation was added.

`shared/tagmaster/quartet-loader.json` owns the 216×96 uniform-centered artwork, five staff lines, four note positions, rotated elliptical heads, butt-capped stems, explicit sRGB colors, 75/255 staff alpha, loop endpoints, stagger, active fraction, sine formula, 121 samples per voice, linear interpolation and still translations.

`iOS/tagmaster/tools/generate_quartet_artwork.py` generates:

- `Android/TagMaster/src/main/java/depollsoft/tagmaster/QuartetArtwork.kt`
- `Android/TagMaster/src/main/res/values/quartet_dimensions.xml`
- `iOS/tagmaster/tagmaster/TMQuartetArtwork.h`
- `iOS/tagmaster/tagmaster/TMQuartetArtwork.m`

The generated four-cubic ellipse follows the Android reference radii and -18° rotation. Both platforms use those exact cubic commands, plus the same rectangular butt-capped stem. Paths and Objective-C sRGB colors/sample arrays are cached once. Android Canvas interpolates the generated samples; iOS uses those values in a linear `CAKeyframeAnimation` with one shared start time and no delayed voice starts. Model-layer still translations are zero. The generated colors are light note `#007AA3`, dark note `#5AC8FA`, light staff `#474747` and dark staff `#CACACA`.

Consumers:

- `Android/TagMaster/src/main/java/depollsoft/tagmaster/TagLoadingView.kt`: shared paths, samples, colors and uniform fit. Existing animator, accessibility, attachment, clipping, activity and settings gates remain.
- `Android/TagMaster/src/main/res/layout/tagdetailview.xml`: generated width/height resource references.
- `iOS/tagmaster/tagmaster/DPTagViewController.m`: shared quartet provider replaces independent geometry/easing. Initial-scroll clipping now stops motion through the existing scroll/layout callbacks. Fetch, cache, errors, refresh content, busy accounting, request guards, copy and IDs are unchanged.
- `iOS/tagmaster/tagmaster.xcodeproj/project.pbxproj`: explicit header/file/group/build-phase registration. Android uses its existing source/resource sets.

`iOS/tagmaster/tagmaster/TMPageViewController.m` uses one regular native glass background behind the four existing native buttons. It guards `UIGlassEffect` with `@available(iOS 26.0, *)`; older systems use `UIBlurEffectStyleSystemMaterial`. Reduce Transparency removes the effect and uses opaque `secondarySystemBackgroundColor`. Reduce Motion disables the glass interactive behavior. Both accessibility changes react through notifications, without animated transitions.

Captions and symbols use `labelColor`. A system-blue outline and light tint mark selection without low-contrast blue caption text. Selection also retains the native selected trait. Only the background effect is corner-clipped; the full rectangular button slots remain hittable, including their upper corners. There is no redundant glass per button, gray backing plate, private UIKit traversal, global appearance change or compatibility-mode flag.

The root content still ends at the bar's top. No content-underlap or additional bottom inset was introduced. The transparent home-indicator-area view is noninteractive. Charcoal navigation bars, titles, icons/order, footer, pitch behavior and shared barber-shop art remain unchanged. AX5 height now measures the current font directly and requests layout when traits change.

## Native measurements

Production Android Canvas and the production iOS quartet layer tree exported 84 transparent sRGB PNGs under `/tmp/tm-quartet/frames`. Fixtures hold real detail fetch completions; detached equal-size instances use the production renderer for deterministic artwork export. The live iOS pending instance separately verifies animation values, duration, linear mode and equal start times.

`compare.py`, `metrics.json`, `geometry.py` and `geometry.json` under `/tmp/tm-quartet` record:

- 56 cross-platform comparisons: zero solid-interior RGBA mismatches and zero mask pixels outside the other renderer's one-pixel boundary allowance.
- Exact phase-0/phase-1/still equality on every renderer, theme and size.
- All opaque note pixels equal the JSON's explicit theme color.
- 168 per-note bounds and stem-section measurements against the JSON, not merely against the other platform. Maximum bounds error is 0.608 raster pixels on the 10× canvas; stem sections match the 18-pixel expected width within one pixel.

At 1×, CoreGraphics splits a one-point staff across two approximately half-alpha pixels while Skia can place it on one pixel. A threshold exactly at half of 75 incorrectly erased one renderer's staff mask. The comparison uses a non-half alpha threshold for outer coverage and a separate note threshold; solid-interior checks are unchanged. This is boundary antialiasing, not a relaxed body-color tolerance.

## Tests and commands

Logs, backup scripts and comparisons are outside the repository under `/tmp/tm-quartet`. Shell scripts use `set -euo pipefail`; tool calls preserve failures with `settle:true`. LSP/ast-grep tools were not exposed in the active registry. Native compilers, edit-hook diagnostics and tests supplied diagnostics.

1. `python3 -m unittest discover -s iOS/tagmaster/tools -p 'test_*artwork.py'`: **16 pass**, including the prior 12 barber-pole tests and four quartet tests. Quartet subtests cover 11 consumer mutations and nine shared-input changes. The final quartet-only run also passes. Both generators' `--check` passes after the final edits.
2. Android, JDK 17, `./gradlew :TagMaster:testDebugUnitTest --tests depollsoft.tagmaster.QuartetArtworkTest --tests depollsoft.tagmaster.TagLoadingStateTest :TagMaster:assembleDebug :TagMaster:assembleDebugAndroidTest`: **11 unit tests pass**, both APKs build. The capture-helper correction rebuilt only the instrumentation APK. Logs: `android-build.log`, `android-capture-build.log`.
3. Native `TagLoadingRegressionTest`: **four distinct methods pass across runs**. Failure/retry/retained refresh, old-screen completion and background/hide/detachment passed in the initial run. The corrected dark pending/completion capture passes with `OK (1 test)`. Light pending/completion passed after releasing an emulator first-draw barrier with animations disabled; see the limitation below. Evidence: `android/test-status.log`, `android/dark-confirmation.log`, `android/light-completion.log`.
4. iOS builds use workspace `iOS/iOS.xcworkspace`, scheme `tagmaster`, derived data `/tmp/tm-dd`, the supplied UUIDs and `-parallel-testing-enabled NO`. `build-for-testing` passes. Phone runs the targeted `TMLoadingRegressionTests` class, then only the affected quartet/layout/motion methods and `TMSummaryLayoutRegressionTests`. **16 distinct phone methods pass across runs**. iPad runs the quartet/material/width tests, then the affected confirmation set: **six distinct methods pass across runs**. Final confirmation is five tests, zero failures, on each device. Logs: `ios/{phone,ipad,phone-confirmation,ipad-confirmation}.log`.
5. `testPageCaptionsHaveReadableContrastOnCharcoal` was replaced by `testPageCaptionsUseReadableMaterialAndAccessibleFallback`. It asserts the actual effect class on iOS 26, live transparency/motion changes, semantic caption colors, blue selected treatment and selected traits. It applies a ≥4.5 contrast formula only to the known opaque fallback. Live glass readability is reviewed in native captures, not calculated against an invented fixed background.
6. Explicit Xcode provider/build-phase registration, generated XML references, generator checks and `git diff --check` pass. New quartet tests are in `iOS/tagmaster/tools/test_quartet_artwork.py` and `Android/TagMaster/src/test/kotlin/depollsoft/tagmaster/QuartetArtworkTest.kt`; native exports/assertions extend `TagLoadingRegressionTest.kt` and `TagmasterAppLogicTests.m`.

### Failures and bounded correction

- The initial iOS link exposed a missing Sources-phase entry. Header/file/group entries were present, but the initial read had not reached the Sources section. Added the explicit entry, then the build passed.
- The first native visual review found that changing only text size left the tab bar at its old height. The single UI correction invalidates controller layout on trait changes and measures the new font rather than relying on UIButton's stale size cache. Phone and iPad AX5, split/rotation-sized layouts and edge hit tests pass in confirmation.
- Android's original `UiAutomation.takeScreenshot` stalled and returned null when its shell session timed out. The fixture now uses native `PixelCopy` after an actual frame commit. The dark confirmation passes.
- The light Android run then stalled in the emulator's ActivityScenario first-draw path while the quartet loop was active. Disabling the animator scale released the already-running fixture; native TestRunner reports one test, zero failures. No test was restarted. Its pending capture is explicitly named `light-reduced-motion`. The original scale was restored immediately and verified again during restoration. A normal-motion light pending-window capture is not claimed; both light animated raster sizes/phases are verified.
- One shell summary grep had an invalid expression after the dark test had passed. The preserved native log was checked with fixed-string grep; the test was not rerun.

## Visual evidence

There are **25 JPEGs**, all at most 800px, under `.impeccable/review/`, with the requested prefixes `tagmaster-quartet-matched-` and `tagmaster-ios-glass-tabs-`.

- `tagmaster-quartet-matched-review.jpg`: side-by-side Android/iOS phases in both themes.
- `tagmaster-ios-glass-tabs-review.jpg`: phone/iPad, light/dark, normal/AX5 material tabs after correction.
- `tagmaster-quartet-matched-pending-and-fallback.jpg`: actual pending app windows and opaque Reduce Transparency/AX5 tab crops.
- Individual files retain both iOS themes/devices, Browse/detail, summary, AX5, opaque AX5 and pending contexts; Android retains dark pending and light reduced-motion pending.

One initial inspection and one confirmation batch used Pi's native image read, one JPEG per call. Final images show wrapped, visible captions, legible semantic text, clear blue selection and no bar over initial placeholders. Existing unrelated captures remain untouched. No further visual iteration was performed.

## Preservation and restoration

Baseline pending diff and SHA-256 manifest were saved before editing. **144 previously pending files compare byte-identically**. Excluding this new audit, the only pre-existing baseline hash changes were the three intentionally extended iOS files: `DPTagViewController.m`, `TagmasterAppLogicTests.m` and `project.pbxproj`. The new quartet/page-control changes also touch previously clean files listed above. No prior uncommitted file was discarded. Shared barber-pole definitions, generator, generated art, compact loader, background vector and launch PDF remain unchanged and pass their generator check.

Fresh backups were made and validated before any test installation. Android tar manifests were checked for unsafe paths and extracted; stopped iOS app/data copies were compared before boot. Prior `/tmp/tm-consistent` and `/tmp/tm-unified` backups were not modified.

Actual before/after state:

| Device | State restored |
| --- | --- |
| `emulator-5554` | Night yes, font 1.0, animator scale 1.0, original size/density, original production/test APKs and all private/external file contents |
| iPhone 17 Pro `C8B74E44-94F7-4CED-A47F-DFF98E34237B` | Booted, dark, large, Reduce Motion 0, original app and private user data |
| iPad Pro 11-inch M5 `55A9555A-9714-4FE0-8F0D-1035652526F9` | Booted, light, large, Reduce Motion key absent, original app and private user data |

`restore-android.log` records `ANDROID_RESTORE_VERIFIED`. `restore-ios.log` records both `IOS_RESTORE_VERIFIED` entries and retained-backup completion. Original apps compare byte-for-byte; user-data checksum readback passes. The iOS check excludes only current container-manager identity metadata and OS-regenerable `Library/SplashBoard` snapshots. Accessibility material fixtures use local method substitution, not global preference writes. All backups remain under `/tmp/tm-quartet/{android,ios}`.

## Remaining limits

Only iOS 26.5 is installed. The availability-guarded iOS 17–25 material fallback compiles but was not executed on an older runtime. The supplied phone/iPad UUIDs identify simulators; no physical-device, manual VoiceOver walkthrough or material GPU-performance claim is made. Rotation coverage changes real UIKit window dimensions and exercises iPad split/sidebar layouts, rather than claiming a physical-device rotation journey. The Android light pending capture is reduced-motion for the documented harness reason. No signing, account, sync, URL, shared Pitch Perfect/depolllib, footer or pitch changes were made. Main can review all pending work before publication.
