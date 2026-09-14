# Shared vector artwork, iOS

## Result

`iOS/tagmaster/tagmaster/TMLogoArtwork.h/.m` supplies borrowed, immutable `TMLogoFullPath`, `TMLogoSilhouettePath`, and `TMLogoHighlightsPath`. One `dispatch_once` builds the three process-lifetime paths. Temporary mutable paths are released; callers neither mutate nor release shared paths. CAShapeLayer makes its own copies.

`TMLogoBackgroundView.h/.m` uses the full compound path, nonzero winding, fixed gray 128/255 and alpha 76/255. It has no drawing loop or animation. Layout only changes the layer transform when bounds change. Uniform scaling preserves the old 480×800 canvas margins. `DPAppDelegate` retains its full-width, top-60/bottom-44 constraints and UITableView backgroundView ownership. The artwork ignores input and is excluded from accessibility.

The loader uses the same provider's silhouette and finial/collar highlights. Code from the shaft-mask definition through the end of `DPTagQueryViewController.m` is byte-identical to the pre-task uncommitted version. All 92 existing path operations, including five closes, match the extracted provider exactly. The 34×58 artwork, 25.71° shaft, signed stripe repeats, two-second animation, lifecycle, and Reduce Motion behavior remain unchanged.

## Provenance and registration

- Read-only canonical input: `Android/TagMaster/src/main/res/drawable/ic_barberpole.xml`, SHA-256 `94945f08f42627974860086ea78f27f5cd00d0b56e47590bd75191b7891b0c54`.
- `iOS/tagmaster/tools/generate_logo_artwork.py --check` verifies all nine contours and 122 cubics, exact viewport 299.75076×513.52234, native constants, and generated launch PDF. The bounded offline converter accepts only the input's M/m/c/z grammar. No runtime parser, library, or build dependency.
- Sources entries: `project.pbxproj` lines 718–719. Header/file/group entries verified.
- PNG resource moved from app Resources to test Resources, line 701. Historical file remains unchanged, SHA-256 `9c1d524ee9fd1a60b66381c574342f5f409f58f6aa4600ef674cfbbc01081763`.
- No production PNG lookup remains. Native tests load the historical PNG explicitly from the test bundle and render the shared vector for the loader comparison.
- `LaunchScreen.xib` also had a PNG lookup. It now uses `LaunchWatermark`, a PDF generated from the same contours and canvas mapping. Launch screens cannot execute custom app drawing code. `assetutil` confirms a preserved Vector rendition in Assets.car. Production bundle scan finds no screenbackground PNG outside the test plugin.

## Acceptance evidence

`testSharedVectorArtwork` checks 100 repeated identities for each public path, all nine closed contours, 122 curves, geometric equality of extracted parts, and eight interior cutout points. Layer copies remain identical across unchanged layout and resizing.

Both simulators run 16 watermark comparisons each: UIView/UITableView ownership, light/dark, and four sizes. Every case passes exact color/alpha, decorative AX, hit-testing, constraints, uniform scale, no animation keys, and historical-image comparison.

| Root size | Ink intersection/union | Mean alpha error, 0–1 | Maximum bbox edge difference |
| --- | ---: | ---: | ---: |
| 320×568 | 0.969892 | 0.002245 | 1px |
| 402×874 | 0.968997 | 0.002208 | 1px |
| 834×1210 | 0.968490 | 0.001983 | 0px |
| 1194×834 | 0.969450 | 0.000935 | 1px |

The pending-query test checks ten phase/theme/still renders per device. Background pixels remain byte-identical as loader phase advances, with no background animation keys. Existing checks retain zero colored spill, zero changes outside the shaft, unchanged silhouette alpha, and exact loop-end/still equality. Live stripe offsets advance 11.40→47.06 on phone and 11.53→47.49 on iPad.

Targeted native results, logs in `/tmp`:

- `tm-shared-vector-phone.log`: geometry and query failure/retry/exhausted-refresh pass. Two new-test failures produced 68 assertions: layer-pointer identity incorrectly ignored CAShapeLayer copy semantics, and Auto Layout undid an artificial resize.
- `tm-shared-vector-phone-confirm.log`: corrected shared-artwork and pending-query lifecycle tests pass, 2 tests, 0 failures. Provider pointer-identity assertions remain; layer geometry and stable per-layer identity are checked separately. Resize is tested after detaching constraints. No production fix or relaxed pixel threshold.
- `tm-shared-vector-ipad.log`: shared artwork, existing geometry, and pending-query lifecycle pass, 3 tests, 0 failures.

Four distinct methods pass across seven device/method combinations. Commands use workspace `iOS/iOS.xcworkspace`, scheme `tagmaster`, derived data `/tmp/tm-dd`, `-parallel-testing-enabled NO`, and explicit `-only-testing` selections. iPad uses `test-without-building`. No full-suite rerun. `git diff --check` and project plist validation pass. LSP tools were absent from the active registry; native compiler/XCTest supplied diagnostics.

## Captures and restoration

Thirteen JPEGs in `.impeccable/review/tagmaster-ios-shared-vector-*.jpg`, all ≤800px. One combined native review, `tagmaster-ios-shared-vector-review.jpg`, includes both devices' old/new light/dark watermarks, loader phases, and actual held-query windows with the existing charcoal navigation. No visual correction or second review was needed.

Original containers were backed up stopped under `/tmp/tm-shared-vector-backup` before testing, then restored to the current registered paths while stopped. Both recursive byte-comparison diffs are empty. Both devices are booted again, with settings identical to the recorded originals:

- Phone `C8B74E44-94F7-4CED-A47F-DFF98E34237B`: dark, large, ReduceMotionEnabled=0.
- iPad `55A9555A-9714-4FE0-8F0D-1035652526F9`: light, large, ReduceMotionEnabled absent.

No Android/shared-library/signing/config changes, commits, or pushes were made by this worker. The new development app build remains installed; original app data is restored.

## Limits

The canonical vector and historical raster are close, not pixel-identical. No physical-device profiling, VoiceOver gesture automation, full-suite certification, or frame-rate improvement is claimed. Runtime watermark decoding is removed and path construction is cached. Bundle-size reduction is not claimed: the old PNG is 17,961 bytes; the launch asset compiler retains both a vector rendition and a raster fallback. Native source is 14,021 bytes and the generated PDF is 5,940 bytes before compilation.
