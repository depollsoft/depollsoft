# Android density report

Root verified as `/Users/depoll/.local/share/pi-worktrees/20260907005637/tm-refine-worktree-20260907`. Scope is `Android/TagMaster` and `density/android-*`. Existing implementation preserved. No commits, pushes, delegates, new builds or test/capture reruns in this continuation.

## Verdict

UI and focused behavioral checks pass across the saved runs. No unresolved test failures found. Preference-content restoration is **not proven** by the existing hashes, as explained below. This is not a claim of complete release certification.

## Test evidence

| Saved evidence | Exact result |
| --- | --- |
| `android-build-tests.log` | App and instrumentation APK assembly completed. JVM run: 56 tests, 53 passed, 3 failed; overall `BUILD FAILED` in 29s. |
| `android-row-native-metrics.log` and current DensityTest XML | Only the three failed row tests rerun: 3 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESSFUL` in 17s. |
| `android-phone-light-native.log` | 4 tests passed: populated navigation/scroll, independent context-menu mutations, Favorites entry/Back, Teachable entry/Back. |
| `android-phone-dark-large-native.log` | Populated navigation/scroll test passed once at font scale 1.5, dark. |
| `android-tablet-light-native.log` | Populated navigation/scroll test passed once at 800dp simulated tablet width, light. |

Totals are 56 distinct JVM checks with passing evidence across the original run and targeted correction, not a single final 56-test green run. Native evidence is 6 passing executions of 4 distinct tests, not 6 distinct tests. The targeted rerun replaced the initial XML; the initial aggregate and failure names remain in its log.

Initial failures were `DensityTest.defaultRowIsTwoLinesSixtyToSeventyTwoDpWithReadOnlyStatus` at line 194, `savedRowsHaveNoInvisibleHeightPlaceholderAndKeepLoadingErrorStates` at line 266, and `longTitleAndLargeTypeGrowWithoutClipping` at line 243. All were `AssertionError`. The log does not preserve their measured values. The current correction uses Robolectric native graphics on SDK35, attaches rows to an activity, measures and lays out the real view tree. It retains the 60–72dp assertion and text-layout clipping checks. The passing XML records a 61dp default row, 61dp loaded saved row, and 334dp long-title/alternate row at font scale 2.0. These are Robolectric native-renderer measurements, not emulator dp measurements. No assertions changed in this continuation.

Recorded Gradle task selections, run from `Android` with the supplied JDK17/SDK environment:

```sh
./gradlew :TagMaster:assembleDebug :TagMaster:assembleDebugAndroidTest :TagMaster:testDebugUnitTest --tests depollsoft.tagmaster.DensityTest --tests 'depollsoft.tagmaster.Desk*Test' --tests depollsoft.tagmaster.DeepLinkContractTest --tests depollsoft.tagmaster.MediaPlayerViewTest
./gradlew :TagMaster:testDebugUnitTest --tests 'depollsoft.tagmaster.DensityTest.defaultRow*' --tests 'depollsoft.tagmaster.DensityTest.savedRows*' --tests 'depollsoft.tagmaster.DensityTest.longTitle*'
```

Native commands and device configurations are in `android-capture.sh`, invoked as `bash .impeccable/review/density/android-capture.sh`. Both APKs were installed successfully before that batch. No main or instrumentation source file is newer than the existing app APK. The later source change is the JVM test correction, which has its own passing run. An additional APK typecheck was therefore unnecessary.

## Acceptance coverage

- Home has exactly two live count-bound entries at 0, 1 and 100 overlapping IDs, no inline tag views/ItemsControl and unchanged measured height. Entry targets are at least 48dp. Both buttons launch their dedicated activities.
- Favorites is registered in the manifest, titled Favorites, and has an empty state. Teachable Tags retains its own destination and empty state. Both roots receive the full PoleBackground.
- Separate lists preserve order and contain overlapping IDs once each. JVM tests cover independent move/removal in both lists. Native long-press tests exercise Favorites move/remove and Teachable removal without changing the other membership, including the returning Home count.
- Native hierarchy assertions verify all 24 synthetic IDs in order on each destination and at least five fully visible saved rows. Tag/Back preserves a nonzero Favorites scroll offset on all three configurations. Teachable scroll restoration is not independently asserted.
- Shared catalog/saved rows retain complete titles, optional distinct alternate titles, ID and read-only material status. No checkbox controls or invisible sizing row remain. Metadata-preservation tests find rating in Summary and posted/downloads in Details. Loading, failure and subsequent successful binding are covered.
- Native sp typography, wrap-content height, 48dp minimums and full-page pole remain. Home/saved destinations retain readable-width wrappers. Browse retains its existing full-width tablet list; no new catalog width cap is claimed.
- Diff inspection confirms no model, storage-key, migration, sync, catalog data, MediaPlayer or PDF implementation changes. No labels or custom-list feature ships. `Android/TagMaster/DESIGN.md` and `.impeccable/design.json` already describe the built-in destinations, compact rows and future-only labels correctly; preserved without broader rewriting.

## Captures

Twelve existing PNGs cover Home, Favorites, Teachable and Browse for phone light, phone dark/large and tablet light. `android-phone-light-*`, `android-phone-dark-large-*` and `android-tablet-light-*` are native emulator captures, not mockups. Home counts and saved lists are **synthetic memory-only fixture evidence**, IDs 910001–910024. Browse is the live read-only catalog. The tablet is emulator-5554/API36 resized to 1600×2200 at 320dpi, not physical tablet hardware.

This continuation inspected existing `android-phone-light-home.png`, `android-phone-dark-large-favorites.png` and `android-tablet-light-browse.png` individually through native image read. Each is at most 1000px high and below 300KB. They show peer entries without feeds, compact readable rows, native Back and the unobscured barberpole. No confirmation capture was needed. Main owns the fresh review captures.

## Cleanup and limitations

The native fixture requires a signed-out Firebase user, enables the existing ListModel test mode before replacing memberships, and restores original membership references, tag-cache entries and AppCompat night mode in teardown. The capture shell force-stops the app and restores its private preferences backup on exit. No account writes were requested; membership edits use test mode.

Read-only continuation probes confirmed physical size 1080×2400 and density 420 with no overrides, font scale 1.5 and night mode yes, matching the recorded initial state. The app process is stopped, the private density backup is absent, and the twelve density capture files have been removed from the device. Older character-correction captures were left untouched.

**Preference verification gap:** `android-preferences-before.sha256` is `17bb5536c5f053b007fcb29d5fbffdcf10ac513083120a49794316b5c5c6eedd`; `android-preferences-after.sha256` is `8aada0e56ed328156930ce7d293c4bcb8de40ffe49d450d5c780354bd95f5ec3`. They do not match. The script hashes `tar cf - shared_prefs`, which includes filesystem metadata, and restores with `cp -R`, which can change timestamps. This can explain different archives but does not establish identical file contents. The original backup was deleted and no before-file hashes were retained, so historical byte-for-byte preference restoration cannot now be certified. Do not overwrite these hashes or claim a match. Any later authorized capture should retain sorted per-file SHA256 hashes and compare contents before deleting its backup.

No physical-device, TalkBack, authentication/sync, audio/PDF playback, landscape or exhaustive accessibility-state rerun was performed. Long-title font-scale-2 geometry is JVM-native evidence; the emulator large-text batch used 1.5. Gradle deprecation warnings remain. Final continuation check `git diff --check -- Android/TagMaster` passed.
