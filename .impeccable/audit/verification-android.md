# Tag Master Android verification

Date: 2026-09-07. Astra integration of the existing four-worker implementation. No commits, deployment, signing/credential changes, or iOS edits.

## Outcome

Tag Master and Pitch Perfect debug builds pass. **262 unit tests across 23 classes pass**, with zero failures, errors or skips. Four focused native flow tests pass. The real two-page PDF regression passed in the full device suite.

The full connected suite is **not green**: 101 tests, 55 passed, 46 failed, zero skips/errors. All failures are NoMatchingViewException: 34 for viewPager, 12 for tabLayout. Source inspection confirms these tests launch MeActivity, whose existing HEAD layout has neither a pager nor bottom navigation. No failures were hidden, skipped, or fixed by adding phantom views. Other legacy tests sometimes swallow exceptions, so their passing count is not comprehensive UI assurance.

## Acceptance checks

- **Routing:** UrlHandlerActivity uses exact hosts barbershoptags.com, <www.barbershoptags.com>, tags.depoll.com; HTTP/HTTPS only; positive Int IDs. Tests cover null URI/host, opaque/relative URI, missing/empty pg/dbase, unsupported scheme/host, lookalike hosts, malformed/zero/negative/overflow IDs and both schemes for all three hosts. Other web pages retain browser fallback. The shared browser uses an explicit internal trampoline.
- **Pitch:** Tests cover timed accessibility click, touch down/up without retrigger, cancel, replacement during hold, detach during hold/timed click, touch toggle and accessibility toggle. Only notes activated by the button are stopped. Public getters/setters remain compatible. Pitch Perfect builds.
- **Dates/cache:** Feed weekday dates, one-digit days, milliseconds including pre-epoch, ISO, US and absent/invalid values tested. Absent dates remain absent. Background disk loading, immediate memory cache, disk-to-memory reuse and missing-directory handling tested. Cache version, API parameters, share URL and persistence keys retained.
- **Tag.kt scope:** Reviewed against HEAD and removed unrelated import, branch and method-formatting churn. Final git diff -w: 27 added / 30 removed lines. Original CRLF retained. Android/.pi-lens.json disables automatic mutation only, not diagnostics, because automatic ktlint repeatedly expanded this file to roughly 546 changed lines.
- **PDF:** PdfPagesRegressionTest generates a red page then a blue page and checks real PdfRenderer output dimensions and pixels. Passed. Size guard and external fallback remain.
- **Media:** Six unit tests cover asynchronous prepare, pause/resume/stop controls, stop during download, replacement, detach/release and late prepared callbacks. Stop works during loading/pause. Leaving Tracks stops playback in onPause.
- **Forms/navigation:** Native tests cover visible Teachable Tags, invalid Open Tag feedback, actual sort dropdown selection, query/filter retention across recreation, rating cancellation and explicit selection. No remote rating submitted. Toolbar/Up, tabs, settings and keyboard paths inspected.
- **Visual blockers:** Theme-resolved menu icon tints restore light-mode visibility, with unit coverage for all toolbar menus. Scrollable tabs prevent large-text clipping. The entire Tracks path scrolls so landscape users can reach Bass; unit and native tests pass.
- **Real paths:** Explicit HTTPS links from tags.depoll.com and <www.barbershoptags.com> opened tag 1809, Lost. Lead playback visibly reached 4.7/43.5 seconds. Switching tabs returned Play at position 0.0. Searching love returned populated catalog rows with dates and material indicators.

## Commands, counts and logs

All Gradle commands ran from Android with JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home.

