# Android loading and bottom tabs

Implemented in `tagmaster/impeccable-ux-polish`. Android only. No commits or pushes, shared-library, signing or configuration edits.

Waiting now feels like four voices gathering: a 216dp native five-line staff with four blue notes and a gentle stagger. Native text reads “Gathering the quartet…” and “Loading tag <id>…”. No production delays, network assets, sound or haptics.

## Acceptance evidence

| Check | Evidence |
| --- | --- |
| Initial request before resume, visible pending, fast success | `TagLoadingStateTest`: no pager adapter or placeholder fragments until data; initial loader visible, tabs/actions/watermark/progress hidden. Immediate cached-task success does not wait for animation. |
| Delayed success, failure, retry, cancellation | Unit and native controllable `TaskCompletionSource` fixtures complete explicitly, without sleeps. Native tests assert actual visible states and tag-ID fields. Initial errors stop waiting and expose Retry. |
| Refresh success and failure | Loaded content, tabs and useful actions remain; only refresh is disabled and the small progress indicator appears. Failed refresh retains the tag and offers Snackbar Retry. Native retry verifies updated bound title. |
| Current request and screen | Unit tests reject mismatched tag IDs, stale new-intent completions and post-finish completions. Native tests reject an old screen's completion. |
| Animation lifecycle and disabled motion | Native motion-enabled test passes for hidden, background/resumed and destroyed/detached states. No-motion native runs assert static illustration. Visible-rectangle/layout/scroll guards also stop a clipped illustration. No physical-device scrolling/performance measurement. |
| Equal full-width tabs | Both XML layouts use MODE_FIXED, fill gravity and tabMaxWidth=0dp. Both Activities use only the existing horizontal system/cutout inset helper. Pager/content width caps remain unchanged. Unit tests measure every contiguous equal slot at phone, landscape and 1067dp expanded widths, plus fontScale 2.0. Native expanded slots fill 1600px at 240dpi. |
| Labels, swipes and restoration | Wrapping native labels grow the bar instead of scrolling or clipping. Native cached-detail and Me navigation tests preserve selection, page swipes and restored tabs. Existing labels/order/icons/blue top indicator remain. |
| Native captures | Six controlled pending/loaded pairs, with a hardware frame-commit fence before screenshots. JPEG dimensions are at most 800px. Phone light/dark, 2.0 font scale, landscape, expanded and Remove Animations. |
| Accessibility | Notes are decorative; one grouped, polite native loading status carries the real tag ID. No per-note announcements or frame-driven status changes. TalkBack speech was not manually tested. |

## Tests and registration

- **20 targeted unit tests passed**: TagLoadingStateTest 9, BottomTabsLayoutTest 4, BrandStyleRegressionTest 5, PolishLayoutRegressionTest 2. SDK 28, plain Application, JDK 17.
- **61 unique native tests passed**: TagDetailActivityTest + MeActivityTest reported `OK (53 tests)`; TagLoadingRegressionTest + PolishFlowRegressionTest reported `OK (8 tests)`. Additional motion-enabled lifecycle execution reported `OK (1 test)`. Each of six capture configurations reported `OK (1 test)`.
- Debug app and instrumentation APK builds passed. `git diff --check` passed.
- New production Kotlin files are under `src/main/java/depollsoft/tagmaster/`: `TagLoadingView.kt` and `FullWidthTabLayout.kt`. Both are referenced by layout resources; both Activities register `bottom_tab_content.xml` in their mediators. Loading copy lives in `res/values/strings_detail.xml`.
- New tests are under standard `src/test/kotlin/` and `src/androidTest/java/` paths and were discovered by Gradle/instrumentation. Native capture harness is SDK 29+; actual native execution used Android 16 on emulator-5554. Unit state coverage remains SDK 28.
- LSP extension discovery was unavailable. Compiler, unit tests and actual native instrumentation supplied verification.

## Defects caught while testing

The revised tab regression failed 3/3 on baseline MODE_AUTO before production edits. Native refresh testing then found that Tag's ID equality suppressed bound-field updates on refreshed instances. Local identity-aware trackable storage fixes that without changing shared code or Tag equality.

Expanded native testing caught Material's per-tab maximum width even after the content inset cap was removed. Explicit tabMaxWidth=0dp fixes the measured gaps. The navigation fixture now selects only the displayed label, since Material retains its own GONE default label when using a custom wrapping label. No fake views were added to satisfy tests.

Robolectric kept its window GONE despite an attached/shown Activity, so animation lifecycle checks run natively instead of weakening the production window guard. Native capture fixtures associate completion with the current Activity across orientation recreation. Frame-commit callbacks prevent stale compositor frames from being called loaded.

## Captures and bounded review

All files are under `.impeccable/review/`, prefixed `tagmaster-android-loading-`, with `-pending.jpg` and `-loaded.jpg` suffixes for:

- `phone-light`
- `phone-dark`
- `phone-dark-large`
- `phone-no-motion`
- `landscape-dark`
- `expanded-dark`

One bounded visual review and one confirmation set completed. The expanded per-tab cap was the visual correction. Final captures use disabled animation for deterministic stills; the live motion-enabled lifecycle test separately verifies the loop. No unrelated screen redesign or speculative ratings.

## Device preservation

Private data was backed up before installation to `/tmp/tagmaster-loading-data.tar`. Original settings are in `/tmp/tagmaster-loading-device-settings.txt`. After testing, private files were restored and a byte-for-byte directory comparison passed. Settings matched the original night=yes, fontScale=1.0, automatic rotation, rotation=0, all animation scales=1.0, physical 1080×2400 at 420dpi. Test-generated external captures were removed from the device after retrieval. The installed APK is the updated debug build; app data was preserved.

Evidence logs remain in `/tmp/`, not the repository: `tagmaster-loading-final-verification.log`, `tagmaster-loading-cache-navigation-final.log`, `tagmaster-loading-state-flow-final.log`, `tagmaster-loading-motion-final.log`, capture-specific logs, and `tagmaster-loading-final-restore.log`.
