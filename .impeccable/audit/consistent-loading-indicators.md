# Consistent loading indicators

## Result and acceptance ledger

One owner extended the existing unified artwork across both platforms. Nothing was committed or pushed.

- [x] Reuse the exact shared geometry, palette, phase formula and two-second linear timing. Add only `compactArtworkHeight: 32`; derive its 18.7586-point/dp logical width from the original 34×58 box.
- [x] Replace the intended app-owned circular indicators. All six Android pending bindings now call `Loading`, not just `Visibility`. The eight XML placements include portrait and landscape summary variants.
- [x] Extract `TMBarberPoleLoadingView.h/.m` for Objective-C and Swift; register the source and bridge. Keep the query controller body byte-identical to its existing implementation.
- [x] Reserve compact accessory space, retain button heights/icons, and put indicators outside disabled filled buttons. Keep the search artwork and footer spacing unchanged.
- [x] Stop stripe motion for completion, failure, cancellation, hidden/clipped views, detachment and background. Test view reuse and live reduced-motion changes.
- [x] Build both native apps; run focused component, real-binding and native-host tests. Keep older query, quartet, pitch and footer tests intact.
- [x] Capture light/dark native pending states and same-phase compact/regular comparisons. Complete one visual correction batch and one native confirmation batch.
- [x] Restore original APKs/apps, user data and settings. Validate readback; retain all backups.

## Host inventory

| Host | Treatment | State and retained behavior |
| --- | --- | --- |
| Android Random Tag, `MeHeaderView.kt` / `meviewheader.xml` | Compact pole in the existing trailing slot | Real `IsLoading` binding; request errors and empty results clear it. The native row action remains unchanged. |
| Android saved rows, `SavedTagItemView.kt` / `savedtagitemview.xml` | Compact pole in the existing loading area | Requires a bound tag ID and unresolved load. Completion, failure, rebinding to null and detachment stop it; existing row placeholders remain. |
| Android tracks, `MediaPlayerView.java` / `mediaplayerview.xml` | Compact pole beside native controls | `IsLoading` follows download/preparation; Stop remains enabled. Play/pause icons, seekbars and balance remain intact. |
| Android sheet rendering, `SheetMusicActivity.kt` / `sheetmusicview.xml` | Centered compact pole | A render-pending flag replaces `drawable == null` alone. `finally` clears it even if decoding and external fallback fail. No pitch behavior changed. |
| Android rating and sheet opening, `TagSummaryFragment.kt` / both `tagsummaryview.xml` layouts | Neutral adjacent compact slots | Existing operation booleans bind the loader; both success and failure restore controls and retain icons. |
| Android detail/settings linear indicators | Kept | Useful compact progress bars; no reason to replace them with poles. |
| Android rating bars and media sliders | Kept | Ratings, playback position and balance are values, not circular loaders. |
| iOS Random Tag, `DPHomeViewController.m` | Compact row accessory | Real busy counter reloads the row. The row names the operation once; the accessory is not a second accessibility element. |
| iOS track preparation, `DPTagTracksController.swift` | Compact tapped-row accessory | Readiness/failure stop it. An old completion cannot clear a replacement accessory. Streaming, retry and cache behavior remain intact. |
| iOS loaded-tag refresh, `DPTagViewController.m` | Compact navigation accessory | Only tag refresh drives it, not unrelated row/button work. Share/actions remain available. Shared dark-surface metal stays visible on charcoal. On iOS 26 only this item's shared glass background is hidden. |
| iOS rating and sheet opening, `DPTagSummaryController.m` | Reserved neutral slot beside the native button | Original text/icon configurations are untouched. Success/failure stop the pole; rating success retains the existing Rated state. |
| iOS saved rows, `DPTagCell.m` | Kept text-only `Loading tag…` | It already replaces the shared grid overlay with a self-sizing label, not a circular spinner. No extra row whitespace or shared-library edits. |
| iOS query `UIRefreshControl` | Kept | Native pull-to-refresh gesture and indicator. The regular query footer still uses the shared pole. |
| iOS rating `UIProgressView` | Kept | Numeric rating visualization, not loading. |
| Quartet initial-tag loaders | Kept on both platforms | Separate initial-loading treatment; no extra navigation spinner over the quartet. |
| System AVPlayer, Firebase/sign-in UI | Kept | Platform/third-party-owned controls are outside this change. |

Mechanical source inspection finds no remaining `CircularProgressIndicator` placements in the inventoried Android layouts and no `UIActivityIndicatorView` / `showsActivityIndicator` constructions in Tag Master's iOS source. Remaining native refresh/rating/bar references were reviewed individually rather than treated as grep failures.

## Shared renderer and lifecycle

