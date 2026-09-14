# Tag Master (Android) — Native Audit

Date: 2026-09-07
App: `Android/TagMaster/` (Android Views, AppCompat/Material Components 1.14, Bindroid MVVM, targetSdk 36 / minSdk 24)
Evidence device: emulator-5554, sdk_gphone64_arm64, Android 16 (API 36), 1080×2400 @ 420 dpi, gesture navigation. Debug APK built from this worktree and installed; screenshots captured with `adb exec-out screencap`. No tablet emulator was available, so adaptivity evidence is source-only. Playbook: `~/.claude/skills/impeccable/reference/audit.native.md` scored against `android.md`. Product truth: `PRODUCT.md` (Tag Master section). `DESIGN.md` was not used because it covers Pitch Perfect only.

Environment note: the Bindroid git submodule was not initialized in this worktree, so the first build failed; running `git submodule update --init Android/Bindroid` fixed it (no source changes). The emulator was at font scale 1.5 on arrival; captures were taken at 1.0 and 1.3, and the device was left at font scale 1.0 and night mode on, as instructed.

## Audit Health Score

| # | Dimension | Score | Key Finding |
| --- | ----------- | ------- | ------------- |
| 1 | Accessibility | 1 | Icon-only FABs and the rate button have no labels; the rating dialog is unusable with TalkBack; home's primary actions are bare TextViews with 30 dp targets |
| 2 | Performance | 2 | Favorites list is non-virtualized and each row does synchronous file I/O on the main thread; thumbnails re-download on every bind with no cache |
| 3 | Appearance & Theming | 2 | Material 2 theme with three raw grays and a Holo-era blue accent; hard-coded `#FFFFFF` tints; dark FAB at ~2:1 contrast; no Material 3 / dynamic color |
| 4 | Platform Conformance | 2 | Bottom navigation used as in-screen tabs, modal `ProgressDialog`s on every fetch, Toasts for errors, legacy Spinners/EditText, deep links only handle `http` and are unverified |
| 5 | Adaptivity | 2 | One layout set, no `sw600dp`/landscape resources, phone bottom bar would ship untouched on tablets; IME handling on search is correct |
| **Total** | | **9/20** | **Poor (major overhaul)** |

## Platform Conformance Verdict

**Fail, narrowly.** This does not read as a ported website; it reads as a 2012-era Holo app that was moved onto Material Components without adopting Material's structure. A fluent Android user will trip on:

- **Bottom navigation used as tabs.** Browse (`Latest / Rating / Downloads / Classic`) and Tag Detail (`Summary / Details / Tracks / Videos`) each carry their own `BottomNavigationView` bound to a `ViewPager2`, while the home screen has no bottom navigation at all. These are sibling views of one screen, which Material reserves for tabs; bottom navigation is for top-level destinations. (`res/layout/tagmasterview.xml:46-52`, `res/layout/tagdetailview.xml:32-38`, `depollsoft.lib.kotlin/.../BottomNavigationUtils.kt:7-26`)
- **Home's primary actions are plain `TextView`s.** `Browse`, `Random Tag`, `Open Tag` are 24 sp bold text with click listeners, no ripple, no button semantics, 30 dp tall. (`res/layout/meviewheader.xml:7-46`, `MeHeaderView.kt:125-136`)
- **Modal `ProgressDialog` for every network fetch.** Random tag, tag detail, sheet music, rating submission, and track loading all block the whole screen with a deprecated `ProgressDialog`. (`MeHeaderView.kt:42-45`, `TagDetailActivity.kt:83-87`, `TagSummaryFragment.kt:167-170, 244-247`, `MediaPlayerView.java:168-171`)
- **Toasts for actionable errors** instead of Snackbars in at least nine places (listed under P2).
- **Legacy controls where Material components exist**: bare `EditText` search box, `Spinner`s, `ToggleButton` `PLAY`/`STOP`, two unlabeled `SeekBar`s, a custom `Dialog` of five `RatingBar`s. (`res/layout/tagsearchview.xml:29-38, 85-91`, `res/layout/mediaplayerview.xml:8-65`, `res/layout/ratingview.xml:27-95`)
- **No Up affordance on child screens.** Every child activity has a dead `HOME_MENU_ITEM_ID` handler but never enables the navigation icon. (`TagSearchActivity.kt:137-145`, `SettingsActivity.kt:349-357`, `TeachableTagsActivity.kt:35-43`)
- **Edge-to-edge is opted out with a flag Android 16 ignores.** On the API 36 evidence device the app only stays clear of the system bars because AppCompat's action-bar overlay consumes insets; the bottom navigation and FAB sit above an unstyled strip instead of extending under the gesture area. (`res/values/themes.xml:13`, `res/values-night-v21/themes.xml:15`; see `tagmaster-android-browse.png`)

What is native and right: Material FAB and Extended FAB, `BottomNavigationView` component itself, AppCompat `AlertDialog`s, system Back and predictive Back are never hijacked, `adjustResize` keeps the search FAB above the keyboard, Material icons throughout, all type in `sp`.

## Executive Summary

- Audit Health Score: **9/20** (Poor)
- Total issues: **P0: 0, P1: 8, P2: 15, P3: 5**
- Top critical issues:
  1. **Every posted date renders as 12/31/69.** The live feed sends `Tue, 25 Jan 2011`; `parseDate` accepts none of that form and returns epoch zero. Affects every list row, the Details tab, and every video. (`Utilities.kt:18-36`, `barbershop/Tag.kt:165`, `barbershop/Video.kt:31`)
  2. **Multi-page sheet music renders page 1 repeatedly.** The PDF loop opens page `0` on every iteration. (`SheetMusicActivity.kt:114-115`)
  3. **Deep links are effectively dead on Android 12+.** Only `http` is declared, no `https`, no `autoVerify`, and the handler is a deprecated `ActivityGroup`. The app's own share link (`tags.depoll.com`) is not handled at all. (`AndroidManifest.xml:77-100`, `UrlHandlerActivity.java:13-20`, `barbershop/Tag.kt:497-498`)
  4. **Screen-reader blockers**: unlabeled search FABs and rate button; rating dialog driven only by raw `ACTION_UP` touch events; home actions are unlabeled text.
  5. **Navigation model and feedback patterns are off-platform**: bottom nav as tabs, modal progress dialogs, Toast errors.
- Recommended next steps: fix the three functional bugs first (dates, PDF pages, deep links), then run `/impeccable harden` for accessibility, `/impeccable colorize` for a Material 3 token scheme with edge-to-edge, `/impeccable shape` for navigation/controls, and `/impeccable polish` last.

## Detailed Findings by Severity

### P1 — Major

**[P1] Posted dates show as 12/31/69 on every list row, the Details tab, and every video**

