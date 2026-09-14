# Android logo barber pole

## Acceptance ledger

- [x] Canonical art, not a rotated generic loader. `BarberPoleLogo.kt` reads `res/drawable/ic_barberpole.xml` once per view with AndroidX PathParser, then caches native paths. The first contour supplies the exact outer silhouette. Original contours 1, 2, 7 and 8 supply ball/collar highlight cutouts; contour 5 supplies the curved helical stripe. No dependency, bitmap asset, copied full logo or per-frame parsing was added. The watermark is unchanged.
- [x] Uniform fitting of the 299.75076 × 513.52234 viewport inside a centered 34 × 58dp artwork box. Actual artwork is approximately 33.856 × 58dp. Existing 16dp margins and footer constraints remain unchanged. No stretching or cropping.
- [x] Large round finials, flared collars and highlights remain stationary and recognizable at small and enlarged sizes in both themes. Native unit raster bounds measured 300 × 514, aspect 0.58366. The opaque-pixel upper-ball centroid and lower-ball center-row measurement give 25.288 degrees from vertical. Canonical endpoint centers give about 25.6 degrees; stripe translation uses the requested 26-degree axis. The canonical finial/collar comparison covers 67,800 pixels, including 19,146 opaque reference pixels, with a one-pixel curve-edge tolerance for native boolean-path rasterization.
- [x] Red/white/blue remain visible at phases 0, .25, .5 and 1. Only the stripe pattern translates along the pole's own axis. Color is clipped to an inset curved shaft. Metal, outer alpha, finials and collars do not move. Phase 1 equals phase 0 exactly. Near-wrap phases .001 and .999 pass continuity checks.
- [x] Existing two-second linear animator and loading state machine unchanged. Loading tags, public Loading setter/getter binding, host resume, hidden/offscreen/background/detach/pending-end behavior and live Remove Animations handling retained. No sound or artificial delay.
- [x] Search/Browse query parameters, pagination, list/footer relationship, navigation, pitch and quartet loader semantics unchanged. `TagQueryFragment.kt` and shared libraries were not edited. The other agent's iOS changes were not touched.

## Exact verification

JDK 17: `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`.
Commands run from `Android/`, with `set -euo pipefail`; logs are outside the repository.

1. `./gradlew :TagMaster:testDebugUnitTest --tests depollsoft.tagmaster.BarberPoleLoadingViewTest :TagMaster:assembleDebug :TagMaster:assembleDebugAndroidTest`
   - `/tmp/tagmaster-logo-pole/build.log`
   - Both APK builds succeeded. Four of five unit tests passed. The new reference-raster test initially failed because its reference did not use the artwork's uniform fit transform.
2. `./gradlew :TagMaster:testDebugUnitTest --tests depollsoft.tagmaster.BarberPoleLoadingViewTest.canonical_diagonal_silhouette_round_balls_and_highlights_are_preserved :TagMaster:assembleDebugAndroidTest`
   - `/tmp/tagmaster-logo-pole/geometry-fix.log`
   - Test APK rebuilt successfully. One test failed at a boolean-path antialiasing edge after the transform correction. The assertion now uses a bounded one-pixel canonical neighborhood, not a broad alpha tolerance.
3. `./gradlew :TagMaster:testDebugUnitTest --tests depollsoft.tagmaster.BarberPoleLoadingViewTest.canonical_diagonal_silhouette_round_balls_and_highlights_are_preserved`
   - `/tmp/tagmaster-logo-pole/geometry-final.log`: 1 passed.
4. `./gradlew :TagMaster:testDebugUnitTest --tests depollsoft.tagmaster.TagLoadingStateTest.initial_failure_retry_and_cancellation_stop_waiting`
   - `/tmp/tagmaster-logo-pole/cancellation.log`: 1 passed. This existing cancellation test belongs to detail loading and was not modified.

Six distinct unit tests passed across targeted runs: the five loader tests and the existing cancellation test. There were eight unit executions total, six passing and two failing during reference-test development. No unchanged passing test or full suite was rerun. The stripe test checks 15 phase comparisons across 68 × 116, 340 × 580 and deliberately wider 400 × 580 frames, including fixed alpha, shaft-mask confinement, colors and wrap continuity. Other passing loader tests cover lifecycle/Remove Animations, XML/reflection/accessibility and footer bounds.

Native command, once per configuration:

`adb -s emulator-5554 shell am instrument -w -r -e class depollsoft.tagmaster.BarberPoleQueryRegressionTest -e captureLabel LABEL depollsoft.tagmaster.test/androidx.test.runner.AndroidJUnitRunner`

- Light, motion 1.0: `/tmp/tagmaster-logo-pole/native-light.log`, 1 passed, 7.787s.
- Dark, motion 1.0: `/tmp/tagmaster-logo-pole/native-dark.log`, 1 passed, 6.495s.
- Light, motion 0: `/tmp/tagmaster-logo-pole/native-light-reduced-motion.log`, 1 passed, 4.623s.
- Total: 3 native test executions passed. Each covers held real Search requests, query parameters, pending pagination after a result row, failure/retry/completion, background/resume and pending offscreen Browse. Production networking has no timing changes.
- `/tmp/tagmaster-logo-pole/native-pixels.log`: 15 frame comparisons at actual 89 × 152 device pixels. Every comparison has zero changed pixels outside the transformed shaft mask and zero alpha changes. Phase .25 changes 2,083 interior pixels; .5 changes 1,052. Exact endpoint and reduced-motion natural samples change zero. Moving natural samples 500ms apart change 2,083.

Kotlin navigation/LSP extension calls were unavailable in this session. Kotlin edit checks, compiler, Robolectric native raster tests and Android instrumentation provided validation. `git diff --check` passed. Compiled `BarberPoleLogo.class` and companion were confirmed in `build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes/depollsoft/tagmaster/`. The normal app source set registers the new file automatically. XML loader type/ID, dimensions and `query_loading` resource were confirmed. No new resource registration is required.

## Visual evidence and bounded review

One native capture batch, one visual inspection, no visual correction or confirmation round needed. Android 16 emulator `emulator-5554`, 1080 × 2400, density 420, font scale 1.0. Not physical-device performance evidence.

- `.impeccable/review/tagmaster-android-logo-pole-review.jpg`: 800 × 780 contact sheet, inspected with a single Pi read.
- `.impeccable/review/tagmaster-android-logo-pole-{light,dark,light-reduced-motion}-comparison.jpg`: canonical reference beside phases 0/.25/.5/end, enlarged and 34 × 58 artwork, plus a crop of the actual held-query Search footer.
- Each configuration also has `search-pending`, `search-next-phase`, `search-pagination` and `browse-pending` JPEGs under the same prefix.
- 16 JPEG evidence files total, all dimensions at most 800px. Comparisons use Android native Canvas and the current pending view; context crops come from native screenshots. Reduced-motion columns intentionally remain at phase zero.

## Restoration and scope

`/tmp/tagmaster-logo-pole/native-batch.sh` backed up and restored the original production/test APKs and all private app files. Restoration also runs on batch failure. `restore.log` records successful reinstalls; an independent recursive comparison of extracted before/after private files passed. `app-before.apk` and `app-restored.apk` compare byte-for-byte equal. Animator duration scale, night mode and font scale compare equal to their backups: 1.0, yes and 1.0. Font scale was never changed. Only these fixtures' new external JPEGs were removed from the device after pulling them. No account or unrelated preferences were changed.

No commits, pushes, tool configuration changes or edits to watermark assets.
