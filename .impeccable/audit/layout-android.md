# Android layout pass

A1 and M3 are implemented in the original four production files under `Android/TagMaster`. The known Open Tag, query-final-row and landscape-part failures are now resolved as fixture coordinate/readiness issues, with native evidence below. Settings continuation also passes. The final focused set passed 12 tests; this does not replace the earlier screen-coverage limits or certify an unrun full suite. The repair changed only `LayoutRegressionTest.kt` and this report, preserving the existing production fixes, cleanup test and footer formatter diff.

## Changes

- `src/main/java/depollsoft/tagmaster/TagTracksFragment.kt`: a retained UI binding observes the loaded tag's playable collection. A loaded empty collection shows the existing explanation and hides the player and part group. A null tag, no selection, pending download, failure, pause and retry do not mean empty. On populated-to-empty replacement, selection and radio selection clear, the player's remote location resets and its existing stop path runs **before** hiding transport. The binding is released with the fragment view. `MediaPlayerView` is unchanged.
- `src/main/res/layout/tagtracksview.xml` and `layout-land/tagtracksview.xml`: move the existing empty explanation ahead of recording notes. Keep notes when supplied, the existing typography, scroller, populated transport and all nine part choices. The landscape notes column expands when the parts group disappears.
- `src/main/res/layout/tagdetailview.xml`: retain `detailErrorState`, `detailErrorText` and `detailRetryButton`, but place error content in a fill-viewport ScrollView. Center when it fits and allow scrolling when it does not. No activity, retry, initial-loading composition or loaded-refresh Snackbar logic changed.

No shared helper, audio implementation, art, palette, footer, tab, signing, config, persistence, API or backend changes. No iOS/Pitch Perfect/third-party AuthUI edits. No commits or pushes.

## Acceptance ledger and pre-edit probe

| Check | Evidence / outcome |
| --- | --- |
| Confirmed-empty only | Zero tracks with/without notes, one track, all parts/extras, null selection and null host tag asserted through the actual activity/fragment. Complete state test passed phone dark 1.0, phone light 2.0 and expanded light 1.0. |
| Empty has no transport focus stops | Player/parts GONE and play/balance absent from focusables asserted in the same tests. Existing explanation precedes the recording notes bounds. |
| Cancel before hiding | Four native runs passed held-download failure/retry, populated-to-empty cancellation, late completion rejection, silent WAV playback, pause/resume, active-to-empty cancellation and background/return. Selection, remote location, radio check, loading and playing flags are asserted. No shared audio changes. |
| Full player returns | Those runs retain timeline, seek, balance, play/stop and all available standard/extra part views. Empty-to-populated replacement restores them. |
| Short initial error | Before editing: font 2.0 at normal landscape and 640×320dp fit. At 640×240dp the actual failed request failed `All error text is laid out`. That reproduction authorized M3. |
| Error text and Retry reachable | After editing, initial-error test passed all four main configurations plus 640×240dp/font 2.0. It checks full text layout, scroll-to-Retry, scroll-to-message-start, retry transition to loaded, and native Back leaving foreground. A root Android task can background on Back rather than become DESTROYED; the baseline test was corrected accordingly. |
| Loaded refresh preserved | Existing `TagLoadingRegressionTest#failure_retry_and_failed_refresh_keep_useful_content` passed in the final selected run. |
| Footer and tabs | All four Browse selections passed equal-width/summed-full-width tab and visible-bounds assertions in the first batch. Long saved-row title and overflow-target assertions passed. The confirmation passed Home/Teachable geometry and final footer link reach in all four configurations. No footer code changed. |
| Pitch and sheet face | Existing summary and sheet note lifecycle/render tests passed, including blue held/toggle state and opaque sheet face. Both `SheetKeySurfaceTest` light/dark unit tests passed. |
| Known failures resolved | Final focused native checks pass Open Tag validation/Cancel and query-final-row reach in all four existing configurations, plus all nine parts and full-player control reach in landscape/font 2.0. Settings cache/theme/changelog geometry also passes. Exact fixture causes, fullscreen-IME behavior and logs are documented in the repair supplement below. |

## Screen matrix

`A(suffix)` below means `.impeccable/review/tagmaster-layout-after-android-<suffix>.jpg`. Captures use the current built application, not the previously installed APK. Tests measure actual controllers and view bounds. Query/saved-list data is injected at existing model/adapter/cache boundaries, not persisted into live favorites or teachable lists.