- **Location**: `Android/TagMaster/src/main/java/depollsoft/tagmaster/Utilities.kt:18-36` (`parseDate`), `barbershop/Tag.kt:165`, `barbershop/Video.kt:31`, rendered by `TagItemView.kt:80`, `TagMiscFragment.kt:33-34`, `VideoDisplay.kt:56-60`. Evidence: `tagmaster-android-browse-dark.png`, `tagmaster-android-detail-videos-dark.png`, `tagmaster-android-home-dark.png`.
- **Category**: Performance/Correctness (surfaced in every list)
- **Impact**: The `Latest` browse tab, `Sort by: Most recent`, and `Posted` metadata are meaningless to users; the app looks broken on its first screen. Live check of the feed (`api.php?n=2&fldlist=id,Title,Posted`) returned `Tue, 25 Jan 2011`, which none of the three accepted formats (epoch millis, `yyyy-MM-dd`, `MMM d, yyyy`) match, so `Date(0)` is returned.
- **Guideline**: Material — content must be accurate; a fallback value must not masquerade as data.
- **Recommendation**: Add an `EEE, d MMM yyyy` (`Locale.US`) parser to `parseDate` before the fallbacks; return `null` instead of `Date(0)` on failure and hide the `Posted` row when null (the bindings already use `BoolConverter` for visibility). Add a unit test with the live format alongside the existing tests in `src/test/kotlin/.../UtilitiesTest.kt:26-49`.
- **Suggested command**: `/impeccable harden`

**[P1] Multi-page PDF sheet music renders the first page N times**

- **Location**: `SheetMusicActivity.kt:114-115` — `for (page in 0 until renderer.pageCount) { val page = renderer.openPage(0) …`
- **Category**: Performance/Correctness
- **Impact**: Any tag whose chart is a multi-page PDF shows page 1 repeated; singers cannot see the ending in-app. The workaround is the `Open in external app` action.
- **Guideline**: n/a (functional defect)
- **Recommendation**: Use `renderer.openPage(page)` with a distinct loop variable name. Consider rendering pages lazily into a `RecyclerView` instead of one appended bitmap (see the performance note below).
- **Suggested command**: `/impeccable harden`

**[P1] Deep links only match `http`, are unverified, and the share link is not handled**

- **Location**: `AndroidManifest.xml:77-100` (`UrlHandlerActivity` intent filter, `android:scheme="http"` only, no `android:autoVerify`), `UrlHandlerActivity.java:13` (`extends ActivityGroup`, deprecated since API 13), `barbershop/Tag.kt:497-498` (`getTagUri` → `http://tags.depoll.com/tag.php?id=`), `SavedTagItemView.kt:81-82` (opens that URL for failed favorites).
- **Category**: Conformance
- **Impact**: On Android 12+ unverified `http(s)` links open in the browser by default, and `https://www.barbershoptags.com/dbpage.php…` (the form the site now uses) never matches the filter. Links shared from the app itself do not reopen the app on Android. Group singing use case in `PRODUCT.md` ("open a tag by ID", sharing) is weakened.
- **Guideline**: Android App Links: declare `https`, add `android:autoVerify="true"`, host `assetlinks.json` on each domain; use a normal `Activity`.
- **Recommendation**: Add `<data android:scheme="https" …/>` entries for both hosts and for `tags.depoll.com` path `/tag.php`; set `autoVerify` (verification requires `assetlinks.json` on `barbershoptags.com`, which the site owner must publish; `tags.depoll.com` is first-party and can be verified now). Replace `ActivityGroup` with a trampoline `AppCompatActivity` that keeps `canHandleUri` semantics unchanged. Do not change the URL formats produced by `getTagUri` or the share text.
- **Suggested command**: `/impeccable harden`

**[P1] Icon-only controls have no accessibility labels**

- **Location**: Search FAB — `res/layout/meview.xml:57-67`, `res/layout/tagmasterview.xml:33-43`, `res/layout/tagsearchview.xml:201-211`; Rate button — `res/layout/tagsummaryview.xml:192-203`. Confirmed via `uiautomator dump`: `FloatingActionButton '' ''` and `ImageButton '' '' rateButton`.
- **Category**: Accessibility
- **Impact**: TalkBack announces "button" with no name for the app's primary search entry on three screens and for rating.
- **Guideline**: Android accessibility — every interactive element needs a `contentDescription`.
- **Recommendation**: Add `android:contentDescription="@string/Search"` to the three FABs and `@string/Rate` to the rate button; add `app:tooltipText` where useful.
- **Suggested command**: `/impeccable harden`

**[P1] Rating dialog cannot be operated by TalkBack, switch access, or keyboard, and has no cancel control**

- **Location**: `RatingsPopup.java:32-54` (five `RatingBar`s driven by `OnTouchListener` on `ACTION_UP`), `res/layout/ratingview.xml:27-95` (`isIndicator="true"`, `clickable="true"`), custom `Dialog` with `FEATURE_NO_TITLE` (`RatingsPopup.java:19`). Evidence: `tagmaster-android-rating-dark.png`.
- **Category**: Accessibility / Conformance
- **Impact**: Accessibility services dispatch `performClick`, not touch events, so a rating can never be submitted; the only exit is system Back. The custom dialog also lacks Material dialog framing (no title bar, no actions).
- **Guideline**: Material dialogs: title, content, and dismiss/confirm actions; controls must respond to click actions.
- **Recommendation**: Replace with a `MaterialAlertDialogBuilder` containing one interactive `RatingBar` (`numStars=5`, `stepSize=1`) plus `Cancel`/`Submit` actions; keep `RatingsModel.addRating` and `Tag.rate` calls unchanged.
- **Suggested command**: `/impeccable harden`

**[P1] Home's primary actions are bare `TextView`s: no role, no ripple, 30 dp targets**

- **Location**: `res/layout/meviewheader.xml:7-46`, click handlers in `MeHeaderView.kt:125-136, 40-41`. Measured bounds 85 px ≈ 32 dp tall, full width, no `background=?selectableItemBackground`. Evidence: `tagmaster-android-home.png`.
- **Category**: Accessibility / Conformance
- **Impact**: TalkBack announces "Browse" as plain text; users get no touch feedback; targets are below 48 dp. This is the app's top-level navigation.
- **Guideline**: Material list items / text buttons; 48×48 dp minimum touch target; interactive text must expose a button role.
- **Recommendation**: Convert to Material list items (`minHeight=56dp`, `?selectableItemBackground`, leading Material icon, `android:screenReaderFocusable`), or `MaterialButton` with `Widget.Material3.Button.TextButton`. Keep the same string resources and click targets so `MeActivityTest` keeps passing (it references `meHeaderView1`, not the individual ids).
- **Suggested command**: `/impeccable shape`

**[P1] Navigation model: bottom navigation used as tabs, no Up affordance**

- **Location**: `res/layout/tagmasterview.xml:46-52`, `res/layout/tagdetailview.xml:32-38`, `res/menu/browsenavigation.xml`, `res/menu/tagdetailnavigation.xml`, `depollsoft.lib.kotlin/src/main/java/depollsoft/lib/kotlin/ui/BottomNavigationUtils.kt:7-26` (uses deprecated `setOnNavigationItemSelectedListener`); dead Up handlers in `TagSearchActivity.kt:137-145`, `SettingsActivity.kt:349-357`, `TeachableTagsActivity.kt:35-43`, `TagMasterBrowserActivity.java:16-24`. Evidence: `tagmaster-android-browse.png`, `tagmaster-android-detail-summary.png`.
- **Category**: Conformance
- **Impact**: Two different screens each present a bottom bar of sibling views; the home screen has none; child screens show no navigation icon in the top app bar. A fluent user expects tabs here and a back arrow in the app bar.
- **Guideline**: Material 3 — Navigation bar for 3–5 top-level destinations only; Tabs for sibling content within a destination; Top app bar with navigation icon on child screens.
- **Recommendation**: Replace both `BottomNavigationView`s with `TabLayout` + `TabLayoutMediator` over the existing `ViewPager2`/fragments (keep fragment classes, order, and menu strings). Set `android:parentActivityName` or `supportActionBar.setDisplayHomeAsUpEnabled(true)` on child activities and route the existing Up handlers through `onSupportNavigateUp`. Update the Espresso tests that reference `R.id.bottomNavigation` (12 usages across `MeActivityTest.kt`, `TagDetailActivityTest.kt`, `TagSearchActivityTest.kt`).
- **Suggested command**: `/impeccable shape`