- Baseline: ./gradlew :TagMaster:assembleDebug :TagMaster:testDebugUnitTest :PitchPerfect:assembleDebug, exit 0. /tmp/tagmaster-android-integration-build.log.
- Final: ./gradlew :TagMaster:testDebugUnitTest :TagMaster:assembleDebugAndroidTest :TagMaster:assembleDebug :PitchPerfect:assembleDebug, exit 0. /tmp/tagmaster-android-verified-final.log.
- Kotlin and Java unit compilation tasks actually ran. XML results for 23 classes exist under Android/TagMaster/build/test-results/testDebugUnitTest/. No test task was NO-SOURCE. Final 262 tests, zero failures/errors/skips.
- Full device suite: ANDROID_SERIAL=emulator-5554 ./gradlew :TagMaster:connectedDebugAndroidTest, timeout 1500 seconds. One runnable full suite, exit 1 after about 85 seconds. /tmp/tagmaster-android-connected-run.log. An earlier invocation stopped at compilation before device execution, /tmp/tagmaster-android-connected.log.
- Full XML: Android/TagMaster/build/outputs/androidTest-results/connected/debug/TEST-MonkeySSH_IME_API36(AVD) - 16-_TagMaster-.xml. FavoritesFlowTest 23/23; MeActivityTest 2/22; PdfPagesRegressionTest 1/1; TagDetailActivityTest 21/31; TagSearchActivityTest 8/24.
- Focused final: adb -s emulator-5554 shell am instrument -w -r -e class depollsoft.tagmaster.PolishFlowRegressionTest depollsoft.tagmaster.test/androidx.test.runner.AndroidJUnitRunner. /tmp/tagmaster-native-verified.log: OK (4 tests). Wrapper explicitly required that success string because adb exit alone does not establish test success. The unchanged failing legacy suite was not repeated.
- git -c core.whitespace=cr-at-eol diff --check -- Android: exit 0. Plain diff-check flags CR characters in added lines of preserved CRLF Tag.kt.
- /tmp/tagmaster-runtime-errors.log: zero AndroidRuntime error lines in the retained final review buffer, not a hardware/performance certification.
- Targeted LSP was attempted but unavailable through this session's registered Fabric actions. Used Gradle diagnostics and executable behavioral checks.

### Failures encountered and resolved

- Resource-backed Robolectric tests first exposed ten class-setup failures because SDK 36 requires Java 21. Pinning SDK 28 exposed 130 production Firebase initialization failures in formerly resource-less tests. Test-only robolectric.properties now selects SDK 28 and plain Application under required Java 17. Final 262/262 pass.
- New PDF test initially treated PdfDocument as Closeable. Fixed explicit try/finally close before device execution.
- Initial three native probes failed due synthetic dropdown selection, keyboard dismissal closing a dialog on this emulator, and unlike-production dialog reuse. Switched to actual dropdown selection, kept ID dialog open and created a fresh rating dialog each time. Three probes then passed.
- New layout unit test incorrectly asserted RadioButton.performClick's listener-return flag. Kept scroll and checked-state assertions instead.
- First four-test native probe failed two tests during asynchronous orientation transitions. Allowed orientation and scrolling to settle before interaction. Final four tests pass with the same behavioral assertions.
- A uiautomator dump could not idle during audio playback. Its stale hierarchy was not playback evidence. The inspected native image shows 4.7/43.5 seconds; the subsequent idle hierarchy shows the post-tab-switch reset.

## Inspected native captures

Each capture came from adb exec-out screencap, then sips -Z 900 into /tmp before one-image-at-a-time pi.read. One inspection batch, one repair batch, one confirmation batch. Mislabeled intermediate settings captures were removed.

All paths below start with .impeccable/review/:

- tagmaster-android-after-home-confirmed.png
- tagmaster-android-after-home-dark-large.png
- tagmaster-android-after-home-expanded.png
- tagmaster-android-after-search.png
- tagmaster-android-after-search-dark-large-confirmed.png
- tagmaster-android-after-search-keyboard-dark-large.png
- tagmaster-android-after-search-results-confirmed.png
- tagmaster-android-after-detail-confirmed.png
- tagmaster-android-after-tracks-playing-confirmed.png
- tagmaster-android-after-tracks-dark-large-confirmed.png
- tagmaster-android-after-tracks-landscape-confirmed.png
- tagmaster-android-after-tracks-expanded-confirmed.png
- tagmaster-android-after-settings-filters-confirmed.png
- tagmaster-android-after-settings-dark-large-confirmed.png

Phone: Android 16 emulator, physical 1080x2400, density 420. Large text used font_scale 1.3. Expanded checks used the same emulator with a 1600x2560 / density 240 override. Home and Tracks were inspected at that override. This is not verification of separate tablet hardware, every expanded screen, foldable posture or hardware gestures/performance. Expanded content remains full-width; split pane, rail and capped-width refinement remain deferred.

