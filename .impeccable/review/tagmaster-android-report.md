# Tag Master Android refresh verification

Verified 2026-09-07. This continues the existing singing-desk implementation after the image transport interruption. It does not restart the design audit. Main still owns the fresh final design review and design documentation.

## Scope and environment

- First command was `git rev-parse --show-toplevel`. Exact result: `/Users/depoll/.local/share/pi-worktrees/20260907005637/tm-refine-worktree-20260907`.
- Changes in this continuation are confined to `Android/TagMaster`, this report, and Android screenshots. No commits. iOS, shared Android modules, storage models, preference keys, synchronization and migrations were not edited.
- Existing charcoal/blue character, handwriting wordmark, icon and barber pole retained. Home/Browse/Search remain the selected structure. Settings stays an app-bar utility.
- Java: `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`.
- Android SDK: `/opt/homebrew/share/android-commandlinetools`. Native device: `emulator-5554`, actual Android API36.
- Phone: 1080x2400 physical pixels, density 420, system dark theme, font scale 1.5.
- Tablet evidence is an explicit runtime size simulation on that same emulator: `wm size 1600x2200`, `wm density 320`, giving 800dp available width. It uses an actual `NavigationRailView`, not a stretched phone screenshot or separate tablet AVD. Light theme and font scale 1.0 for this confirmation. Size/density overrides were reset afterward; original dark theme and 1.5 font scale restored. Animations were left at their original settings.
- No signed-in account, cloud-sync, physical audio quality, or hardware performance claims.

## Acceptance ledger

| Check | Result and evidence |
| --- | --- |
| Keep implemented desk and adaptive navigation | Preserved. `MeActivity` registers Home, Browse and Search through `NavigationBarView` and ViewPager2. `layout-w600dp` provides a native rail. JVM and API36 navigation assertions pass. |
| Finish stale Espresso migration with hard assertions | Complete. Five test classes share `SingingDeskTest`; suite registers all five. No swallowed assertion failures, JVM `assert(...)`, or guessed pager swipes. Tests use native IDs, exact intent extras, JUnit assertions and bounded waits. |
| Dark actionbar contrast | Existing correction retained. White title and light controls on charcoal. Final detail and sheet screenshots show readable titles and Up buttons. |
| Only visible child contributes Refresh | `TagQueryFragment` uses a MenuProvider bound to `viewLifecycleOwner` at RESUMED. Phone and tablet assert one matching visible Refresh in Browse and none in Search. Final tablet screenshot shows one Refresh. |
| Teachable entry available before first save | `teachableSection` and See all remain visible when empty. Empty-state model assertions and real entry/Up navigation pass. |
| Positive valid tag IDs only | Home already rejected blank, zero and overflow with editable inline feedback. Deep-link recognition now rejects nonpositive, nonnumeric and overflowing IDs. Valid 1 and Int.MAX_VALUE boundaries tested. Detail rejects invalid extras without a fetch/retry; sheet viewer exits for invalid tag IDs. |
| Audio callbacks safe after stop/release | All 22 media tests pass. Request generations invalidate queued/background fetch completions; callbacks check player identity; stop releases once and removes polling. Retry prepares a new player after decode errors. Tracks onPause invalidates both active and pending playback without requiring detach. |
| Pitch stops when leaving summary/sheet | `TagSummaryFragment.onPause` and sheet onPause/onStop retained. Sheet touch handles ACTION_UP, CANCEL and OUTSIDE. No physical listening verification. |
| Insets and Back on child screens | Shared helper wired to Detail, Sheet, standalone Browse/Search, Results, legacy Query, Settings and Teachable. Added missing Up wiring and removed CLEAR_TOP handlers that discarded the selected desk destination. Legacy WebView owns top/side/bottom/IME padding because it has no desk actionbar. API36 tests exercise app-bar Up and system Back. JVM tests cover cutout/IME padding, repeated dispatch and keyboard dismissal. |
| SDK35 JVM default | Existing `src/test/resources/robolectric.properties` had `sdk=35`, correctly scoped to module tests. Added `application=depollsoft.tagmaster.TestDeskApplication` after merged resources exposed production Firebase startup in five legacy test classes. Production compileSdk37/targetSdk36 remain unchanged. |
| Preserve data contracts | No model/cache/sync/migration source edits. Existing URL/type cache keys, tag ID extra, query serialization, list storage and attribution retained. |