**[P1] Deprecated modal `ProgressDialog` blocks the screen on every fetch**

- **Location**: `MeHeaderView.kt:42-45` (random tag), `TagDetailActivity.kt:36, 83-87, 124` (tag load), `TagSummaryFragment.kt:167-170` (sheet music), `TagSummaryFragment.kt:244-247` (rating), `MediaPlayerView.java:168-171` (track load), `DepollSoftCommon/.../BrowserActivity.java:26-28`.
- **Category**: Conformance
- **Impact**: During a live sing, choosing a random tag or opening a chart throws up a full-screen modal with hard-coded "Loading..."; the user cannot cancel or keep browsing. `ProgressDialog` has been deprecated since API 26.
- **Guideline**: Material progress indicators (linear/circular, inline or in the top app bar); never block the whole UI for a background fetch.
- **Recommendation**: Show a `LinearProgressIndicator` under the top app bar (or `CircularProgressIndicator` in the tapped control) and disable only the initiating control; on Tag Detail keep the existing `bottomNavigation`/tabs hidden until `tag != null` as today. Use the existing `isLoading` trackables where present (`QueryModel.isLoading`).
- **Suggested command**: `/impeccable shape`

### P2 — Minor

**[P2] Teaching video thumbnails use cleartext `http` and never load**

- **Location**: `TeachingVideoDisplay.java:80-82` (`http://img.youtube.com/...`, `http://www.youtube.com/watch`), loader `DepollSoftCommon/.../LoadingImageView.java:38-39`. No `networkSecurityConfig`/`usesCleartextTraffic` in `AndroidManifest.xml`.
- **Category**: Performance / Correctness
- **Impact**: With targetSdk ≥ 28 cleartext is blocked, so the `IOException` is swallowed and the preview stays blank. `VideoDisplay.kt:22-24` already uses `https`.
- **Recommendation**: Switch both URLs to `https`.
- **Suggested command**: `/impeccable harden`

**[P2] Non-interactive status indicators are `CheckBox`es**

- **Location**: `res/layout/tagitemview.xml:168-198` (`Sheet music`, `Learning tracks`, `clickable=false`), `res/layout/videodisplay.xml:120-132` (`Multitrack`).
- **Category**: Accessibility
- **Impact**: TalkBack announces "checkbox, not checked, Learning tracks" for something the user cannot toggle, on every row of every list.
- **Recommendation**: Replace with `TextView`s using `drawableStart` (`ic_check`/`ic_clear`) and a contentDescription like "Learning tracks available" / "No learning tracks"; or a Material `Chip` in non-interactive mode with `importantForAccessibility="no"` on the icon.
- **Suggested command**: `/impeccable harden`

**[P2] Key/pitch buttons respond only to raw touch events**

- **Location**: `PitchPerfectLib/.../PitchPipeButton.java:55-76` (`onTouchEvent` `ACTION_DOWN`/`UP`), `SheetMusicActivity.kt:61-67` (`setOnTouchListener` on the Extended FAB, `ClickableViewAccessibility` suppressed).
- **Category**: Accessibility
- **Impact**: The key note (a core "reach the right note quickly" action in `PRODUCT.md`) cannot be sounded via TalkBack, switch access, or keyboard. The content description from `PitchPipeButton.java:103-120` is good but the control is inert to click actions.
- **Recommendation**: Add a `performClick` path that plays the note for a fixed duration (e.g. 1.5 s) when triggered by an accessibility click; keep press-and-hold for touch.
- **Suggested command**: `/impeccable harden`

**[P2] Touch targets below 48 dp**

- **Location**: Footer hyperlinks 43 px ≈ 16 dp tall — `res/layout/meviewfooter.xml:45-53, 88-95, 109-127`; `Sheet Music` link 27 dp — `res/layout/tagsummaryview.xml:330-339`; rate `ImageButton` 50×40 dp — `tagsummaryview.xml:195-196`; key `PitchPipeButton` 40 dp tall — `tagsummaryview.xml:285-286`; `ratingProgressBar` 15 dp — `tagsummaryview.xml:186`; video rows' `LoadingImageView` 80×60 inside a clickable row (ok) but `TeachingVideoDisplay` row has no min height — `res/layout/teachingvideodisplay.xml:2-4`.
- **Category**: Accessibility
- **Impact**: Mis-taps on the attribution links and on `Sheet Music`, the single most important link on the summary screen.
- **Guideline**: 48×48 dp minimum with 8 dp spacing.
- **Recommendation**: Give `Hyperlink`s `minHeight=48dp` with `gravity=center_vertical` and vertical padding; make `Sheet Music` a `MaterialButton` (filled or tonal, `Widget.Material3.Button.Icon` with a chart icon); set `minWidth/minHeight=48dp` on the rate and key buttons.
- **Suggested command**: `/impeccable harden`

**[P2] Spinner labels are not associated with their controls**

- **Location**: `res/layout/tagsearchview.xml:73-91, 99-117, 125-143, 151-169, 177-195` and `res/layout/settingsview.xml:149-167, 175-193, 201-219, 227-245`.
- **Category**: Accessibility
- **Impact**: TalkBack reads "Title, dropdown list" without "Sort by".
- **Recommendation**: Add `android:labelFor="@id/…Spinner"` to each label, or migrate to Material exposed dropdown menus (`TextInputLayout` + `MaterialAutoCompleteTextView`) which carry their own hint. Keep the `@array/*Choices` entries and the selection semantics in `TagSearchActivity.kt:60-132` / `SettingsActivity.kt:112-188`.
- **Suggested command**: `/impeccable harden`

**[P2] Type sizes are hand-picked per view; body metadata is 12 sp**

- **Location**: `android:textSize="12sp"` on ~60 views across `tagitemview.xml`, `tagsummaryview.xml`, `tagmiscview.xml`, `settingsview.xml`, `tagsearchview.xml`, `meviewfooter.xml`, `videodisplay.xml`, `teachingvideodisplay.xml`, `tagtracksview.xml`, `tagvideosview.xml`, `teachabletagsview.xml`; headings at raw `20sp`/`24sp`; only `ratingview.xml:24` uses a `textAppearance`.
- **Category**: Accessibility / Theming
- **Impact**: Rating, ID, download counts, lyrics, and notes are all 12 sp (below Material `bodyMedium` 14 sp); in an afterglow with the phone held out, lyrics at 12 sp are hard to read. Scaling works (verified at 1.3×, no clipping), but the baseline is too small and nothing maps to the type scale.
- **Guideline**: Material type scale — map to `bodyLarge/Medium`, `titleMedium`, `headlineSmall`, `labelMedium`.
- **Recommendation**: Replace `textSize` with `?attr/textAppearanceBodyMedium` etc.; lyrics/notes → `bodyLarge`; tag title → `headlineSmall`; row title → `titleMedium`; metadata → `labelMedium`.
- **Suggested command**: `/impeccable typeset`