## Device restoration

Recorded before changes and restored afterward:

- font_scale 1.0; night mode yes
- accelerometer_rotation 1; user_rotation 0
- physical 1080x2400 / density 420, no overrides
- window_animation_scale 1.0; transition_animation_scale 1.0; animator_duration_scale absent/null

Readback: /tmp/tagmaster-device-restored.log. The Gradle connected runner removed the app at cleanup; reinstalled for manual review. No manual account/list reset performed. Only the two synthetic PDF/landscape test cache files were removed afterward.

## Deferrals and release dependencies

- Legacy Espresso needs separate navigation/fixture cleanup; its 46 failures remain visible.
- RecyclerView, dynamic color, split-pane/navigation rail and expanded max-width layouts deferred. No architecture rewrite.
- LoadingImageView retains bounded workers and cache/resize improvements, but the pending queue is unbounded. Queue redesign deferred.
- Optional account/sync code and persistence contracts retained. Signed-in login, deletion, cross-device sync and network-denied cold start were not reverified end to end. Local disk-cache regression passed; real playback populated the existing media cache.
- Accessibility names/targets and non-touch activation inspected/tested. No full TalkBack session or hardware contrast/performance certification.
- No association files published or verified. App Links release requires valid hosted Digital Asset Links with release signing fingerprints at the first-party and BarbershopTags hosts, followed by verification tests. Local routing and autoVerify entries are not verified App Links.

## Native navigation test repair, 2026-09-07

This follow-up supersedes the 46 wrong-screen failures above. Changes are limited to `Android/TagMaster/src/androidTest/` and additions to these audit logs. No production/config/iOS edits, screenshots, commits, or repeated unit/build checks. Connected runs compiled the test APK as needed.

### Acceptance ledger

- [x] Traced `MeActivity` and `MeHeaderView` before editing. Home has explicit Search, Browse, Teachable Tags and Settings routes, not a pager. App metadata belongs to the Home footer. Tests use those real routes and exercise Browse tab selection and Up/Back.
- [x] Search tests launch `TagSearchActivity`, select actual Material dropdown entries, submit with the button and IME, verify filters/query in `TagSearchResultsActivity`, retain query/collection across recreation, render bound result rows, and open the matching Detail tag.
- [x] Detail tests launch `TagDetailActivity` with `TAG_ID_EXTRA`. A synchronous serialized disk-cache fixture exercises the real `loadTagById` path. Tests verify Summary/Details values, video row/preview, all five part selections and player URI bindings, real pager swipes, tab/page agreement, and selection after recreation. Pager waits are condition-based with a five-second failure deadline, not arbitrary sleeps.
- [x] Retained 22 Home, 24 Search and 31 Detail tests. Removed swallowed screen assertions and tautological visibility checks from those classes. The only optional catch there dismisses the documented first-run changelog. No fake views, skipped tests, or production test hooks added.
- [x] `NavigationTestFixture.kt` supplies data through existing cache/model contracts. Search result fixtures replace only the fragment's model with a populated, non-paginating model; existing production fragments/adapters/navigation render it. Background requests on the old model cannot alter fixture rows. Remote query results, audio downloads and thumbnail pixels are not prerequisites for these assertions. Fixture cache bytes are restored or deleted after each test.
- [x] Inspected each failed run and repaired fixture/root mismatches. No failure identified a production bug requiring an out-of-scope edit.
- [x] Preserved and restored the installed APK, all private app data and original device settings. No account, list or cache reset performed on the user's saved data.

### Exact runs and evidence