| Screen family | Status | Evidence and limits |
| --- | --- | --- |
| Home zero/one favorites | Verified unchanged from completed assessment | Existing current-artifact evidence in `layout-assessment.md`; no new live-list mutation. |
| Home many/long titles | Verified unchanged | Fifteen disk-fixture tags, separate adapter ID source, production saved rows. Four configurations passed text/48dp overflow-target bounds. `A(phone-light-large-home-many)`. |
| Home footer | Verified unchanged | Final link reachable in four confirmation runs. `A(confirm-phone-dark-home-footer)` shows complete compact legal/credit group. Earlier `home-footer` captures were partial scroll positions, not whole-footer proof. |
| Browse Latest/Rating/Downloads/Classic | Verified unchanged for selected modes | All four selections rendered in all four configurations; sort identity, row text and equal tabs asserted. `A(expanded-light-browse-classic)` and other mode files. No live-query/persistence changes. |
| Search form/filters/keyboard | Verified unchanged from prior assessment | Prior portrait IME and rotated form evidence retained. Rating capture includes the current Search form. No new landscape active-IME certification. |
| Search empty/error | Verified unchanged for fixture content | Long status text fits and is visible in all four configurations. `A(phone-light-large-search-error)`, `A(phone-dark-search-empty)`. Error prose is test data, not changed product copy. |
| Search results/pagination | Verified unchanged for fixture pagination | Existing inline-loading capture retained. Final checks now wait for the 17-item adapter, completed layout and hidden loading host before selecting row 16; its exact title, complete text and full bounds pass in all four configurations. Network pagination/retry journey remains unexercised. |
| Teachable empty | Verified unchanged from prior assessment | Existing empty-screen evidence; no saved-list writes. |
| Teachable populated/reorder geometry | Verified unchanged | Fifteen local IDs feed the real adapter/rows; reorder changes that isolated ID list only. Four initial and four confirmation geometry runs passed. `A(landscape-dark-large-teachable-many)`, `A(confirm-expanded-light-teachable-reordered)`. Actual persistence/context-menu reorder not invoked. |
| Summary ordinary/missing materials | Verified unchanged | Completed assessment evidence plus passing current summary note lifecycle test. No additional long-prose fixture. |
| Details default/font 2.0 | Verified unchanged from prior assessment | Existing Android metadata evidence retained; no metadata-layout edit. Narrow 320dp long-name combinations not newly rendered. |
| Initial tag loading/error/retry | Fixed M3 | Actual held requests and real failure copy. `A(short-error-initial-error-bottom)` is the scrolled bottom, not the whole message at once. Message start/end and Retry are separately asserted. |
| Loaded refresh failure | Verified unchanged | Existing regression passed; useful loaded content and Snackbar Retry retained. |
| Tracks empty/no notes/long notes | Fixed A1 | `A(phone-dark-empty-tracks-notes)`, `A(phone-light-large-empty-tracks-notes)`, `A(landscape-dark-large-empty-tracks-notes)`. Empty player focus stops removed. |
| Tracks populated/all/extra/no selection | Fixed A1 state boundary; landscape reach verified | Original default/large phone/expanded results retained. Final landscape/font 2.0 native test selects Other 4, then individually scrolls to and checks every standard/extra part and play, stop, timeline and balance controls. Unequal-height columns require target-specific scrolling, not page-bottom scrolling. |
| Tracks pending/failure/retry/playing/paused/replacement/return | Verified preserved | Four passing silent-media runs. `A(phone-dark-tracks-pending)` shows retained full controls. Pending cancellation and actual silent playback cancellation are both asserted. |
| Videos empty | Verified unchanged from prior assessment | Existing no-video state retained. |
| Videos populated/long/missing metadata | Verified unchanged for user submissions | Four passing controller tests. `A(phone-light-large-videos-long)`. Long singer and key wrap; missing key/date rows collapse. No video destination opened. Populated teaching-video header not rendered. |
| Sheet music/key/rotation | Partially verified unchanged | Current sheet-key lifecycle/render test passed with a local image. No actual score/zoom/rotation capture in this pass; aspect-correct letterboxing is not treated as a defect. |
| Rating dialog/pending | Verified unchanged for chooser | Native chooser rendered and cancelled in four configurations, e.g. `A(phone-light-large-rating-dialog)`. No rating submitted. Pending-rating state relies on prior assessment evidence. |
| Open Tag validation | Verified native behavior in all four configurations | Corrected local-viewport comparison includes TextView scroll offsets. Full viewport, complete validation/labels, ancestor clips, minimum-48dp targets, native Cancel dismissal and no tag request pass. Portrait/expanded targets remain above the actual IME. Landscape uses Android fullscreen extraction; native Back returns to the preserved dialog before Cancel. No claim that Cancel is visible inside the fullscreen system editor. |
| Settings upper/account entry | Verified unchanged from prior/source evidence | Existing login entry retained. Source launches third-party AuthUI directly, so no separate app-owned login dialog was available to inspect. No sign-in/logout action. |
| Settings lower/cache/theme/changelog | Verified native geometry in all four configurations | Await cache-size completion; individually scroll to Clear Cache, all three theme controls and Changelog; assert complete labels and at least 48dp reachable targets. Open and dismiss the actual changelog dialog. No cache clear, theme selection, auth or saved-list action. Private diagnostics remain source-only. |
| Settings private build | Not rendered | Normal debug build hides the branch. Source has a wrapping metadata label, 8dp related gap, 24dp section start and full-width minimum-48dp Copy Logs button inside the scroller. No private config or logs touched. |