**[P2] Theme is Material 2 with raw grays, a Holo accent, and hard-coded tints; no Material 3 or dynamic color**

- **Location**: `res/values/themes.xml:4-17` (`Theme.MaterialComponents.Light.DarkActionBar`, `colorPrimary=@color/dark`, `colorSecondary=@color/darker`, `colorAccent=@android:color/holo_blue_dark`), `res/values-night-v21/themes.xml:4-21` (overrides `colorSurface` to `#373737`, `android:colorForeground`, a different `bottomNavigationStyle` than light), `res/values/colors.xml:3-6`, FAB `android:tint="#FFFFFF"` in `meview.xml:65`, `tagmasterview.xml:41`, `tagsearchview.xml:209`; `textColor="?android:attr/textColorPrimary"` mixed with Material roles in `tagitemview.xml:180,196`. Evidence: `tagmaster-android-home-dark.png` (FAB `#474747` on `#121212`, ≈2.0:1), `tagmaster-android-detail-summary.png` (blue `holo_blue_dark` progress on gray).
- **Category**: Theming
- **Impact**: The search FAB nearly disappears in dark mode; the accent blue is off-brand for a barbershop app; no tonal elevation; light and dark bottom bars use different styles so the same screen looks different in each appearance.
- **Guideline**: Material color roles, dynamic color on Android 12+ with a static fallback, dark theme as a first-class scheme, tonal elevation.
- **Recommendation**: Move to `Theme.Material3.DayNight.NoActionBar` (add a `MaterialToolbar` per screen) with a static scheme derived from the existing charcoal (`#373737`/`#474747` as `surfaceContainer`/`primary` tones) and a barbershop-appropriate accent (deep red or navy) for `secondaryContainer`; enable `DynamicColors.applyToActivitiesIfAvailable` in `TagMasterApplication`. Remove hard-coded tints (FAB icon color comes from `onSecondaryContainer`). Keep the handwriting title (`Utilities.kt:38-44`) as the single brand flourish.
- **Suggested command**: `/impeccable colorize`

**[P2] Edge-to-edge opt-out is ignored on targetSdk 36 and no insets are handled**

- **Location**: `res/values/themes.xml:12-15`, `res/values-night-v21/themes.xml:14-17` (`windowOptOutEdgeToEdgeEnforcement=true`, `navigationBarColor`), no `WindowCompat`/`OnApplyWindowInsetsListener` anywhere in `Android/TagMaster/src/main/java`. Evidence: `tagmaster-android-browse.png` shows a plain strip beneath the bottom navigation in the gesture area; `tagmaster-android-home-dark.png` shows the FAB stopping above it.
- **Category**: Conformance
- **Impact**: Android 16 deprecates and disables the opt-out for apps targeting 36; the app currently avoids overlap only because AppCompat's `ActionBarOverlayLayout` consumes insets. Lists end above the gesture bar with dead space, and the bottom bar does not extend under it as Material expects.
- **Guideline**: Edge-to-edge with window insets for status bar, navigation bar, display cutout, IME.
- **Recommendation**: Remove the opt-out and `navigationBarColor`; call `WindowCompat.setDecorFitsSystemWindows(window, false)`; apply insets to the toolbar (top), list padding (`clipToPadding=false` already set), `BottomNavigationView`/tabs (bottom), and FAB margins.
- **Suggested command**: `/impeccable layout`

**[P2] Legacy controls instead of Material components**

- **Location**: bare `EditText` search box `res/layout/tagsearchview.xml:29-38`; `Spinner`s `tagsearchview.xml:85-195`, `settingsview.xml:160-245`; `ToggleButton`/`Button` `PLAY`/`STOP` with two `SeekBar`s in a `TableLayout` `res/layout/mediaplayerview.xml:8-77`; `Balance` slider with no label association and the position slider with no label at all (`mediaplayerview.xml:36-41, 59-65`); theme radio group and checkbox in `settingsview.xml:258-292`; `Button`s with all-caps text throughout settings (`settingsview.xml:65-70, 88-98, 110-132, 294-299`); Open Tag dialog with a raw `EditText` and no hint or margins (`MeHeaderView.kt:137-151`).
- **Category**: Conformance
- **Impact**: The tracks screen reads as an early-Android media widget; `PLAY` as a text toggle and `STOP` disabled-gray on gray are hard to parse at a glance while singing.
- **Guideline**: Material text fields, exposed dropdown menus, icon buttons (play/pause/stop with `MaterialButton.Icon` or `FloatingActionButton`), `Slider` with `labelBehavior`, `MaterialSwitch` for the wake-lock toggle, segmented `MaterialButtonToggleGroup` for theme.
- **Recommendation**: Rebuild `mediaplayerview.xml` with play/pause and stop as icon buttons (48 dp), a `Slider` for position with the time label, and a labeled `Slider` for balance; migrate search and settings inputs as noted above. Keep the `RemoteLocation`/`ContentCache` playback logic.
- **Suggested command**: `/impeccable shape`

**[P2] Toasts used for actionable errors**

- **Location**: `MeHeaderView.kt:63-66, 72-77, 100-103`; `TagDetailActivity.kt:105-109`; `TagSummaryFragment.kt:176-181, 215-221, 251-256`; `MediaPlayerView.java:124-125, 184-185, 203-204`; `SheetMusicActivity.kt:146-150`; `SettingsActivity.kt:104-106, 196-201, 213-218, 336`.
- **Category**: Conformance
- **Impact**: "Could not load a random tag." and "Failed to load track." vanish in two seconds with no retry; the long random-tag filter message is truncated in a short toast.
- **Guideline**: Snackbars for transient feedback, actionable when useful.
- **Recommendation**: Route through a `Snackbar` on the activity's `CoordinatorLayout` root with `Retry`/`Settings` actions; keep confirmations ("Cache cleared.") as plain Snackbars.
- **Suggested command**: `/impeccable shape`

**[P2] Favorites reorder/remove is hidden behind long-press; Teachable Tags entry disappears when empty**

- **Location**: `SavedTagItemView.kt:70-74` (long-press → context menu), `FavoriteTagItemView.kt:34-57`, `TeachableTagItemView.kt:34-57`, `res/menu/favoritetagcontextmenu.xml`, `res/menu/teachabletagcontextmenu.xml`; `MeHeaderView.kt:183-187` binds `teachableButton` visibility to `teachableTagIds.size > 0`, so `res/layout/teachabletagsview.xml:21-27` (`NoTeachableTags`) is unreachable from the home screen; that empty-state text is also 12 sp flush to the top-left with no padding.
- **Category**: Conformance
- **Impact**: Teachers have no visible way to discover "Move up/down" or that teachable lists exist until they find `Mark as Teachable` in a detail toolbar; the only affordance is an undiscoverable long press.
- **Guideline**: Material list items with trailing overflow/drag handle; empty states that explain the feature.
- **Recommendation**: Add a trailing overflow `IconButton` (⋮) on saved rows opening the same menu, or a drag handle with `ItemTouchHelper`; always show `Teachable Tags` on home and give the empty state a centered Material empty-state layout with the `Mark as Teachable` hint. Keep `ListModel` ordering semantics.
- **Suggested command**: `/impeccable clarify`

**[P2] Favorites list is non-virtualized and each row performs main-thread file I/O**

