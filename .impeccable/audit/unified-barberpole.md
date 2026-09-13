# Unified barber pole

## Result and acceptance ledger

The current Android logo-derived appearance is canonical. Both platforms now consume one offline definition, not independently authored stripes.

- [x] Shared geometry, palette, clipping, size, phase formula and motion. Generated-output and renderer-consumption drift checks pass.
- [x] Actual native sRGB comparisons at 340×580, phases 0/.25/.5/.75/1, both themes, Android against iPhone and iPad. Zero solid-interior RGB mismatches; maximum border displacement 1 raster pixel.
- [x] Stationary frame/alpha, shaft-only color changes, exact loop endpoint, near-wrap continuity and uniform 34×58 artwork. Static logo provider and launch PDF unchanged.
- [x] Existing native query, accessibility, lifecycle and reduced-motion assertions retained. Geometry expectations updated for the canonical shaft, 26° parent rotation, 216-unit motion and transparent highlights.
- [x] One combined native visual review, with same-phase small/enlarged pairs and pending-query context. No visual correction or confirmation round needed.
- [x] Original app binaries, private user data and device settings restored. Restoration scripts needed repairs, documented below. No commit or push.

## Shared definition and generation

`shared/tagmaster/barberpole-loader.json` contains loader-only parameters. It references the original XML and contour indices rather than copying the full logo. It specifies the shaft commands, clip, fill rules, zero stroke, transparent highlight contours, colors, uniform fit, 34×58 size, signed repeat range, axis, phase formula, direction, duration, linear timing and static phase.

`iOS/tagmaster/tools/generate_logo_artwork.py` emits checked-in native code into:

- `Android/TagMaster/src/main/java/depollsoft/tagmaster/BarberPoleLogo.kt`
- `iOS/tagmaster/tagmaster/TMLogoArtwork.h`
- `iOS/tagmaster/tagmaster/TMLogoArtwork.m`

The original XML remains read-only input. The generator documentation now distinguishes this from generated Android output. No build step, runtime JSON/SVG parser, dependency or per-frame path construction was added. Existing Android source registration and existing iOS provider registration suffice; project/build/signing files are unchanged.

The generator rotates contour 5 by -26°, computes bounds over its transformed control points, then scales x by 1.4 about that bounds center. It computes this once before emitting either language. It does not use CoreGraphics curve-extrema bounds. A new Android native-raster unit test reproduces the original Android PathMeasure/computeBounds transform and compares bounds, length and 101 path samples within 0.001 canonical units.

Metal is original contour 0 with contours 1, 2, 7 and 8 cut out using even-odd fill. The shaft is the former Android mask. Red is `#BE2A35`, blue `#0063A5`, white `#FFFFFF`, light-theme metal `#474747`, dark-theme metal `#CACACA`. Highlights are holes, not white paint. Cached Objective-C paths and sRGB colors are borrowed immutable process-lifetime values; callers neither mutate nor release them. Android paths are cached once and only read by Canvas.

Both renderers use repeats -6…6 and `(index + normalizedPhase * 2) * 108`. One two-second linear loop advances 216 units in positive local y. iOS now rotates `axisLayer` and translates its `stripes` child, so motion follows the shaft instead of screen y. Uniform fitting gives actual artwork 33.8554776×58 inside the 34×58 logical box. Native surrounding padding is unchanged.

`--check` verifies both native outputs and the PDF, requires renderer references to shared values, rejects the old independent palette/period/handcrafted geometry, and verifies the unchanged Android XML loader size. `test_logo_artwork.py` tests seven deliberate drift cases in temporary repository copies plus the current outputs.

Provenance:

- Shared JSON SHA-256: `72c8069bb0624c265200e89bf17cbe7e17c88523d3f4ee4e483832db2890942d`
- Canonical XML SHA-256: `94945f08f42627974860086ea78f27f5cd00d0b56e47590bd75191b7891b0c54`
- Unchanged launch PDF SHA-256: `da51a8ced5ba2793cefc44ff96306a2e70ee037fd13e2a5b1738cc53b506e115`

The entire old `TMLogoArtwork.m` is a byte-identical prefix of the new file. Full-logo paths retain all nine contours and 122 cubics. The background view, XML, PDF and query controller body after its private interface are byte-identical to HEAD. No watermark tint/layout, footer behavior, pitch, navigation, quartet loader, search semantics, shared library, credential or configuration edits.

## Native pixel measurements

Production Android Canvas and the production iOS layer tree rendered transparent PNGs in explicit sRGB, at actual 340×580 and 34×58 pixel sizes. No device-density assumption or reconstructed renderer was used. iOS frames came from the held real query view; Android uses its production renderer in a detached equal-size instance during the held query. Natural on-screen motion and still behavior are tested separately by the existing fixtures.

The table shows mean absolute error over premultiplied RGBA channels on the 0–255 scale. iPhone and iPad results are identical. Raw pixels are not identical across Skia/CoreGraphics.

| Phase | Light MAE | Dark MAE | Visible red/blue bands |
| --- | ---: | ---: | --- |
| 0 | 0.226076 | 0.317306 | 1 / 2 |
| .25 | 0.226398 | 0.318536 | 1 / 2 |
| .5 | 0.225975 | 0.317108 | 2 / 1 |
| .75 | 0.226041 | 0.318051 | 2 / 1 |
| 1 | 0.226076 | 0.317306 | 1 / 2 |

Across all 20 cross-platform comparisons:

