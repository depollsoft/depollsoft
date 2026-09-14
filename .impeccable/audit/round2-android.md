# Tag Master Android round 2

Date: 2026-09-07. Working tree only; nothing committed. Round-1 work left intact. Device: Android 16 emulator (emulator-5554), 1080x2400 @ 420 dpi; expanded checks used a 1600x2560 / 240 dpi override on the same emulator, so tablet evidence is an override, not tablet hardware.

## Score delta

Audit scores come from `2026-09-07__tagmaster-android.md`. The round-1 column is the re-score recorded at the start of round 2 in `progress-android-round2.md` (round-1 verification did not publish numbers). Scale is 0-4 per dimension.

| Dimension | Audit | Round 1 | Now | What moved it / what still holds it back |
| --- | --- | --- | --- | --- |
| Accessibility | 1 | 3 | 3 | Links now announce as buttons (shared `Hyperlink`), saved-row spinner and availability indicators have names, Detail has an inline error with Retry. Held back: no full TalkBack session; uiautomator dumps were used only to drive taps. |
| Performance | 2 | 2 | 3 | Home and Teachable Tags are RecyclerViews (`SavedTagListAdapter`, `StaticViewAdapter` + ConcatAdapter); `LoadingImageView` has a bounded 3-worker / 24-deep queue with LRU cache and downsampling; Refresh deletes cache files off the main thread. Held back: no frame-time measurement on hardware. |
| Appearance & theming | 2 | 3 | 3 | Material 3 roles verified in light and dark on every screen; legacy `dark`/`darker` colors removed; only one filled button per screen. Held back: no dynamic color. |
| Platform conformance | 2 | 3 | 3 | Inline error + Retry on Detail instead of Toast + finish; brand title while loading; Up/deep-link handling in `EdgeToEdge.navigateUpOrHome`. Held back: ListView/retainInstance still in a few fragments; App Links unverified. |
| Adaptivity | 2 | 2 | 3 | Content max width (640 dp, 960 dp for two-column landscape) applied to every list/form via `View.applyContentInsets`; Summary and Tracks have two-column `layout-land` layouts; large text verified on home, browse, search, summary, tracks, sheet, settings, teachable. Held back: no `sw600dp` resources, no list-detail pane. |
| **Total** | **9/20** | **13/20** | **15/20** | |

## What changed in round 2

Adaptivity

- `res/values/dimens.xml`, `res/values-land/dimens.xml`: `content_max_width` and `two_column_max_width` tokens.
- `EdgeToEdge.kt`: `applyContentInsets` (side insets + bottom inset + centered max width in one listener), `applyImeAndBarInsetsAsPadding`, `navigateUpOrHome`.
- `res/layout-land/tagsummaryview.xml`, `res/layout-land/tagtracksview.xml`: two-column landscape layouts.
- Browse/detail tabs, query/videos/summary/misc/tracks fragments, search, settings: content insets and max width applied.

Performance

- `SavedTagListAdapter.kt` (ListAdapter keyed by tag id + `RowDivider`), `StaticViewAdapter.kt`, `MeActivity.kt` (ConcatAdapter header / favorites / footer), `TeachableTagsActivity.kt` (RecyclerView), `MeHeaderView.kt` (lifecycle-owned cleanup, favorites empty state).
- `DepollSoftCommon/.../LoadingImageView.java`: bounded executor queue (DiscardOldest), LRU bitmap cache, size-aware decode. Public `getSource`/`setSource` unchanged; `:PitchPerfect:assembleDebug` passes.
- `TagDetailActivity.kt`: cache deletes on Refresh moved off the main thread.

Conformance / states

- `TagDetailActivity.kt`, `res/layout/tagdetailview.xml`, `res/values/strings_detail.xml`: inline error state with Retry (`detail-error` capture), brand title while loading.

Accessibility

- `DepollSoftCommon/.../Hyperlink.java`: `onInitializeAccessibilityNodeInfo` reports the Button class (the log from the interrupted session claimed this but the file was unchanged; written now).
- `res/layout/savedtagitemview.xml`: loading indicator named; `StatusIndicatorView` announces available/unavailable.

Craft-floor fixes from this session's inspection round

- `res/layout/meviewfooter.xml`: 32 dp top padding so the app-info footer no longer runs into the Favorites section.
- `res/layout/tagitemview.xml`: the two availability indicators sit in a ConstraintLayout `Flow` (packed start, 24 dp gap, chain wrap) so at font scale 1.3 they drop to a second line as whole items instead of breaking "Learning tracks" mid-phrase.
- `res/layout/tagsummaryview.xml`, `res/layout-land/tagsummaryview.xml`, `res/values/strings.xml`: the centered "(Favorite)" / "(Teachable)" kicker above the title became a status line ("Favorite", "Teachable tag") under the version row; ids unchanged so existing tests still resolve.
- `res/layout/settingsview.xml`: View Changelog is an outlined button; Log in stays the single filled action.
- `res/values/colors.xml`: unused `dark` / `darker` removed.

Tests

- `MeActivityTest`, `FavoritesFlowTest` assert on `homeList` / `favoritesAdapter` instead of the old ItemsControl. Coordinator patched `NavigationTestFixture.onResumed` to poll for a resumed activity (test-only).

## Verification

All commands from `Android/` with `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and `ANDROID_SERIAL=emulator-5554`.

| Command | Result |
| --- | --- |
| `./gradlew :TagMaster:assembleDebug :TagMaster:testDebugUnitTest :PitchPerfect:assembleDebug` | exit 0, BUILD SUCCESSFUL; 262 unit tests, 0 failures, 0 errors, 0 skipped (`/tmp/r2-unit.log`) |
| `./gradlew :TagMaster:connectedDebugAndroidTest` | exit 0; 105 tests, 0 failures, 0 errors, 0 skipped (`/tmp/r2-connected.log`; XML under `TagMaster/build/outputs/androidTest-results/connected/debug/`) |
| `git -c core.whitespace=cr-at-eol diff --check -- Android` | exit 0 |

Before the connected run the installed APK and the app's private data were pulled; after it the same APK was reinstalled and the data restored with `run-as ... tar -x`.

## Screenshots (`.impeccable/review/tagmaster-android-round2-*.png`)

home, home-dark, home-large, home-expanded; browse, browse-dark, browse-large, browse-landscape, browse-expanded; search, search-dark, search-large, search-expanded; summary, summary-dark, summary-large, summary-landscape, summary-expanded, summary-saved; tracks, tracks-dark, tracks-large, tracks-landscape; videos, videos-dark; sheet, sheet-dark, sheet-large; rating, rating-dark; settings, settings-dark, settings-large, settings-expanded, settings-bottom; teachable, teachable-dark, teachable-large; detail-error; changelog-large.

Every capture was inspected at reduced size. One inspection round found the five defects listed above; one rebuild and one recapture of the affected screens confirmed them fixed. No third round was needed.

## Deferrals

- No TalkBack session on hardware; names and roles were verified in code and by uiautomator hierarchy only.
- Dynamic color (Material You) not added; the brand palette is static by design (charcoal + barber-pole red).
- No `sw600dp` resources or list-detail pane; expanded widths get a centered 640 dp measure, which is a reading-width fix, not a tablet layout.
- ListView/retainInstance remain in the query and videos fragments; migrating them is an architecture change outside a polish round.
- App Links still unverified (needs hosted Digital Asset Links with release fingerprints).
- Videos row's single "Multitrack" indicator stays centered under the metadata; it is consistent with the row family but could read better left-aligned with the labels.
- Device settings restored: font scale 1.0, night mode on, size/density reset, accelerometer rotation 1 / user rotation 0.