- **Location**: `res/layout/meview.xml:42-50` (`ItemsControl`, a `LinearLayout` — `DepollSoftCommon/.../ItemsControl.java:54-81`), `SavedTagItemView.kt:55-65` (launches on `Dispatchers.Main`), `barbershop/Tag.kt:324-345` (`loadTagById` reads and JSON-deserializes the cache file synchronously on the calling thread before returning a `Task`), same pattern in `teachabletagsview.xml:35-44`.
- **Category**: Performance
- **Impact**: With a long favorites list every row inflates, binds, and reads a file on the UI thread at once; scrolling the home `ScrollView` also stutters because nothing is recycled.
- **Recommendation**: Wrap the cache read in `withContext(Dispatchers.IO)` (or `Task.callInBackground`) inside `loadTagById`; move favorites/teachables to a `RecyclerView` with `DiffUtil` keyed by tag id.
- **Suggested command**: `/impeccable optimize`

**[P2] Thumbnail loader spawns a raw thread per bind with no cache or downsampling**

- **Location**: `DepollSoftCommon/.../LoadingImageView.java:32-56`, bound from `VideoDisplay.kt:62` and `TeachingVideoDisplay.java:70` on every `onAttachedToWindow`.
- **Category**: Performance
- **Impact**: Scrolling the videos list re-downloads and fully decodes each thumbnail; threads are unbounded.
- **Recommendation**: Use Coil (or Glide) with the existing `ContentCache` directory, `size(80dp,60dp)`; or at minimum an `LruCache<String,Bitmap>` and `BitmapFactory.Options.inSampleSize`.
- **Suggested command**: `/impeccable optimize`

**[P2] No large-screen or landscape layouts; phone bottom bar would ship untouched on tablets**

- **Location**: `Android/TagMaster/src/main/res/` contains only `layout/`, `values/`, `values-night*/`, `values-v14/` — no `layout-sw600dp`, `layout-land`, or `values-sw600dp`; all activities set `configChanges="keyboardHidden|orientation"` (`AndroidManifest.xml:30-108`) so rotation re-lays out but nothing adapts; lists and tables are `fill_parent` with 16 dp margins (`tagqueryview.xml:17-29`, `tagsummaryview.xml:16-23`).
- **Category**: Adaptivity
- **Impact**: On a tablet or foldable the tag list stretches to 800+ dp lines, the search form's spinners span the full width, and the bottom bar remains a phone bottom bar. Landscape on a phone gives a 4-line list under a 56 dp app bar and a bottom bar.
- **Guideline**: Window size classes — navigation rail on medium/expanded width, list-detail on expanded, max content width.
- **Recommendation**: Add `layout-sw600dp` variants (or a `ConstraintLayout` with `layout_constraintWidth_max=640dp`) for lists, summary, search, settings; on expanded width switch the tabs/bottom bar to a `NavigationRail`; consider a list-detail pane for Browse → Tag on tablets.
- **Suggested command**: `/impeccable adapt`

### P3 — Polish

**[P3] Media player timer, prepare, and cache-size work on the main thread**

- **Location**: `MediaPlayerView.java:108, 287-303` (`Timer` at 25 ms posting to main, never `cancel()`ed on detach), `MediaPlayerView.java:194-196` (`player.prepare()` synchronous), `TagDetailActivity.kt:60-82` (cache file deletes on refresh), `SettingsActivity.kt:93, 363-369` → `ContentCache.getCacheSize()` walks the directory in `onCreate`, `Utilities.kt:39` (`Typeface.createFromAsset` on every activity), `ListModel.kt:59-64` → `Preferences.java:191` (`commit()` on every list change).
- **Category**: Performance
- **Impact**: Small jank on track start and settings open; a leaked timer thread per detail screen.
- **Recommendation**: `prepareAsync()`; `timer.cancel()` in `onDetachedFromWindow`; compute cache size on IO; cache the `Typeface`; `apply()`.
- **Suggested command**: `/impeccable optimize`

**[P3] Decorative barber-pole watermark is not marked decorative**

- **Location**: `res/layout/meview.xml:7-13`, `settingsview.xml:7-13`, `tagmasterview.xml:8-14`, `tagsearchview.xml:7-13`, `tagdetailview.xml:16-22`, `tagqueryactivity.xml:7-13`, `teachabletagsview.xml:7-13`.
- **Category**: Accessibility
- **Impact**: Harmless today (no contentDescription, not focusable) but fragile; explicit `importantForAccessibility="no"` documents intent.
- **Suggested command**: `/impeccable harden`

**[P3] User-facing strings hard-coded outside resources**

- **Location**: `res/menu/memenu.xml:7` (`android:title="Settings"`), `res/layout/settingsview.xml:312, 328` (`Private Build`, `Copy Logs`), `res/layout/meviewfooter.xml:26` (`" - "`), and every dialog/toast string in `MeHeaderView.kt`, `TagDetailActivity.kt`, `TagSummaryFragment.kt`, `SettingsActivity.kt`, `MediaPlayerView.java`; spinner logic compares English literals (`TagSearchActivity.kt:64-128`, `SettingsActivity.kt:120-184`).
- **Category**: Theming (consistency)
- **Impact**: `resourceConfigurations = ['en']` makes this safe today, but copy edits require code changes and the spinner logic breaks if the arrays are ever reworded.
- **Recommendation**: Move to `strings.xml`; compare spinner positions rather than labels.
- **Suggested command**: `/impeccable polish`

**[P3] Legacy layout idioms**

- **Location**: `fill_parent`/`dip` throughout; negative margin hack `tagsummaryview.xml:287`; `retainInstance = true` `TagQueryFragment.kt:26`; `android:tint` (not `app:tint`) on FABs; `supports-screens` block `AndroidManifest.xml:13-17`; `TableLayout` for forms.
- **Category**: Conformance
- **Recommendation**: Clean up while touching each layout in the bundles above.
- **Suggested command**: `/impeccable polish`

**[P3] Sheet music rotation re-renders and re-decodes the entire image**

- **Location**: `SheetMusicActivity.kt:34-36, 104-163` (`rotation` change → full `loadImage()`; PDF pages appended into a single bitmap up to 100 MiB).
- **Category**: Performance
- **Recommendation**: Rotate via `PhotoView`/`ImageView` matrix instead of re-rendering; render pages lazily.
- **Suggested command**: `/impeccable optimize`

## Patterns & Systemic Issues

- **Text-as-control**: clickable `TextView`s and `Hyperlink`s stand in for buttons on home, footer, and summary (`meviewheader.xml`, `meviewfooter.xml`, `tagsummaryview.xml:330`). Fix once with a shared `MaterialButton`/list-item style.
- **Hand-picked `textSize` on ~70 views** instead of type-scale roles; a single `/impeccable typeset` pass over 14 layouts.
- **Modal + toast feedback loop**: every async call is `ProgressDialog` → `Toast`. Replace with a shared progress indicator + Snackbar helper used by all six call sites.
- **Role misuse**: `CheckBox` for status, `BottomNavigationView` for tabs, `Dialog` for a rating picker.
- **Theme drift between appearances**: light and night themes pick different `bottomNavigationStyle`s and the night theme overrides `colorSurface`; tokens are not the source of truth.
- **Main-thread I/O in binding code**: `loadTagById`, `Preferences.commit`, `getCacheSize`, `MediaPlayer.prepare`.
- **Legacy platform APIs**: `ProgressDialog`, `ActivityGroup`, `ListView`, `retainInstance`, `setOnNavigationItemSelectedListener`, `startActivityForResult`.