Android uses the existing `BarberPoleLoadingView` and cached `BarberPoleLogo` paths. `barberPoleCompact` selects a fixed compact role; generated dimension resources size its slots. A larger parent cannot magnify compact artwork. `findViewTreeLifecycleOwner` supplies the resumed gate for the new hosts. Query pagers retain their explicit `hostResumed` gate. Pre-draw checks handle clipping/scrolling; settings observation handles Remove animations. Detachment removes observers and cancels the animator.

iOS uses the extracted layer renderer everywhere. Compact size is intrinsic and capped while fitting uniformly. A single weak visibility registry checks attached pending views at 10 Hz because ancestor hiding/scrolling need not relayout an accessory. Core Animation owns stripe motion; the monitor creates no images, paths or layers. Stopped, detached and background views leave the registry. Empty registries stop their timer. `isAnimating` retains the existing requested-operation semantics; tests inspect the stripe animation separately for actual motion. Notification observers are removed on deallocation.

Frame and collars stay stationary. Accessibility names one operation, with no per-frame announcements. No production delays, audio, haptics, runtime image loading or per-frame UIImage generation were added.

Removing the one compact-height line from the shared JSON reproduces its previous SHA-256 exactly: `72c8069bb0624c265200e89bf17cbe7e17c88523d3f4ee4e483832db2890942d`. The canonical Android XML and launch PDF are unchanged. The existing full native raster matrix remains accepted evidence and was not repeated.

## Files touched by this extension

- Shared: `shared/tagmaster/barberpole-loader.json`; `iOS/tagmaster/tools/generate_logo_artwork.py`, `test_logo_artwork.py`.
- Android renderer/generated resources: `Android/TagMaster/src/main/java/depollsoft/tagmaster/{BarberPoleLoadingView.kt,BarberPoleLogo.kt}`; `src/main/res/values/{barberpole_loader.xml,barberpole_dimensions.xml}`.
- Android hosts/layouts: the six host categories and layout paths listed in the inventory above.
- Android tests: new `CompactLoadingBindingsTest.kt`, `SheetLoadingBindingTest.kt`, `SummaryLoadingBindingTest.kt`, `CompactLoadingRegressionTest.kt`; added real-binding assertions in `MediaPlayerLifecycleTest.kt` and `PdfPagesRegressionTest.kt`.
- iOS: `tagmaster/{TMBarberPoleLoadingView.h,TMBarberPoleLoadingView.m,TMLogoArtwork.h,TMLogoArtwork.m,DPTagQueryViewController.m,DPHomeViewController.m,DPTagTracksController.swift,DPTagViewController.m,DPTagSummaryController.m,tagmaster-Bridging-Header.h}`; `tagmaster.xcodeproj/project.pbxproj`; `tagmasterTests/TagmasterAppLogicTests.m`.
- This audit and `.impeccable/review/tagmaster-consistent-loading-*.jpg`.

Existing uncommitted unified-artwork changes remain. No shared library, Pitch Perfect, signing, account, backend, persistence or query-contract code was edited. Xcode project changes only register the extracted source/header.

## Commands and results

All test scripts used `set -euo pipefail`; shell calls used `settle:true`. Raw logs and fixture scripts are under `/tmp/tm-consistent`, not the repository.

1. Shared checks: `python3 -m unittest discover -s iOS/tagmaster/tools -p test_logo_artwork.py` passes **12 tests**, including all eight prior tests and four compact-size drift cases. Generator `--check` and `/tmp/tm-consistent/verify-source.py` pass. The latter verifies all eight XML placements, six pending bindings, native imports, Swift consumption, single Xcode registration and preserved source/artwork.
2. Android, JDK 17 at `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`, from `Android/`: `./gradlew :TagMaster:assembleDebug :TagMaster:assembleDebugAndroidTest` passes. Focused `:TagMaster:testDebugUnitTest --tests depollsoft.tagmaster.<Class>` runs pass **18 distinct tests**: `BarberPoleLoadingViewTest` 6, `CompactLoadingBindingsTest` 2, `MediaPlayerLifecycleTest` 6, `SheetLoadingBindingTest` 2, `SummaryLoadingBindingTest` 2. Logs: `android-unit.log`, `android-sheet-fixed.log`, `android-summary-unit.log`.
3. Android direct instrumentation on `emulator-5554`: `adb shell am instrument -w -r -e class depollsoft.tagmaster.CompactLoadingRegressionTest .../AndroidJUnitRunner` passes in light and dark, then once per theme for confirmation. Dark confirmation uses font scale 1.3. `PdfPagesRegressionTest` passes once. **Two distinct native methods, five passing executions**. Logs: `android/{light.log,dark.log,pdf.log,light-confirmation.log,dark-confirmation.log}`. No Gradle uninstall.
4. iOS uses `xcodebuild -workspace iOS/iOS.xcworkspace -scheme tagmaster -derivedDataPath /tmp/tm-dd`, explicit supplied simulator UUIDs and `-parallel-testing-enabled NO`. Production/test builds pass. `test` / `test-without-building -only-testing:tagmasterTests/<Class>/<method>` pass **nine distinct methods**:
   - `TMFooterPitchTests`: the three `testCompact…` methods and `testBarberPoleLogoGeometry`.
   - `TMPolishRegressionTests/testMissingSheetDataIsNotCachedAndRetryPresentsQuickLook`.
   - `TMSummaryLayoutRegressionTests`: both normal and AX5 row-hugging/page-switch tests.
   - `TMLoadingRegressionTests/testPreViewPendingDelayedSuccessAndSingleAnnouncement` and `testBackWhilePendingStopsMotionAndDoesNotRetainController`.
   - Across phone/iPad and the bounded confirmation, **15 passing executions**. Logs: `ios/{phone.log,phone-fixed.log,ipad.log,retained-regressions.log,phone-confirmation.log,ipad-confirmation.log}`. Final build: `ios-final-build-fixed.log`.