## Initial pass commands, counts and limits

All Gradle commands used `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` from `Android/`. Logs and scripts remain outside the repository at `/tmp/tagmaster-layout-android`.

- `./gradlew :TagMaster:assembleDebug :TagMaster:assembleDebugAndroidTest`: passed for unchanged baseline/probe and implemented source. Later Android-test-only builds passed after fixture/export changes.
- `./gradlew :TagMaster:testDebugUnitTest`: **single full run, 300 tests in 33 suites, zero failures/errors/skips**. Production source did not change after this run.
- Direct `adb -s emulator-5554 install -r -t -d …`, never Gradle install/uninstall. `-d` was needed because the branch debug code is 260907001 and the original installed code was 260912001. The first attempt without it failed safely.
- Native first batch: seven new controller tests × four configurations, plus the short-window error test. **29 completed, 20 passed, 9 failed.** Failures were Open Tag fixture/visibility checks, premature final-query selection, and one landscape extra-part reach check. No all-pass claim.
- Configurations: 1080×2400/420dpi portrait dark/font 1.0; portrait light/font 2.0; rotated phone dark/font 2.0; 1600×2560/420dpi expanded light/font 1.0. Short error at rotated 630×1680/420dpi, font 2.0. This is emulator coverage, not physical-device/TalkBack certification.
- First batch reached two passing existing tag-loading tests, then stalled in `pending_background_hidden_and_detached_stop_motion`; the outer 600s tool deadline interrupted the shell before its EXIT trap completed. Captures were recovered and explicit restoration completed. That existing test is not reported as passed.
- One visual confirmation batch: Home/Teachable/footer test passed in each configuration. Open Tag Cancel visibility failed in each; later tests stalled and each invocation was stopped by a 140s guard. The complete initial matrix was not rerun.
- A separate selected preservation run passed **3 tests**: loaded-refresh retry, summary note lifecycle/rendering, sheet note lifecycle/rendering.
- Test-helper fixes changed global-screen comparison to view-local visibility and added unconditional dialog dismissal. Two final **no-capture behavioral attempts** did not resolve the Open Tag assertion/blocked landscape-query execution. No further visual polishing or production changes followed.
- Recovery/export initial-error invocation passed once. `LayoutCaptureCleanupTest` passed once, verifies exported JPEG bytes and explicitly verifies the originally empty external-files directory after removing generated captures.
- **91 JPEG captures** retained, all named `tagmaster-layout-after-android-*.jpg`; native renderer scales the maximum dimension to 800px. Fifteen representative full screens were individually inspected using unchanged `pi.read` results. Captured does not mean every file/all text was visually inspected. No new validation/settings capture is claimed.
- Both new test classes are present in the built test APK DEX. They use standard `src/androidTest/java` discovery; no new resources, IDs, manifest entries or source-set configuration.
- LSP/ast-grep were advertised but absent from this worker's tool registry. Compiler, existing XML compilation, unit tests and native assertions supplied evidence. No independent detector rerun; main owns the final scan.