## Positive Findings

- **All type is in `sp` and scales cleanly**; verified at 1.3× on home, browse, summary, tracks, and search with no clipping or overlap (`*-large.png`).
- **Keyboard handling on search is right**: `adjustResize` keeps the search FAB above the IME and the options visible (`tagmaster-android-search-keyboard-dark.png`).
- **System Back and predictive Back are untouched**; no custom back interception is active (the `SettingsActivity.onKeyDown` guard is inert).
- **Bottom bar items and toolbar actions are labeled**: `Summary/Details/Tracks/Videos`, `Remove Favorite`, `Mark as Teachable`, `Rotate 90°`, `Open in external app` all announce correctly.
- **Key note is one tap away on the chart**: the Extended FAB `Major:F` on the sheet music viewer, with a proper content description from `PitchPipeButton`, is exactly the "reach the right note" moment `PRODUCT.md` asks for.
- **Keep-screen-on for sheet music** (`SheetMusicActivity.kt:50`) respects the live-singing context.
- **Offline-first caching** of tags (`Tag.cache`), tracks, and charts via `ContentCache`, with cache size and clearing exposed in Settings.
- **Material icon set is consistent** (Material Symbols vectors in `res/drawable/`), and the barber-pole watermark is themed for both appearances (`values-night/colors.xml`).
- **Dark theme is a real scheme**, not an invert; dialogs, spinners, and lists all render correctly in both appearances.
- **Lists paginate** on scroll and via `Fetch additional tags…` with a loading state and an error/status line (`TagQueryFragment.kt:40-58`, `QueryModel.kt:44-81`).
- **Terminology is exactly the community's**: tags, learning tracks, parts, tenor/lead/baritone/bass, favorites, teachable tags, Classic Tags, BarbershopTags.com attribution — nothing to change.

## Recommended Actions

1. **[P1] `/impeccable harden`**: fix `parseDate` for `EEE, d MMM yyyy` and hide `Posted` when null; fix `openPage(page)`; add `https` + `autoVerify` deep links and retire `ActivityGroup`; `https` thumbnails; add content descriptions to the three FABs and the rate button; rebuild the rating dialog as a Material dialog with one `RatingBar`; add `performClick` to pitch buttons; `labelFor` on spinner labels; 48 dp targets on links and buttons; replace status `CheckBox`es with icon text.
2. **[P1] `/impeccable shape`**: bottom navigation → tabs on Browse and Tag Detail; Up affordance on child screens; `ProgressDialog` → inline progress indicators; Toast → Snackbar; home actions → Material list items; media player → icon buttons + `Slider`s; search/settings inputs → text field and exposed dropdowns.
3. **[P2] `/impeccable colorize`**: Material 3 theme with a static charcoal scheme plus dynamic color, remove hard-coded tints, fix dark FAB contrast, unify bottom-bar/tab styles across appearances.
4. **[P2] `/impeccable layout`**: real edge-to-edge with insets for toolbar, lists, tabs, FAB.
5. **[P2] `/impeccable typeset`**: map all `textSize`s to type-scale roles; lift metadata and lyrics off 12 sp.
6. **[P2] `/impeccable clarify`**: visible overflow/drag affordance on saved rows; always-visible Teachable Tags with a proper empty state.
7. **[P2] `/impeccable optimize`**: IO-thread tag loads, `RecyclerView` for saved lists, image caching, `prepareAsync`, timer cancel, `apply()`.
8. **[P2] `/impeccable adapt`**: `sw600dp` layouts, max content width, navigation rail on expanded width.
9. **`/impeccable polish`**: string resources, legacy attributes, decorative-image flags, final pass.

> You can ask me to run these one at a time, all at once, or in any order you prefer.
>
> Re-run `/impeccable audit` after fixes to see your score improve.

## Implementation Brief

Priority order within each bundle; bundles A–G are independent and can be worked in parallel. Every bundle must keep the Android unit tests (`./gradlew :TagMaster:testDebugUnitTest`) and Espresso tests (`src/androidTest`) green, updating test view ids only where noted.

### Bundle A — Correctness (no UI redesign; ship first)

1. `Android/TagMaster/src/main/java/depollsoft/tagmaster/Utilities.kt:18-36` — extend `parseDate` to try `EEE, d MMM yyyy` (`Locale.US`) before the ISO/US fallbacks; return `null` on failure. Update `Tag.kt:165` and `Video.kt:31` to accept a nullable result (`Tag.posted` is already `Date?`; make `Video.posted` nullable or keep `Date(0)` only if a null would break `ToStringConverter`). Bind `postedTextView`/`postedRow` visibility to `posted != null` in `TagItemView.kt:80`, `TagMiscFragment.kt:33`, `VideoDisplay.kt:56`. Add tests in `src/test/kotlin/depollsoft/tagmaster/UtilitiesTest.kt` using `"Tue, 25 Jan 2011"` and `"Sun, 3 Aug 2025"`. Preserve the existing accepted formats and the `TagParseFromXmlTest` millisecond case.
2. `SheetMusicActivity.kt:114-115` — `renderer.openPage(pageIndex)`; rename the loop variable. Preserve `MAX_BITMAP_SIZE` fallback to the external app.
3. `TeachingVideoDisplay.java:80-82` — `https://` for thumbnail and watch URLs. Preserve the YouTube id source (`tag.teachingVideo`).
4. `AndroidManifest.xml:77-100` — add `<data android:scheme="https"/>` for both `barbershoptags.com` hosts and a new filter for `tags.depoll.com` path `/tag.php` (both schemes), `android:autoVerify="true"`. `UrlHandlerActivity.java` — change base class to `AppCompatActivity`, keep `canHandleUri` unchanged and add handling for `tags.depoll.com/tag.php?id=`; keep the fallback to `TagMasterBrowserActivity` for non-tag URLs. Do not change `Tag.getTagUri` output or the share text in `TagDetailActivity.kt:38-54`. Note for release: `assetlinks.json` must be published at `https://tags.depoll.com/.well-known/` (first-party) and at `barbershoptags.com` (site owner) for verification.

### Bundle B — Accessibility (layouts + small code)

