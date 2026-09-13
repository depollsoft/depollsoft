# Android floating sheet-key surface

The supplied screenshot showed an inset rectangular shadow/fill artifact inside the key control over sheet music. The ExtendedFloatingActionButton used a transparent idle/disabled fill, which is unsuitable for its elevated Material background.

## Fix

`sheet_key_fill.xml` now uses an opaque `sheet_key_surface` for idle and disabled states, while the existing activated state remains primary blue. The surface aliases the light surface-container-lowest and dark surface-container-high roles. These are distinct from colorSurface, so Material's elevation overlay does not unexpectedly tint the face and reduce label contrast. Summary-page outline styling, note activation, hold/release/cancel behavior, dimensions and layout remain unchanged. No iOS or shared-library changes.

## Verification

- New `SheetKeySurfaceTest`: 2/2 failed on the transparent baseline, then pass with the fix. Light/dark checks require opaque fill in every state, readable text/icon colors, unchanged bounds, and identical body pixels over white paper and black ink. Uses the reported Major:Eb label.
- The initial opaque colorSurface candidate passed those unit tests but native rendering exposed an additional Material elevation tint. The final dedicated surface role avoids it; the native assertion now explicitly requires opaque floating-button fill.
- Existing `FooterPitchRegressionTest.sheet_note_bound_rendering_and_lifecycle` passes in light and dark. Covers idle, held, released, cancelled, disabled, timed accessibility activation and stopping on background. Rendered fill/text/icon checks pass. Native captures use a local chart/note fixture rather than the user's chart.
- Debug app and test APK builds pass. Device app/data backed up before each native run, original app/data restored after, and night mode returned to its recorded on state. Backup folders remain under /tmp/tm-sheet-key-*. No production audio code changed; native tests use a silent player.
- Captures: `.impeccable/review/tagmaster-android-sheet-key-surface-{light,dark}-{before,held,released}.jpg`. Idle light, held light and released dark captures were inspected.

Logs: `/tmp/tm-sheet-key-baseline.log`, `/tmp/tm-sheet-key-surface-final.log`, `/tmp/tm-sheet-key-{light,dark}-final.log`. No full suite or physical-device certification claimed.