- 64,188–64,234 solid-interior pixels compared per frame, with zero mismatches. Interiors exclude a one-pixel neighborhood of color/alpha boundaries.
- 4,154–4,297 pixels differ somewhere in premultiplied RGBA, out of 197,200 canvas pixels. Differences are confined to raster boundaries.
- Red, blue, white, metal and thresholded outer-alpha masks each have maximum bidirectional Chebyshev border displacement of 1 pixel. This is 0.1 logical point on the 10× canvas, not a claim of raw pixel perfection.
- Outer-alpha mask intersection/union is 0.996545. Solid red/blue connected-component counts match on all three renderers at every phase.
- 72 exported phase/size/theme/device comparisons pass invariant alpha, exact endpoint equality and near-wrap continuity at .001/.999. Existing shaft-mask tests retain zero changes outside their one-raster-pixel boundary allowance. Metal and background remain stationary.

Metrics and raw evidence are outside the repository: `/tmp/tm-unified/frames/` contains 98 transparent PNGs; `metrics.json`, `invariants.json`, `provenance.log`, `compare.py` and `invariants.py` describe and reproduce the measurements. The offline comparison helper uses the already-installed Pillow; the source generator and drift tests use only Python's standard library.

## Tests

All native test methods passed on their first run. No unchanged passing native suite was rerun. A later Android invocation ran only the newly added canonical-transform test.

1. JDK 17, from `Android/`:
   `./gradlew :TagMaster:testDebugUnitTest --tests depollsoft.tagmaster.BarberPoleLoadingViewTest :TagMaster:assembleDebug :TagMaster:assembleDebugAndroidTest`
   Five unit tests pass; both APKs build. Log: `/tmp/tm-unified/android-build.log`.
2. New canonical-transform unit test only: one pass. Log: `/tmp/tm-unified/android-canonical-transform.log`. Six distinct Android unit tests total.
3. Native `depollsoft.tagmaster.BarberPoleQueryRegressionTest`, once each in light, dark and light with animations disabled: three passes. Logs: `/tmp/tm-unified/android/{light,dark,light-reduced-motion}.log`. Held Search, parameters, pagination, failure/retry/completion, background/resume and offscreen Browse checks remain intact.
4. iOS workspace `iOS/iOS.xcworkspace`, scheme `tagmaster`, derived data `/tmp/tm-dd`, explicit simulator UUIDs, `-parallel-testing-enabled NO`:
   - iPhone: `TMFooterPitchTests/testBarberPoleLogoGeometry`, `testBarberPolePendingQueryLifecycle`, `testSharedVectorArtwork`, plus `TMPolishRegressionTests/testQueryFailureRetryAndExhaustedRefresh`. Four pass.
   - iPad, `test-without-building`: the three `TMFooterPitchTests` methods above. Three pass.
   - Logs: `/tmp/tm-unified/ios/{phone,ipad}.log`.
5. `python3 -m unittest discover -s iOS/tagmaster/tools -p test_logo_artwork.py`: eight pass. Log: `/tmp/tm-unified/generator-tests.log`.
6. Generator `--check`, explicit source/provenance comparison, saved-frame invariant probes and `git diff --check` pass.

LSP/ast-grep extension tools were not exposed in the active Fabric registry. Native compilers, Robolectric native graphics, instrumentation and XCTest supplied diagnostics. No full-suite or physical-device performance claim.

## Visual review and snapshots

One `pi.read` inspection of `tagmaster-unified-barberpole-review.jpg` confirmed matching curved bands, same phase positions, transparent finial/collar highlights and legibility at 34×58 in both themes. No follow-up visual capture was needed.

Six checked-in-candidate JPEGs under `.impeccable/review/`, all dimensions ≤800px:

- `tagmaster-unified-barberpole-review.jpg`, 800×780.
- `tagmaster-unified-barberpole-android-light-pending.jpg`, 360×800.
- `tagmaster-unified-barberpole-android-dark-pending.jpg`, 360×800.
- `tagmaster-unified-barberpole-android-light-reduced-motion-pending.jpg`, 360×800.
- `tagmaster-unified-barberpole-ios-phone-pending-phases.jpg`, 780×360.
- `tagmaster-unified-barberpole-ios-ipad-pending-phases.jpg`, 780×360.

The iOS native comparison sheets include small/enlarged phases, Reduce Motion and actual light/dark pending-window insets. Separate full pending-window JPEGs were written into a nested fixture directory that the initial pull omitted. They were not recovered by rerunning tests; retained native comparison sheets supply the real pending context. Existing unrelated review PNGs were left alone.

## Restoration and limits

Actual initial settings:

- Android `emulator-5554`: 1080×2400, density 420, night yes, font 1.0, animator scale 1.0.
- iPhone `C8B74E44-94F7-4CED-A47F-DFF98E34237B`: booted, dark, large, ReduceMotionEnabled 0.
- iPad `55A9555A-9714-4FE0-8F0D-1035652526F9`: booted, light, large, ReduceMotionEnabled absent.

Scripts in `/tmp/tm-unified/` used `set -euo pipefail`, bounded timeouts and EXIT restore traps. Tool calls used `settle:true`. Original production/test APKs and private Android data were backed up before installation. Original iOS app bundles and stopped data containers were copied before tests.

Restoration exposed two script defects after all native tests had passed. Android's external tar command incorrectly included `shell` after `exec-out`; validation was added before any future external restore. The retained external pull contained only this fixture's images, not user files. Original APKs and all private Android data were restored and byte-compared successfully, and theme/font/motion settings match their backups. No application user-data loss was observed.

iOS bundle registration briefly returned an obsolete path after reinstalling the backup. Restoration-only recovery completed both devices without rerunning tests. Both original app bundles compare byte-for-byte equal. iPhone's complete data container matches; iPad user data matches, with only OS-regenerated `Library/SplashBoard` launch snapshots differing after reboot. Both devices are booted with their original theme/text/motion settings. Backups remain in `/tmp/tm-unified/{android,ios}/`.

No raw build logs, PNG comparison frames, signing material or credentials were added to the repository. No commits or pushes.