1. Content descriptions: `meview.xml:57`, `tagmasterview.xml:33`, `tagsearchview.xml:201` FABs → `@string/Search`; `tagsummaryview.xml:192` rate button → `@string/Rate`; decorative `ic_barberpole` ImageViews → `android:importantForAccessibility="no"` (7 layouts listed above).
2. `res/layout/meviewheader.xml` — replace the four action `TextView`s with Material list rows (`minHeight=56dp`, `?attr/selectableItemBackground`, leading icons `ic_search`/`ic_people`/`ic_refresh`/`ic_launch` or new Material Symbols, `textAppearanceTitleMedium`). Keep ids `browseButton`, `teachableButton`, `randomTagButton`, `openByIdButton` and the strings `BrowseTags`, `TeachableTags`, `RandomTag`, `open_tag`; keep the `Favorites` heading as a heading (`accessibilityHeading="true"`).
3. `RatingsPopup.java` + `res/layout/ratingview.xml` — rebuild as `MaterialAlertDialogBuilder` with title `@string/ChooseARating`, a single interactive `RatingBar` (`numStars=5`, `stepSize=1`, `isIndicator=false`, contentDescription), positive `Submit` and negative `Cancel`. Keep the public surface `getRating()`/`setOnDismissListener` used by `TagSummaryFragment.kt:237-268`, or adapt that call site; keep `RatingsModel` and `Tag.rate` untouched.
4. `PitchPerfectLib/.../PitchPipeButton.java` — add `performClick()` override that plays the note for ~1.5 s when no touch is in progress; `SheetMusicActivity.kt:61-67` — same via `setOnClickListener` fallback. Preserve press-and-hold behavior for touch.
5. `tagitemview.xml:168-198`, `videodisplay.xml:120-132` — replace `CheckBox` with `TextView` + `drawableStart` (`ic_check`/`ic_clear` selector) and contentDescription strings (`Sheet music available` / `No sheet music`, etc.). Update bindings in `TagItemView.kt:83-89` and `VideoDisplay.kt:61` from `Checked` to a drawable/description property (Bindroid `ReflectedProperty` on a small custom `StatusIndicatorView` is the least invasive).
6. `labelFor` on every spinner label in `tagsearchview.xml` and `settingsview.xml` (or do this as part of Bundle D's dropdown migration).
7. Touch targets: `meviewfooter.xml` hyperlinks `minHeight=48dp`; `tagsummaryview.xml:330-339` `Sheet Music` → `MaterialButton` (keep id `sheetMusicLink` and the `HyperlinkUri` binding by keeping a `Hyperlink` subclass, or move the click logic in `TagSummaryFragment.kt:162-232` to the new button); rate/key buttons `minWidth/minHeight=48dp`.

### Bundle C — Theme, color, edge-to-edge

1. `res/values/themes.xml`, `res/values-night-v21/themes.xml` — migrate to `Theme.Material3.DayNight.NoActionBar` (or `Theme.Material3.DayNight` if keeping the window action bar for now). Define the full role set in `res/values/colors.xml` (light) and `res/values-night/colors.xml` (dark) from the charcoal brand (`#373737`/`#474747`) with one accent; remove `colorAccent=holo_blue_dark`, `android:colorForeground`, and the divergent `bottomNavigationStyle`s. Keep `alertDialogTheme` intent (dialog buttons legible in dark).
2. `TagMasterApplication.kt:22` — `DynamicColors.applyToActivitiesIfAvailable(this)` after `setDefaultNightMode`. Preserve `themeMode` preference key `tagmaster.theme` and the Settings radio behavior.
3. Remove `android:tint="#FFFFFF"` from the three FABs; remove `textColor="?android:attr/textColorPrimary"` overrides in `tagitemview.xml:180,196`, `videodisplay.xml:128`.
4. Edge-to-edge: delete `windowOptOutEdgeToEdgeEnforcement` and `navigationBarColor`; in a shared base or each activity call `WindowCompat.setDecorFitsSystemWindows(window,false)` and apply insets: top → toolbar, bottom → `BottomNavigationView`/tabs padding, list `paddingBottom`, FAB margins; IME → search root. Verify with `tagmaster-android-browse.png` re-capture (no strip under the bar).
5. Title font: keep `makeTitleString` (`Utilities.kt:38-44`) but cache the `Typeface` in a lazy val.

### Bundle D — Navigation and controls

1. `tagmasterview.xml`, `tagdetailview.xml` — replace `BottomNavigationView` with `com.google.android.material.tabs.TabLayout` above/below the `ViewPager2`; wire with `TabLayoutMediator` using the existing menu strings and icons. Keep fragment classes and order (`TagBrowserActivity.kt:41-56`, `TagDetailActivity.kt:129-143`), keep the `Visibility` binding on the tab strip (`TagDetailActivity.kt:152`). Retire `BottomNavigationUtils.kt` for Tag Master (leave the file for other consumers). Update Espresso tests that reference `R.id.bottomNavigation` (`MeActivityTest.kt`, `TagDetailActivityTest.kt`, `TagSearchActivityTest.kt`) to the new tab id.
2. Up navigation: add `android:parentActivityName=".MeActivity"` to `TagBrowserActivity`, `TagSearchActivity`, `SettingsActivity`, `TeachableTagsActivity`; for `TagDetailActivity`/`SheetMusicActivity`/`TagSearchResultsActivity` enable `setDisplayHomeAsUpEnabled(true)` and finish on Up (they can be entered from deep links). Route existing `HOME_MENU_ITEM_ID` handlers through `onSupportNavigateUp`.
3. Progress: replace every `ProgressDialog` with a `LinearProgressIndicator` (`android:id="@+id/progress"`) under the toolbar or a `CircularProgressIndicator` in the initiating control; bind to existing loading state (`QueryModel.isLoading`; add a `TrackableBoolean isLoading` to `TagDetailActivity`, `MeHeaderView`, `TagSummaryFragment`, `MediaPlayerView`). Preserve `safeDismiss` semantics (no crash if the activity is gone).
4. Feedback: replace the Toast call sites (listed in P2) with a `Snackbar` helper; give `Could not load a random tag` a `Retry` action and the no-match message a `Settings` action.
5. Media player (`mediaplayerview.xml`, `MediaPlayerView.java`): icon `MaterialButton`s for play/pause (toggle icon) and stop (48 dp each, content descriptions `@string/Play`/`@string/Pause`/`@string/Stop`), `Slider` for position with the time label, labeled `Slider` for balance (`@string/Balance`). Keep the `RemoteLocation`, `ContentCache`, `setBalance` math, and `IsPlaying` bindings.
6. Search/settings inputs: `TextInputLayout`+`TextInputEditText` for the search box (keep id `searchTextBox`, hint `@string/SearchBoxHint`, `imeOptions=actionSearch`, the `EditTextTextProperty` binding in `TagSearchActivity.kt:38-39`); exposed dropdown menus for the five search options and four random-tag filters, selecting by index (keep the `@array/*Choices` and `QueryModel`/`SettingsModel` semantics); `MaterialSwitch` for `sheetMusicWakeLockCheckBox` (keep the two-way `CompoundButtonCheckedProperty` binding); `MaterialButtonToggleGroup` for the theme radios (keep ids `radio_system/light/dark` or adapt `SettingsActivity.kt:269-313`).
7. Open Tag dialog (`MeHeaderView.kt:137-163`): `MaterialAlertDialogBuilder` with a `TextInputLayout` view (hint `@string/TagId`, `inputType=number`, 24 dp horizontal margins); keep `openTag(id)` and the `TAG_ID_EXTRA` intent.

### Bundle E — Discoverability of lists

1. `savedtagitemview.xml` / `SavedTagItemView.kt` — add a trailing overflow `MaterialButton.Icon` (`ic_more_vert`, 48 dp, contentDescription `More options`) that opens the same context menu; keep long-press as a secondary path. Preserve `FavoritesModel`/`TeachableTagsModel` operations and ordering.
2. `MeHeaderView.kt:183-187` — remove the size-based visibility binding so `Teachable Tags` is always shown; `teachabletagsview.xml:21-27` — center the empty state with padding, `bodyLarge`, and a second line explaining `Mark as Teachable` lives on a tag's toolbar. Update `MeActivityTest`/`FavoritesFlowTest` expectations if they assert the hidden state.

### Bundle F — Performance

1. `barbershop/Tag.kt:324-345` — perform the cache file read/deserialize inside `Task.callInBackground` (keep the `SoftReference` memory cache path synchronous). `SavedTagItemView.kt:55-65` — no change needed after that, but scope the coroutine to the view (cancel on detach).
2. `meview.xml:42-50`, `teachabletagsview.xml:35-44` — `RecyclerView` + `ListAdapter` keyed by tag id, replacing `ItemsControl`; keep `FavoriteTagItemView`/`TeachableTagItemView` as row views.
3. `LoadingImageView.java` — Coil `ImageView.load(url) { size(...); memoryCachePolicy }` or an `LruCache` + `inSampleSize`; keep the `Source` property for Bindroid.
4. `MediaPlayerView.java` — `prepareAsync` with `OnPreparedListener` start; `timer.cancel()` in `onDetachedFromWindow`; use `Handler.postDelayed` at 250 ms instead of a 25 ms `Timer`.
5. `ListModel.kt:59-64` / `Preferences.java:185,191` — `apply()`; `SettingsActivity.kt:93` — compute cache size on IO and post the result.

### Bundle G — Adaptivity

1. Add `res/layout-sw600dp/` variants (or `ConstraintLayout` max-width wrappers) for `tagqueryview.xml`, `tagsummaryview.xml`, `tagmiscview.xml`, `tagsearchview.xml`, `settingsview.xml`, `meview.xml` with content capped at ~640 dp and centered.
2. On expanded width, present Browse/Detail tabs as a `NavigationRail` (or keep tabs; never a phone bottom bar).
3. Landscape: two-column summary (`metadata | lyrics/notes`) and a side-by-side tracks layout (parts list | player).

### Must NOT change

- **Intent/data contracts**: `TagDetailActivity.TAG_ID_EXTRA = "depollsoft.tagmaster.tagid"`; `TagQueryFragment.QUERY_MODEL = "QueryModel"` and the `JsonSerializer` shape of `QueryModel`; `SheetMusicActivity` intent (`tagId` extra, `content://<applicationId>/<type>/<base64 uri>/<id>.<type>` with `FLAG_GRANT_READ_URI_PERMISSION`); `ContentCacheFileProvider` authority `${applicationId}`.
- **Deep-link semantics**: `UrlHandlerActivity.canHandleUri` rules (`dbpage.php`, `pg=view`, `dbase=tags`, `id`); the `http` filters must stay (add `https`, do not replace); `Tag.getTagUri` output `http://tags.depoll.com/tag.php?id=`; share text in `TagDetailActivity.shareIntent`.
- **Persistence keys**: `tagmaster.lists`, migration keys `tagmaster.Favorites`/`tagmaster.TeachableTags` (`ListModel.kt:97, 121-145`), `tagmaster.RatedIds`, `tagmaster.theme`, `SettingsModel` keys (`tagmaster.MinimumRandomTagRating`, `tagmaster.MinimumRandomDownloadsKey`, `tagmaster.SheetMusicRandomKey`, `tagmaster.LearningTracksRandomKey`, `tagmaster.WakeLockOnSheetMusic`), the `TagCache` files directory and `Tag.appVersion` check, the `JsonSerializer` alias for `TrackableCollection` (`TagMasterApplication.kt:15-18`).
- **Account/sync**: Firebase Auth providers (email, Google, Facebook) and `AuthUI` flow in `SettingsActivity.kt:227-253`; Firestore document `users/{uid}` with `lists.favorite` / `lists.teachable` arrays and the merge/delete semantics in `ListModel.kt:59-82, 158-226`; `connectToFirestore` on auth state change; the `LogInInfo` copy about backup and sync.
- **BarbershopTags.com API**: `API_URI_STRING` with `client=TagMaster`, query parameters and `fldlist` in `Tag.kt:225-268, 415-440`, the rating endpoint in `Tag.rate`, and the feed date/format expectations (parsing must become more lenient, never stricter).
- **Terminology and attribution**: tags, learning tracks, parts, tenor/lead/baritone/bass, favorites, teachable tags, Classic Tags, Easy Tags, `Content provided by BarbershopTags.com`, `Terms of Use`, `Donate`, `DepollSoft`, app name `Tag Master` (and `Tag Master β` for the private flavor), the Wickhop handwriting title.
- **Product behavior**: random-tag filters and defaults (`SettingsModel.kt:23-27`), favorites/teachable ordering and move semantics, `Refresh` clearing cached media for the tag, keep-screen-on default `true`, theme choice (Default/Light/Dark), search options and sort choices, offline cache and `Clear cache`, changelog on first run of a version.
- **Launcher and component names**: `depollsoft.tagmaster/.MeActivity` as the launcher, existing activity class names (referenced by Espresso tests and the private build tooling).

## Screenshots captured

All under `.impeccable/review/`, emulator-5554 (Android 16, 1080×2400). Font scale 1.0 unless noted; "dark" = night mode on, otherwise light.

| File | Shows |
| ------ | ------- |
| `tagmaster-android-home-dark.png` | Home (Me) in dark: text actions, one favorite showing `Posted: 12/31/69`, footer links, low-contrast search FAB |
| `tagmaster-android-home.png` | Home in light |
| `tagmaster-android-home-large.png` | Home at font scale 1.3 (light): scales without clipping |
| `tagmaster-android-browse-dark.png` | Browse → Latest in dark: list rows, bottom navigation used as tabs, every date 12/31/69 |
| `tagmaster-android-browse.png` | Browse in light: strip under the bottom bar in the gesture area |
| `tagmaster-android-browse-large.png` | Browse at 1.3× (light) |
| `tagmaster-android-search-dark.png` | Search + Search Options (dark): bare EditText, Spinners |
| `tagmaster-android-search-keyboard-dark.png` | Search with IME open: `adjustResize` keeps FAB and options visible |
| `tagmaster-android-search-results-dark.png` | Search results for `lida` (dark) |
| `tagmaster-android-search.png` | Search in light |
| `tagmaster-android-search-large.png` | Search at 1.3× (light) |
| `tagmaster-android-detail-summary-dark.png` | Tag detail Summary (dark): rating bar, Key button, Sheet Music link, bottom nav tabs |
| `tagmaster-android-detail-summary.png` | Summary in light |
| `tagmaster-android-detail-summary-large.png` | Summary at 1.3× (light) |
| `tagmaster-android-detail-details-dark.png` | Tag detail Details tab (dark) |
| `tagmaster-android-detail-tracks-dark.png` | Tracks tab (dark): PLAY/STOP toggles, unlabeled sliders, part radio buttons |
| `tagmaster-android-detail-tracks.png` | Tracks in light |
| `tagmaster-android-detail-tracks-large.png` | Tracks at 1.3× (light) |
| `tagmaster-android-detail-videos-dark.png` | Videos tab (dark): thumbnails, `Wednesday, December 31, 1969` on every video |
| `tagmaster-android-sheetmusic-dark.png` | Sheet music viewer (dark) with `Major:F` Extended FAB |
| `tagmaster-android-sheetmusic.png` | Sheet music viewer in light |
| `tagmaster-android-rating-dark.png` | Custom rating dialog (dark): five RatingBars, no actions |
| `tagmaster-android-opentag-dark.png` | Open Tag dialog (dark): raw EditText without hint or margins |
| `tagmaster-android-settings-dark.png` | Settings (dark) |
| `tagmaster-android-settings.png` | Settings in light |

Not captured: Teachable Tags list and its empty state (the home entry is hidden while the list is empty and marking a tag would have mutated app data); Random Tag progress dialog (transient); tablet/landscape (no tablet emulator; adaptivity findings are source-only).
