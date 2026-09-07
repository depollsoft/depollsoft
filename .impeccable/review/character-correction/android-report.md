# Android character correction

Status: implemented, shown inline for Main/user review. No commit, push or PR changes. User confirmation is still required.

## Review images first

All paths below are under `.impeccable/review/character-correction/`. Copies are PNG, longest edge <=1000px, each <300KB.

- `android-confirm-phone-light-home.png`
- `android-confirm-phone-light-tag31.png`
- `android-confirm-phone-light-browse.png`
- `android-confirm-phone-light-tracks.png`
- `android-confirm-phone-dark-large-home.png`
- `android-confirm-phone-dark-large-tag31.png`
- `android-confirm-phone-dark-large-tracks.png`
- `android-confirm-tablet-light-home.png`
- `android-confirm-tablet-light-browse.png`
- `android-final-tablet-light-tag31.png`
- `android-final-tablet-light-tracks.png`

Actual screens show the original recognizable pole at page scale, including populated Browse and the two-pane tablet workspace. The first Browse capture caught list rows before compositor text drew; confirmation waits for nonempty row text and a frame-settling interval. Do not use the first-round blank-row image as final evidence.

## Acceptance ledger

- Home-only MeActivity. No active ViewPager, global bar or rail. Existing Find/Search and Browse activity routes retained. Explicit Browse added; Random/ID are quiet text actions. Favorites, always-available Teachable entry, Settings and wordmark retained. No duplicate Home heading.
- Native labeled collection Spinner replaces tabs/pager. Existing independent TagQueryFragments retain sort/model state; Search remains its separate controller.
- Tag Summary opens Learning tracks, Details and Videos. Phone native Back returns to the same Summary instance and then the caller. At >=720dp and font scale <1.5, Summary and material share one workspace; accessibility sizes use one column. Default material is tracks, or details when tracks are absent.
- Original 299x513 pole asset and original light/night tints are unchanged. PoleBackground draws once on each page root, including catalog/results and utility roots. No opaque pane covers it; media control backplates remain local.
- Data/list/query models, backend, MediaPlayerView implementation and its 22 tests, PDF implementation/test, pitch cancellation, rated-state binding, attribution and link validation remain intact.
- Native query return, ID 31, warm deep link, catalog-result-to-tag-to-tracks-to-Back pass in phone light, phone dark/1.5 font, and tablet light. Separate native tests pass query/collection recreation and material recreation/rotation. Cold tablet deep link also inspected through the actual accessibility hierarchy.

## Commands and results

Environment: JDK17 `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`; SDK `/opt/homebrew/share/android-commandlinetools`. Gradle commands ran from `Android`, with `set -o pipefail`, timeout 1200s, full logs under this report directory.

- `./gradlew :TagMaster:assembleDebug :TagMaster:assembleDebugAndroidTest :TagMaster:testDebugUnitTest --tests 'depollsoft.tagmaster.Desk*Test' --tests 'depollsoft.tagmaster.DeepLinkContractTest' --tests 'depollsoft.tagmaster.MediaPlayerViewTest' --console=plain`: 47 passed, zero failures/skips. 8 navigation, 7 layout, 2 inset/error, 8 deep link, 22 media. `android-build-tests.log`.
- Final production debug/test APK build: passed. `android-confirm-build.log`. Later builds changed test code only.
- `adb -s emulator-5554 shell am instrument -w -r -e class depollsoft.tagmaster.SingingDeskInstrumentationTest,depollsoft.tagmaster.SheetMusicPdfTest depollsoft.tagmaster.test/androidx.test.runner.AndroidJUnitRunner`: 21 passed. `android-native-regressions.log`.
- `CharacterStateTest`: 3 passed, recorded individually in `android-tablet-recovery.log`. Query, collection, material recreation and actual rotation. That combined run also contains the then-failing screenshot test; do not describe the whole log as passing.
- `CharacterCorrectionTest`: one hard live route test passed per final phone light, phone dark/large and tablet light configuration. Logs: `android-confirm-phone-light-proof.log`, `android-confirm-phone-dark-large-proof.log`, `android-tablet-ime-proof.log`.
- Initial tablet proof failed because Espresso tapped Open while IME dismissal moved the dialog. Diagnostics showed the ID dialog still active. Test-only fixes wait for that movement, examine focused current windows, and scope the final assertion to the discovered root. No assertion catches or alternate-success UI checks. Only the failed tablet route was rerun after confirmation. Final tablet proof passes.
- `git diff --check -- Android/TagMaster`: passed. XML search finds no ViewPager2, TabLayout, BottomNavigationView or NavigationRailView in either active layout directory.

## Device and limits

One API36 emulator, serial `emulator-5554`. Phone runtime 1080x2400 at density420. Tablet runtime override 1600x2200 at density320, 800dp wide, not physical tablet hardware. Phone dark evidence uses fontScale1.5; JVM one-column accessibility checks use fontScale2.0.

One implementation capture batch plus one confirmation batch. Subsequent work was failure-driven test correction for the missing tablet Tag/Tracks captures, not UI tuning. No delegates. Focused scope exceeded the 20-minute target due to tablet test input/root diagnosis.

Restored `wm size reset`, `wm density reset`, original system night=yes and original fontScale1.5. Did not modify app theme preference or saved lists. Final device queries confirm physical 1080x2400/density420 and original appearance/font.

Limits: no physical audio, TalkBack traversal, authenticated sync, real offline recovery, every filter/random result, or physical predictive-Back gesture validation in this pass. No tablet landscape screenshot; native rotation/state test passed. No fresh settings screenshot or exhaustive per-list scroll-offset/track-part persistence assertion across every posture. These are not claimed as verified. The catalog's pre-existing displayed epoch posted dates were not changed in this navigation/background correction. Main must show the listed actual corrected screens and obtain user confirmation before any PR update.