5. Final `git diff --check` passes. No unrelated full suite was repeated.

### Failures inspected and corrected

- APK installation initially rejected the older worktree version. Added `-d` to replacement installs; never uninstalled production data.
- Extraction first omitted the public `updateAnimation` declaration used by query scrolling. Added it. Test helper compilation also needed its closing `@end` and KVC access to the private detail tag.
- Android's first held-host run selected no available track. The fixture now selects Tenor. A dark resume then hit ActivityScenario's synchronous first-draw barrier while the loop was running; only the fixture pauses animation across that barrier and restores the original scale immediately.
- Six iOS assertions incorrectly treated UIKit's 36pt nav allocation as artwork width. Assertions now measure the actual compact layer. No renderer geometry was relaxed.
- Two initial sheet unit failures came from Robolectric bypassing `ContentProvider.openAssetFile`; the registered input-stream fixture now holds the real decode path. Both tests pass.
- A final generic-simulator build encountered a stale `RecaptchaInterop` precompiled-header dependency. Moved only the stale generated PCH directory aside and rebuilt with the explicit phone destination. No dependency/configuration edits.
- The isolated SourceKit warning says `DPTagTracksController` is missing. Xcode compiles that Swift file, resolves the existing Objective-C declaration through the bridge, and native tests execute its selection/readiness code. No duplicate class was added to satisfy the isolated diagnostic. LSP/ast-grep tools were not exposed in the active registry.

## Visual review

The first combined review found Android captures could precede the hardware frame despite a pending binding, and iOS 26 put the nav loader inside a pale shared glass background. The single correction batch adds 120ms settling only to the capture helper and removes only the loader item's shared background. The confirmation shows contained row artwork, readable adjacent indicators, unchanged button icons and light metal on charcoal navigation chrome.

There are **33 JPEGs**, all at most 800px, including `tagmaster-consistent-loading-review.jpg`. The review combines real pending Random Tag, sheet-button, track and refresh-nav crops with compact/regular phase-0.25 artwork. Phone and iPad host captures include both themes; home/track AX5 and Android font-1.3 captures cover larger text. Existing search-size screenshots remain in the prior unified-artwork review.

Two detached comparison images inherited the device theme before their requested override resolved. They were removed rather than mislabeled as art drift. The review uses the already-captured iPad light comparison and retains the phone dark comparison; sampled metal interiors match the shared palettes within JPEG rounding. The fixture now calls `updateTraitsIfNeeded` before drawing. That final capture-helper-only line was compiled after device restoration, not rerun on devices. No extra native raster or visual capture cycle was added.

## Restoration and limits

Initial settings were measured, not assumed: Android night yes/font 1.0/animation scale 1.0; iPhone dark/large/Reduce Motion 0; iPad light/large/Reduce Motion absent.

`backup.sh` validated Android tar manifests and extracted backups before test installation. It copied stopped iOS bundles/data and compared them before tests. Backups remain at `/tmp/tm-consistent/android` and `/tmp/tm-consistent/ios`.

Restoration failed closed twice before completion. Android emulated external storage refused the root directory timestamp; `tar -mxf` restores contents without that unsupported timestamp. iOS briefly returned retired container registrations even after boot; bounded polling now waits for existing/current paths. The final iOS restore preserves current container-manager metadata rather than copying a previous registration's identity.

`restore-fixed.log` records `ANDROID_RESTORE_VERIFIED`: both original APKs, all private/external file contents and every recorded setting compare equal. `restore-ios-fixed.log` records both `IOS_RESTORE_VERIFIED` entries: original app bundles compare byte-for-byte; stopped user data and post-boot checksum readback match. Only OS-owned container-manager metadata and regenerable `Library/SplashBoard` snapshots are excluded from the post-boot user-data comparison. Both simulators are booted with original settings. No backups were deleted.

No physical-device performance, live screen-reader walkthrough or third-party sign-in UI testing is claimed. Accessibility names/traits and motion state are covered by focused tests. No commit or push.