## Screen inventory and evidence

Paths below are relative to `Android/TagMaster/src/main` unless stated otherwise. A preserved code path is not a claim that its external service was exercised.

| Screen or state | Implementation and verification | Remaining limits |
| --- | --- | --- |
| Home and launch changelog | `MeActivity`, `HomeFragment`, `MeHeaderView`, `meview.xml`, `homeview.xml`. Labeled Find a tag, Random Tag, Open tag ID; native Home/Browse/Search. Espresso checks enabled actions, selected destination and recreation. Launch fixture dismisses only the identified changelog. Final phone/tablet captures. | Random catalog selection's successful network response was not re-probed in this continuation. |
| Favorites and saved repertoire | Home binds existing favorite IDs and saved rows. Heading and empty-state behavior asserted against actual local list. Existing local tag 31 appears in captures. | No list reset or account synchronization during UI testing. Add/remove/reordering models pass JVM tests, not a new signed-in end-to-end flow. |
| Teachable tags | Always-visible Home entry; `TeachableTagsActivity`, `teachabletagsview.xml`, saved-row layout. Empty state and native entry/Up tested. | No signed-in repertoire claims. |
| Browse | `TagBrowseFragment`, `TagBrowserActivity`, `tagmasterview.xml`. Latest, Rating, Downloads, Classic use existing models. Native tab strip above results; expanded rail beside content. Navigation and single Refresh checked on phone/tablet. Live catalog rows captured. | Complete pagination to catalog exhaustion not repeated. |
| Search/filter | `TagSearchFragment`, `TagSearchActivity`, `tagsearchview.xml`. Title/lyrics field and sort, sheet music, learning tracks, parts, collection controls retained. All filters reachable; submit opens Results and Back keeps query text. Search destination survives recreation. | Every filter permutation and query restoration after process death not tested. |
| Results, pagination, empty/error/loading | `TagQueryFragment`, `TagSearchResultsActivity`, `TagQueryActivity`, `tagqueryview.xml`, `loadmoreitemview.xml`. Inline progress/status/retry, retained fetch-more path, Results title fallback. Structural empty/error checks, search-submit/Back and standalone Up pass. | Offline/timeout/pagination failures were not exhaustively injected on the emulator. |
| Tag overview | `TagDetailActivity`, `TagSummaryFragment`, `tagdetailview.xml`, `tagsummaryview.xml`. Four sections remain Summary, Details, Tracks, Videos. Inline loading/error/retry replaces modal fetch interruption. Native tag 31 deep link and final summary capture. Invalid detail extra tested. | Final tablet detail rail is covered by JVM layout tests; no extra tablet-detail capture in this bounded confirmation. |
| Detail metadata | `TagMiscFragment`, `tagmiscview.xml`. Existing detail fields and actions retained, shared readable column/theme. | Not recaptured in this continuation. |
| Learning tracks/player | `TagTracksFragment`, `MediaPlayerView.java`, `tagtracksview.xml`, `mediaplayerview.xml`. Native part selection, play/pause/stop, position and balance. 22 tests cover fetching, decoding, descriptors, cancellation, retries, stale completions/errors, pause/resume, detach, polling, part changes and fragment onPause. Prior track screenshot retained. | Mock player/cache tests do not prove codec support, speaker output, Bluetooth behavior or audio focus under other apps. |
| Videos/teaching videos | `TagVideosFragment`, `tagvideosview.xml`, `videodisplay.xml`, `teachingvideodisplay.xml`. Existing launch/embed paths retained with refreshed layout/theme. | External video playback was not tested in this continuation. |
| Sheet music | `SheetMusicActivity`, `sheetmusicview.xml`. `sheetMusicRoot` inset owner, native Up, chart zoom/rotate/external-open actions, key button and pitch stop hooks. Real tag 31 chart opened and captured with bottom key control clear of gesture bar. Back key was issued after capture. | Chart rotation, external viewer fallback and multipage PDF content were not exhaustively tested. Back command was issued but its resulting activity was not recorded by the dumpsys filter, so no separate asserted sheet-Back pass. |
| Ratings, save and share | Existing detail menu, `ratingview.xml`, summary and save feedback retained. Ratings/list models pass JVM tests. Favorite state visible in final summary. | No rating submitted, external share target selected or signed-in action performed. |
| Settings/account/about | `SettingsActivity`, `settingsview.xml`. Theme controls, cache/list utilities, login/logout entry, changelog, diagnostics and sheet wake-lock setting retained. Phone tests reach system-theme/cache controls and verify system Back and Up preserve Search selection. Prior dark-large Settings capture retained. | Authentication provider flows, deletion, cloud backup and destructive cache/list confirmations not exercised. |
| Terms/in-app web browser | `TagMasterBrowserActivity` remains a subclass of shared `BrowserActivity`; app-scoped full insets added. Local HTML WebView and system Back tested on API36, with actual inset assertions. | External site's layout/accessibility and browser network errors are outside this confirmation. |
| Large text, dark, tablet | Semantic DayNight colors/theme, shared spacing/type roles, `ReadableWidthLayout`, width-qualified rail layouts. JVM measures selected phone/tablet layouts at font scale 2.0. API36 phone uses 1.5; tablet uses 800dp light. | No hardware tablet, actual split-screen, landscape/cutout matrix, TalkBack traversal or Switch Access session. |

