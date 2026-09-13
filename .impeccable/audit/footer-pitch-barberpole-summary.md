# Compact credits, pitch feedback and barber-pole search loading

## Changes on both platforms

- Home footer groups attribution into one link and shares a row for developer/copyright, Terms and Donate. Groups wrap for narrow widths or larger text. All original destinations, metadata and accessible tap areas remain.
- Summary and sheet-music Key controls fill blue while their bound note is sounding, with contrasting text/icons. Release, cancellation, timed completion and replacement restore the outline without changing geometry or shared audio code.
- Browse/Search query indicators now show a small upright barber pole. Red/white/blue stripes move inside a stationary frame with a two-second linear loop. Reduced Motion/Remove Animations gives a still pole; completion, failure, hidden/offscreen state, background and detach stop the animation. No extra waiting, sound or changes to query parameters. The separate quartet tag-detail loader is unchanged.

## Measured results

- Android footer at 360dp: 262.86dp -> 128.38dp at default font size; 274.29dp -> 133.33dp at 1.3; all four links keep disjoint >=48dp targets. Font scale 2.0 and widths 320/600dp also checked.
- iOS footer: 200pt -> 112pt on phone, 210.5pt -> 112pt at sidebar width. All links retain >=44pt targets; AX5 wraps without clipping.
- Held pitch rendering verified in light/dark using real control events and deterministic note fixtures, including fill/text/icon pixels and >=4.5:1 contrast.
- Native stripe-phase checks verify moving stripe content while caps/frame remain stationary.

## Verification

Platform reports contain the targeted commands and detailed evidence:

- `footer-pitch-android.md`: 80 distinct focused unit cases passed across runs, plus native footer/pitch/light/dark/large checks.
- `barberpole-android.md`: four new pole tests, native initial/pagination/retry/motion checks and lifecycle coverage.
- `footer-pitch-ios.md`: eight distinct focused tests passed across targeted runs, plus iPad footer/query checks.

Coordinator rechecked the final Android tree after formatting: all eight new footer/pole tests passed, debug and instrumentation APKs built. Source whitespace checks passed. Native compact-footer, held-pitch and pending-pole JPEGs were inspected. Private app data and recorded simulator/emulator settings were restored by the workers.

No full repository suite or physical-device audio/VoiceOver certification is claimed. Native fixtures and pixel checks provide the targeted evidence. No shared-library, backend, credential or signing changes. Build/test logs remain under /tmp, not in the repository.