## Deterministic repair supplement

### Failure causes and final disposition

- **Open Tag, `Cancel stays reachable`: fixture coordinate error, not clipped transparent insets or a production dialog defect.** The default native Cancel screen rectangle was `(558,947)-(737,1073)`, a 179×126px target at density 2.625, exactly 48dp tall. `getLocalVisibleRect` returned `(524230,0)-(524409,126)` because the native TextView's own `scrollX` was **524230**. Large-font scrollX was 524182. The fixture had compared drawing coordinates with a zero-origin viewport. `fullyVisible` now compares against `(scrollX,scrollY)-(scrollX+width,scrollY+height)`. The original full-visibility assertion is retained, with independent screen-space ancestor clipping, complete text/label, minimum-target and native pointer checks. The test verifies the field starts empty, overflow validation remains, Cancel dismisses the owned dialog, Home survives and no tag request occurs. Open's target is measured; a valid-ID Open journey is not newly claimed.
- **Landscape IME overlap: native fullscreen editor, not dialog cropping.** Focused evidence showed the system numeric extract editor occupying `(136,74)-(2400,1080)`, covering the entire app. The test detects `InputMethodManager.isFullscreenMode`, sends native Back and waits for that IME window to disappear. The validation explanation and dialog remain; Cancel's `(1437,734)-(1712,877)` target then passes full visibility and native dismissal. Portrait and expanded checks retain the active IME and assert no target overlap. No `MeHeaderView`, `dialog_open_tag`, window-mode or input-option production change was needed.
- **Query final row:** adapter replacement/layout readiness raced `setSelection(16)`. The fixture now waits for count 17, no pending layout and the loading host hidden. It verifies final position, exact final-row title, full text and fully reachable row in all four configurations. Existing inline-loading and Browse assertions remain.
- **Landscape extra parts:** scrolling to the page bottom follows the taller notes/player column and can move the shorter part column above the viewport. Target-specific scrolling was already present in the handoff. Final verification now completes and additionally checks each of the nine part choices and all four player controls individually. Other 4 selection still asserts the selected track. No player dimensions or behavior changed.
- **Settings continuation:** a newly reached `NoMatchingViewException` searched the Settings activity for `android:id/button1` during changelog window-focus handoff. Selecting `RootMatchers.isDialog()` fixes the fixture race. The actual dialog button's label/target and click remain asserted. Cache-size readiness and Clear Cache/theme/changelog target geometry now execute without invoking destructive or persistence actions.

### Final focused evidence

All paths in this subsection are under `/tmp/tagmaster-layout-android`. Final-artifact native results total **12 tests, 12 passed, no failures or timeouts**:

| Log | Explicit cases | Result |
| --- | --- | --- |
| `repair-coordinate-default.log` | Owned dialogs/Settings + query, phone dark/font 1.0 | 2 passed |
| `repair-coordinate-large.log` | Same two cases, phone light/font 2.0 | 2 passed |
| `repair-coordinate-expanded.log` | Same two cases, 1600×2560 light/font 1.0 | 2 passed |
| `repair-coordinate-landscape.log` | Same two cases + empty/populated/all-parts/full-player reach, landscape dark/font 2.0 | 3 passed |
| `repair-preservation-final.log` | Held request/error/retry + real silent-WAV pending/failure/retry/play/pause/empty cancellation and lifecycle return | 2 passed |
| `repair-cleanup.log` | Export byte comparison, remove only generated captures, assert original external-files directory empty | 1 passed |

`repair-native-geometry.log` records actual local/screen rectangles, TextView scroll values and IME windows. `repair-dialog-probe.log` preserves the original coordinate failure. `repair-dialog-landscape-evidence.log` preserves the fullscreen-IME overlap failure. `repair-dialog-landscape-final.log` preserves the subsequently reached changelog-root failure; `repair-coordinate-landscape.log` supersedes it with all three cases passing. Earlier passing diagnostic runs were not substituted for these final results.

Only two targeted defect JPEGs were inspected, each 800×360, both outside the repository: `repair-landscape-ime.jpg` shows the fullscreen editor; `repair-landscape-return.jpg` shows validation and Cancel after native Back. No new aesthetic review or screen-capture batch ran. The initial 91-image record above remains historical; no new Settings screenshot is claimed.