Observed content caveat for Main: current catalog rows display `Posted: 12/31/69`. This also appears in the existing local favorite. Date parsing/data was not changed in this refresh; investigate separately rather than treating these dates as verified catalog metadata.

## Test results and prior evidence

Before running anything, inspected all 24 prior unit XML files and both complete logs, `build/refresh-unit.log` and `build/refresh-build-initial.log`.

- Prior build succeeded. Prior unit run reported 125 cases, 10 failures. All ten failures were SDK36 sandbox initialization requiring Java21 on Java17.
- Prior refresh counts were DeskLayout 8, DeskNavigation 7, DeepLinkContract 6, MediaPlayerView 4, all passing. The current media test source had already expanded to 22 cases and required fresh execution.
- First continuation selection ran 195 cases. 65 passed; 130 in five legacy classes failed before test bodies because production `TagMasterApplication` tried to start Firebase without initialization.
- Fixed test application default only, then reran just those five classes. All 130 passed. No production Firebase code was changed.

Final targeted JVM coverage totals **195 passing cases across 15 classes**, accumulated across the first selection and failure-only rerun. This is not a claim that the complete module suite was rerun.

| Class | Passing cases |
| --- | ---: |
| DeskLayoutTest | 8 |
| DeskNavigationTest | 7 |
| DeskInsetsTest | 2 |
| DeepLinkContractTest | 8 |
| MediaPlayerViewTest | 22 |
| FavoritesModelTest | 24 |
| ListModelTest | 27 |
| QueryModelTest | 41 |
| TeachableTagsModelTest | 27 |
| RatingsModelTest | 11 |
| barbershop.TagCacheTest | 1 |
| barbershop.TagParseFromXmlTest | 10 |
| barbershop.TagQueryNetworkStubTest | 4 |
| barbershop.TagTracksAndKeyNoteTest | 2 |
| barbershop.VideoParseFromXmlTest | 1 |

Gradle replaces `build/test-results/testDebugUnitTest` on each filtered run. The latest XML contains the five-class 130-case rerun. Counts for the first selection were read directly from its XML before replacement and are recorded above; its full log remains.

Native API36 results total **22 phone checks plus 4 selected tablet checks passing across runs**:

- Phone suite: 21/22 initially. Filter scroll failed while the pager was settling. Added bounded synchronization, then failure-only run passed 1/1.
- Tablet subset: 2/4 initially. Menu creation and filter scrolling raced pager settling. Added a bounded wait for ViewPager2.SCROLL_STATE_IDLE and the RESUMED menu contribution. Both failed checks passed 2/2 afterward.
- Final helper confirmation on restored dark-large phone: those same two checks passed 2/2.
- No assertions were weakened or caught. No sleeps were added to test assertions. Capture scripts use short waits only to allow native rendering.
- `adb am instrument` can exit zero despite test failures. Results above come from instrumentation status/results, not shell exit alone. Failure-only confirmation commands also grep for exact `OK` counts.

LSP discovery returned no available `lsp_diagnostics` action in this session. Edit hooks reported clean Java/Kotlin syntax. Gradle compilation and runtime tests supplied type/behavior verification.

## Exact verification commands

Run Gradle commands from `Android`. All build/test logs use `set -o pipefail`, `2>&1` and `tee`; tool timeouts were 900-1200 seconds for builds/full native tests, with shorter bounded timeouts for failure-only probes.

First build and selected JVM tests:

```sh
set -o pipefail
./gradlew :TagMaster:assembleDebug :TagMaster:assembleDebugAndroidTest :TagMaster:testDebugUnitTest --tests depollsoft.tagmaster.DeskLayoutTest --tests depollsoft.tagmaster.DeskNavigationTest --tests depollsoft.tagmaster.DeskInsetsTest --tests depollsoft.tagmaster.DeepLinkContractTest --tests depollsoft.tagmaster.MediaPlayerViewTest --tests depollsoft.tagmaster.FavoritesModelTest --tests depollsoft.tagmaster.ListModelTest --tests depollsoft.tagmaster.QueryModelTest --tests depollsoft.tagmaster.TeachableTagsModelTest --tests depollsoft.tagmaster.RatingsModelTest --tests depollsoft.tagmaster.barbershop.TagCacheTest --tests depollsoft.tagmaster.barbershop.TagParseFromXmlTest --tests depollsoft.tagmaster.barbershop.TagQueryNetworkStubTest --tests depollsoft.tagmaster.barbershop.TagTracksAndKeyNoteTest --tests depollsoft.tagmaster.barbershop.VideoParseFromXmlTest --console=plain 2>&1 | tee TagMaster/build/refresh-confirm-build-unit.log
```

Failure-only legacy rerun and successful debug build:

```sh
set -o pipefail
./gradlew :TagMaster:assembleDebug :TagMaster:testDebugUnitTest --tests depollsoft.tagmaster.FavoritesModelTest --tests depollsoft.tagmaster.ListModelTest --tests depollsoft.tagmaster.QueryModelTest --tests depollsoft.tagmaster.TeachableTagsModelTest --tests depollsoft.tagmaster.RatingsModelTest --console=plain 2>&1 | tee TagMaster/build/refresh-confirm-legacy.log
```

Native install/suite, paths from repository root:

```sh
set -o pipefail
{ adb -s emulator-5554 install -r Android/TagMaster/build/outputs/apk/debug/TagMaster-debug.apk && adb -s emulator-5554 install -r Android/TagMaster/build/outputs/apk/androidTest/debug/TagMaster-debug-androidTest.apk && adb -s emulator-5554 shell am instrument -w -r -e class depollsoft.tagmaster.SingingDeskInstrumentationTest depollsoft.tagmaster.test/androidx.test.runner.AndroidJUnitRunner; } 2>&1 | tee Android/TagMaster/build/refresh-confirm-phone-instrumentation.log
```

Test-only APK rebuilds used `./gradlew :TagMaster:assembleDebugAndroidTest --console=plain` with logs `refresh-confirm-test-apk.log` and `refresh-confirm-test-apk-final.log`, then `adb -s emulator-5554 install -r TagMaster/build/outputs/apk/androidTest/debug/TagMaster-debug-androidTest.apk` from `Android`.

