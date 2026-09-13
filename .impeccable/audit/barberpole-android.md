# Android query barber pole

## Result and acceptance ledger

- [x] Browse and Search share an app-local 28x52dp upright pole. Only clipped red/white/blue stripes move, linearly over two seconds; neutral caps and outline stay fixed.
- [x] Existing loadingProgressBar ID retained. Its public Loading setter owns visibility. No missing Indeterminate binding remains.
- [x] Pending initial and pagination requests, completion, failure/retry, background/resume, hidden/offscreen pages and detach are covered.
- [x] Remove Animations shows a still pole; live setting changes stop and restart motion. One stable accessible description, "Loading tags". No per-frame accessibility changes.
- [x] No query parameters, cache/data contracts, quartet loader, sound, haptics, dependencies or other app spinners changed.

The query list is constrained above its footer. Loading may change inside ListView's layout callback, so the pole coalesces a subsequent relayout instead of retaining zero-size bounds. View and fragment lifecycle hooks stop the ValueAnimator and unregister its motion observer.

## Verification and evidence

Four focused unit tests pass for fixed-frame/moving-stripe pixels, motion lifecycle, XML/setter registration and pending-footer bounds. Native pending Search, pagination, failure/retry, resumed activity and offscreen Browse pass with motion enabled and disabled. Final logs: `/tmp/tagmaster-footer-pitch/barber-confirmation.log`, `pagination-final-build.log` and `final-reduced-current.log`.

The native fixture pauses a real Tag.query request at URL.openStream. It disables automatic near-end retry only while asserting terminal failure. ActivityScenario's synchronous first-draw barrier requires a test-only motion pause around launch; the original scale is restored before movement assertions. Natural frames are sampled 500ms apart. Screenshot fixtures freeze only the drawing phase and then restore animation.

Confirmed captures: `.impeccable/review/tagmaster-android-barberpole-dark-confirmed-*.jpg`. Reduced-motion light captures use `tagmaster-android-barberpole-light-reduced-motion-*.jpg`. All are native JPEGs, maximum dimension 800px, including pending pagination after an existing row and the pole above full-width Browse tabs. Earlier unreliable dark captures were discarded.

Original APK, private app data and device settings were restored and compared with their backups. Android 16 emulator coverage only, not physical-device performance. No commits or pushes.
