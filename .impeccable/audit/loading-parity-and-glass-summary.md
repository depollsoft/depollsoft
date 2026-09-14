# Matched loading artwork, consistent indicators and native glass

## Delivered

- Both barber-pole renderers use one shared definition for silhouette parts, curved stripes, colors, clipping, sizes and the two-second loop. Same-phase native comparison found zero solid-interior color mismatches; differences are confined to raster boundaries, at most one comparison pixel.
- Random Tag, appropriate saved-row/track/sheet/rating loaders and iOS refresh use a compact version of the same artwork. Numeric ratings, playback controls, progress bars, native pull-to-refresh and third-party UI remain intact. The host inventory and exclusions are in `consistent-loading-indicators.md`.
- Both tag-detail quartet loaders now share geometry, colors and sampled motion. The four notes use the same 2.8-second loop and settled reduced-motion pose. All 56 cross-platform native phase comparisons passed within boundary-antialiasing tolerance.
- iOS page tabs use actual native `UIGlassEffect` on iOS 26, a system-material fallback on older versions, and an opaque accessible surface for Reduce Transparency. Labels use semantic colors with a blue selection treatment. Full safe-width targets, AX5 wrapping, selection/restoration, sidebar clearance and content separation remain.

## Final checks

- Both artwork generators' `--check` pass. All 16 generator/drift tests pass.
- Coordinator re-read the autofixed Android renderer and native-test file, then rebuilt the debug and instrumentation APKs and reran 11 quartet/loading-state unit tests: all pass.
- Final iOS quartet/material/layout confirmation runs pass 5/5 on both iPhone and iPad. Earlier targeted tests cover the remaining loading hosts, lifecycle, clipping, query behavior and cross-platform raster comparisons, as detailed in the reports below.
- Native comparison sheets for poles, compact host controls, quartet phases and glass tabs were inspected. Source registration, Xcode project plist validation and whitespace checks pass.
- Workers restored original apps, private data and recorded device settings; backups remain under /tmp.

## Evidence and limitations

Detailed reports: `unified-barberpole.md`, `consistent-loading-indicators.md`, `quartet-and-glass.md`.

Native JPEGs: `tagmaster-unified-barberpole-*`, `tagmaster-consistent-loading-*`, `tagmaster-quartet-matched-*`, `tagmaster-ios-glass-tabs-*` under `.impeccable/review/`.

These are targeted checks, not a new full-repository suite or physical-device performance/VoiceOver certification. The iOS 17–25 fallback compiles but was not exercised on an older runtime. The Android light pending-window capture required reduced motion to release a test-harness first-draw stall; light animated artwork was verified in native phase renders. Static backgrounds, account/sync contracts, signing and shared Pitch Perfect libraries are unchanged.