Native runner selectors, each passed to the same `adb -s emulator-5554 shell am instrument -w -r -e class ... depollsoft.tagmaster.test/androidx.test.runner.AndroidJUnitRunner` command:

| Selector | Log under Android/TagMaster/build |
| --- | --- |
| `depollsoft.tagmaster.TagSearchActivityTest#searchFiltersRemainReachableByScrolling` | `refresh-confirm-phone-rerun.log`, 1/1 |
| `depollsoft.tagmaster.MeActivityTest#navigationTypeMatchesCurrentWindowWidth,depollsoft.tagmaster.MeActivityTest#nativeDestinationsSwitchAndReturnHome,depollsoft.tagmaster.MeActivityTest#onlyTheVisibleBrowsePageContributesRefresh,depollsoft.tagmaster.TagSearchActivityTest#searchFiltersRemainReachableByScrolling` | `refresh-confirm-tablet-instrumentation.log`, initial 2/4 |
| `depollsoft.tagmaster.MeActivityTest#onlyTheVisibleBrowsePageContributesRefresh,depollsoft.tagmaster.TagSearchActivityTest#searchFiltersRemainReachableByScrolling` | `refresh-confirm-tablet-rerun.log`, 2/2; `refresh-confirm-phone-final.log`, 2/2 |

Native detail probe used:

```sh
adb -s emulator-5554 shell am start -W -a android.intent.action.VIEW -d 'http://www.barbershoptags.com/dbpage.php?pg=view\&dbase=tags\&id=31' -n depollsoft.tagmaster/.UrlHandlerActivity
```

Capture command: `adb -s emulator-5554 exec-out screencap -p > <path>`. Review copies: `sips -Z 800 <native.png> --out <review.png>`. Originals retain native geometry. Each review copy was size-checked below 300KB, then read individually with Pi's built-in read. No combined image transport or image blobs in summaries.

## Screenshot inventory

All paths are under `.impeccable/review/tagmaster-android/`.

Final native confirmation captures, individually reviewed through 800px copies:

- `phone-confirm-home-dark-large.png`
- `phone-confirm-search-dark-large.png`
- `phone-confirm-summary-dark-large.png`
- `phone-confirm-sheet-dark-large.png`
- `tablet-confirm-home-light.png`
- `tablet-confirm-browse-light.png`

Each has a corresponding `-review.png` copy. Sizes range from 52,905 to 106,622 bytes. Phone originals are 1080x2400; simulated tablet originals are 1600x2200. These are the final confirmation screenshots, not a repeat of every initial screen.

Prior worker captures retained unchanged:

- `phone-android.png`
- `phone-android-list-dark-large.png`
- `phone-android-settings-dark-large.png`
- `phone-android-summary-dark-large.png`
- `phone-android-tracks-dark-large.png`
- `tablet-android.png`
- `tablet-android-browse.png`

Native hierarchy files remain under `Android/TagMaster/build/refresh-confirm-{home,detail,sheet,tablet-home,tablet-browse}.xml`. Capture operations are logged in `refresh-confirm-capture.log`, `refresh-confirm-tablet-instrumentation.log` and `refresh-confirm-phone-final.log`.

## Final build and scope check

Final `set -o pipefail; cd Android; ./gradlew :TagMaster:assembleDebug --console=plain 2>&1 | tee TagMaster/build/refresh-confirm-final-build.log` passed in 5 seconds after all source edits. `git diff --check -- Android/TagMaster .impeccable/review/tagmaster-android-report.md` passed. No changed production paths match Model, Cache, migration or sync. Debug APK: `Android/TagMaster/build/outputs/apk/debug/TagMaster-debug.apk`.

## Handoff

Implementation verification is complete within this bounded continuation. Fresh final design review/doc remains with Main. Do not infer authentication, external media, physical audio, full accessibility or every device posture from these tests. Keep the original worker captures for coverage that was deliberately not repeated here.