All commands ran from `Android` with `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and `ANDROID_SERIAL=emulator-5554`. Every connected invocation used a 1500-second timeout and `settle:true`.

1. `./gradlew :TagMaster:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=depollsoft.tagmaster.MeActivityTest,depollsoft.tagmaster.TagSearchActivityTest,depollsoft.tagmaster.TagDetailActivityTest`
   - Exit 1. **77 tests: 74 passed, 3 failed, 0 errors/skips.** `/tmp/tagmaster-navigation-targeted-1.log`.
   - Two Home checks incorrectly required a nonzero-height Favorites container when its adapter was empty. Replaced with adapter/child counts against saved IDs and verified those IDs across recreation. The video preview ID also belonged to the hidden teaching-video view; scoped the assertion to the actual submissions list.
   - XML preserved in `/tmp/tagmaster-navigation-targeted-1-xml/`.
2. `./gradlew :TagMaster:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=depollsoft.tagmaster.MeActivityTest#testFavoritesItemsControlExists,depollsoft.tagmaster.MeActivityTest#testActivitySurvivesRecreation,depollsoft.tagmaster.TagDetailActivityTest#testVideoPreviewExists`
   - Exit 0. **3/3 passed**, 0 errors/skips. `/tmp/tagmaster-navigation-targeted-2.log`; XML in `/tmp/tagmaster-navigation-targeted-2-xml/`.
3. `./gradlew :TagMaster:connectedDebugAndroidTest`
   - One full integration rerun, justified by the repaired cross-activity flows. Exit 1. **105 tests: 102 passed, 3 failed, 0 errors/skips.** `/tmp/tagmaster-navigation-connected-final.log`.
   - Home **22/22**, Search **24/24**, Detail **31/31**, PDF **1/1**, Favorites **21/23**, Polish **3/4**. The count now includes the four previously focused Polish tests.
   - Full XML preserved in `/tmp/tagmaster-navigation-connected-final-xml/TEST-MonkeySSH_IME_API36(AVD) - 16-_TagMaster-.xml` before subsequent targeted execution overwrote Gradle's results directory.
   - Remaining failures were unrelated test isolation assumptions: Favorites launched into the first-install changelog, another Favorites test expected a visible row without supplying saved data, and the rating probe matched the underlying activity before the fresh dialog became the selected root.
4. `./gradlew :TagMaster:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=depollsoft.tagmaster.FavoritesFlowTest,depollsoft.tagmaster.PolishFlowRegressionTest`
   - After test-only isolation repairs, exit 0. **27/27 passed**, 0 errors/skips. Favorites **23/23**, Polish **4/4**. `/tmp/tagmaster-navigation-isolation-final.log`; XML in `/tmp/tagmaster-navigation-isolation-final-xml/` and `Android/TagMaster/build/outputs/androidTest-results/connected/debug/`.
   - Favorites now handles the optional first-run prompt on every launch. Its populated-list visibility test uses a cached tag and a temporary in-memory favorites collection, suppresses disk/Firebase storage with existing test mode, and restores the original collection in `finally`. The rating probe explicitly selects dialog roots and checks cancellation dismissal before reopening; its fixed launch delay was removed.

The three requested classes pass all **77 tests in the full run**. All **105 current tests have passing results across the full and final targeted runs**, but there is **no single 105/105 full-run result**. The one full run remains recorded as 102/105; it was not repeated after the final isolation fixes. Older unmodified Favorites tests still contain permissive assertions, so their passing count is not a comprehensive Favorites audit.

`git diff --check -- Android/TagMaster/src/androidTest` passed. Test annotations mechanically confirmed at 22/24/31/23/4 with no tests deleted. LSP navigation/diagnostics were attempted but the actions were unavailable in this session; Kotlin compilation and connected execution supplied diagnostics. Existing 262 passing unit tests were not rerun.

### Device restoration evidence

The connected runner uninstalled the app during cleanup. Before the first run, saved the installed APK and complete private app directory to `/tmp/tagmaster-navigation-backup/`. After the final run, reinstalled that exact original APK and restored its private directory without launching the app. Recursive file comparison against the pre-run archive passed with no differences, `/tmp/tagmaster-navigation-backup/data-diff.txt` is empty. Restore command log: `/tmp/tagmaster-navigation-restore.log`.

Settings readback exactly matches the pre-run capture: font scale 1.0, night mode yes, accelerometer rotation 1, user rotation 0, physical 1080x2400 at density 420 without overrides, window/transition animation scales 1.0, animator duration scale absent/null. Evidence: `/tmp/tagmaster-navigation-backup/settings.txt` and `settings-restored.txt`, diff exit 0. Original APK/data archives remain available locally for recovery.