`repair-run.sh` invokes explicit methods with a 100-second process guard and checks both adb exit status and instrumentation `OK`. Geometry-only invocations temporarily set window/transition/animator scales to 0 and restore the recorded **1.0/1.0/1.0** on exit. Earlier idle stalls did not recur. These geometry runs do **not** certify animations. `LAYOUT_GEOMETRY=false` verifies original scales before running the separate actual-request/playback cases. Fixed `Thread.sleep(200)` was replaced by a native Choreographer frame barrier; data/IME waits poll actual conditions with bounded deadlines.

`:TagMaster:assembleDebugAndroidTest` passed after the final fixture edit, JDK 17, log `repair-coordinate-build.log`. Production did not change in this repair, so the existing 300-pass unit run was not repeated. Source discovery confirms seven `LayoutRegressionTest` methods and one cleanup method under the standard androidTest source tree. Both class descriptors are in the built test APK DEX, recorded in `repair-dex-registration.txt`. No new registrations/configuration are required. Kotlin compiler/edit diagnostics and `git diff --check -- Android/TagMaster` pass. LSP/ast-grep extension calls were unavailable in the discovered registry; main still owns the final scan.

### Repair preservation and remaining limits

Before installing, stopped both packages and made a fresh `repair-original/` backup. Both APKs and private-file SHA-256 manifests matched the retained original backups; archives extracted successfully and private archives matched second reads byte-for-byte. The first backup attempt correctly stopped because the original test package's external directory does **not exist**. This was explicitly verified and recorded as `external-absent` before installation; the script now handles absence instead of treating an adb error string as a tar archive. The app's external directory was verified empty. Original and fresh backups are retained; no credentials were inspected.

`repair-cleanup.log` passes before restore. `repair-restore.log` verifies original app/test APK bytes, both private-file manifests, app external-file manifest, absent test external directory, original display/density/font/night/rotation settings and all three animation scales. Originals are installed again. The earlier restore's hardcoded display settings were checked against the fresh snapshot before reuse. Final footer SHA-256 still matches `1a8eaaf80d95edc5318bbce54cc5e649a6199dab7e279d398d95820499fea9dd`; its preexisting formatter diff is preserved. No uninstall/reset, account action, cache clear, live favorite/rating/cloud write, shared/iOS edit, commit or push occurred.

No known Open Tag/query/landscape failure remains unexplained. Fullscreen landscape input requires the native Back step; Cancel is not falsely certified visible under that system editor. Private Settings remains source-only. Network pagination, physical-device/TalkBack behavior and the prior uncompleted loading-motion case remain outside this focused repair's claims. Existing screen-specific limits above remain in force.

## Data preservation and final scope

Before any install, stopped app/test processes; recorded settings and both installed APKs; archived private/external data; extracted archives and generated SHA-256 file manifests; byte-compared a second private-data archive. Original app private data has 19 files. Original test private and both external file manifests were empty. Original backups are retained.

Restoration obstacles were recorded rather than hidden: external storage rejects directory timestamps, so extraction uses `tar -m`; scoped storage denied adb/run-as capture reads, so test teardown exports through app permissions to private cache. An intermediate restore log said VERIFIED even though a generated `quartet-frames` directory was unreadable to shell. That intermediate claim is superseded: `LayoutCaptureCleanupTest` removed only the generated capture artifacts through app permissions and asserted the original external directory was empty. The restore script now stops on any external removal error. Final `verified-final-restore.log` is clean, and both APK byte comparisons, all private/external file manifests and the recorded settings match. No app uninstall, account/favorite/teachable/rating/share action, reset or user-data wipe.

Final settings: 1080×2400, density 420, font 1.0, night yes, accelerometer rotation 1, user rotation 0. Emulator root was unavailable; no root setting changed.

The pre-existing `FooterPitchRegressionTest.kt` was read in full before work and remains byte-identical: SHA-256 `1a8eaaf80d95edc5318bbce54cc5e649a6199dab7e279d398d95820499fea9dd`. Its original formatter-only diff byte-compares unchanged. `git diff --check -- Android/TagMaster` passed. Sheet-key resources, compact footer, four full-width equal tabs, blue pitch state, charcoal navigation and shared artwork/motion remain unchanged.
