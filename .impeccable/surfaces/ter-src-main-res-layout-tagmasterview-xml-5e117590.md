---
version: 1
slug: "ter-src-main-res-layout-tagmasterview-xml-5e117590"
primary_target: "Android/TagMaster/src/main/res/layout/tagmasterview.xml"
related_targets: ["iOS/tagmaster/tagmaster/DPAppDelegate.m","Android/TagMaster/src/main/java/depollsoft/tagmaster/TagDetailActivity.kt","iOS/tagmaster/tagmaster/DPTagViewController.m"]
---

# Tag Master — tablet list-detail surface (Android + iPad)

Scope: every tag list in Tag Master on wide windows (Browse's four tabs, Search results, Home favorites, Teachable Tags) plus the tag detail they open; visitor mode Operate.
Audience/job: a singer or teacher with a tablet on a music stand or a lap at an afterglow, stepping from tag to tag while the group decides what to sing next; alone, a student working through a teachable list on the Tracks page. The list must never leave the screen while a tag is open.
Direction: extension of the established Tag Master world (charcoal chrome, one blue accent, barber-pole watermark, Wickhop handwriting brand title, quartet-on-the-staff illustration). No new identity. Same product behavior and words on both platforms; chrome stays native (Material 3 list-detail on Android, UISplitViewController on iPad).
Memorable moment: tap a tag and it opens beside the list; the row it came from stays lit; the up/down chevrons (and ⌘↑/⌘↓ on an iPad keyboard) walk the list without leaving the page you were on, so a teacher on Tracks hears the next tag's lead part two taps later.
Constraints: phones keep today's full-screen navigation untouched; Android two-pane only at ≥720dp wide and ≥480dp tall (never phone landscape); iPad regular width only; the Summary/Details/Tracks/Videos page survives a tag change; existing tests keep passing; deep links and Random Tag keep their contracts; no chromatic accent fills beyond the existing blue; no colored side bars on rows.
Unresolved: none.

## Direction contract

THESIS: A tag list on a tablet is a set list on a music stand: it stays in view while one tag is open beside it, and the open tag is the lit line on that list. Refused: the phone stack scaled up (full-screen detail that hides the list) and a generic split with an unlit list.

OWN-WORLD: Tag Master's incumbent world, unchanged. Charcoal (#373737) bars with white type; one blue accent (#007AA3 light / #5AC8FA dark) for actions and the tab indicator; barber-pole watermark behind every pane; Material 3 roles on Android (selected row = colorSecondaryContainer fill, full-bleed, no bar); iOS semantic colors (selected row = tint at 14 percent light / 22 percent dark, full-bleed); 1dp/1px outline-variant hairlines; Wickhop only for the "Tag Master" brand title on the list side; tag titles in the system face on the detail bar.

STORY: The visitor sees the list and an invitation ("Pick a tag") beside it; taps a tag; the row lights and the tag opens beside it on the page they last used; chevrons or arrow keys walk neighbours; the list scrolls to keep the lit row visible; nothing they were looking at disappears.

FIRST VIEWPORT: Left pane 360dp (Android) / system primary column (iPad): the list exactly as on phones, own bar (brand title, list actions). Right pane: its own charcoal bar carrying the tag title and, right-aligned, previous/next chevrons then favorite, teachable, refresh, share (overflow as needed). Below the bar: the detail pages and their bottom page tabs, same as phones. Before any selection the right pane shows the quartet staff at rest, "Pick a tag" and "Choose a tag from the list. Its summary, tracks, sheet music, and videos open here." centered over the watermark. A 1dp hairline separates the panes on Android; iPad uses the system column divider.

FORM: List-detail split (canonical layout) — local extension of an established surface; no concept seed rolled (shape-direct per new-work.md).

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.

## Shipped

Recorded from the built code and the review captures (`.impeccable/review/tablet-android-*.png`, `tablet-ios-*.png`), not from the plan. The incumbent Tag Master world is unchanged; these are the durable decisions this surface adds to it.

- Two-pane gate: Android `layout-w720dp-h480dp/` only (`tagmasterview`, `tagqueryactivity`, `meview`, `teachabletagsview`); list pane `@dimen/list_pane_width` = 360dp, detail pane takes the remainder. iPad: `UISplitViewController` double-column, tile behavior, primary column 320–400pt at a 0.36 fraction (`DPAppDelegate.m`). Phones and phone landscape keep the full-screen stack.
- One charcoal band: the detail pane's `AppBarLayout`/`MaterialToolbar` (`tag_detail_pane.xml`) uses the same `@color/brand_chrome` (#373737), `ThemeOverlay.TagMaster.Chrome`, 0dp elevation and `liftOnScrollColor` as `app_toolbar.xml`, so both bars read as a single band. iPad applies the shared opaque `UINavigationBarAppearance` (white 55/255, white titles, `barStyle` black, non-translucent) to the detail navigation bar.
- Pane divider: Android draws a 1dp `?attr/colorOutlineVariant` hairline (`pane_hairline.xml`, #C7C7C7 light / #474747 dark) down the detail container's start edge, below the bar only. iPad uses the system column divider.
- Lit row: Android `tag_row_background.xml` is a ripple over a selector; `state_activated` fills the row full-bleed with `?attr/colorSecondaryContainer` (#CDE9F7 light / #004D6B dark), no inset, no side bar, no elevation; `TagItemView` sets `isActivated` and a "Showing" state description from the pane's selection. iOS `DPTagCell` uses a `selectedBackgroundView` of `systemBlue` at 0.14 (light) / 0.22 (dark) and adds `UIAccessibilityTraitSelected`.
- Detail-bar order, both platforms: previous chevron, next chevron, favorite, teachable, refresh, share. Android `tagpanemenu.xml` shows chevrons `always` (`ic_chevron_up`/`ic_chevron_down`, 24dp vectors tinted `brand_on_chrome`), the rest `ifRoom` into the overflow; disabled chevrons drop icon alpha to 97/255. iOS uses SF Symbols (`heart`/`heart.fill`, `person.2`/`person.2.fill`) as direct toggles beside the list and the action sheet only when collapsed.
- Stepping: Android Ctrl+Up/Down (`TagPaneController`, DPAD keycodes with Ctrl, no key repeat); iPad Cmd+Up/Down `UIKeyCommand`s. Chevrons hide until a tag is open; the list reveals the lit row on each step.
- One watermark per window, not per pane: Android's four two-pane layouts draw a single `ic_barberpole` (`@id/paneWatermark`) on the root below the bar, and the list pane, empty pane and `TagDetailFragment` (when hosted in a pane) draw none; iPad installs one `TMLogoBackgroundView` on the split view (`+[DPAppDelegate installSharedBackgroundIn:]`) and `setUpBackground:` leaves column screens clear. Phones keep a watermark per screen.
- Empty pane (`tag_pane_empty.xml` / `TMTagPlaceholderController`): over the shared watermark, quartet staff at rest (216x96dp; notes in the accent blue #007AA3 light / #5AC8FA dark on both platforms, staff lines at 29% grey), "Pick a tag" in `textAppearanceTitleLarge` / semibold Title2, body in `textAppearanceBodyMedium` on `colorOnSurfaceVariant` / Body on `secondaryLabelColor`, 12dp then 8dp stacking, 480dp max text width, 32dp padding.
- Words are shared verbatim across platforms (`strings_tablet.xml`): "Pick a tag", "Choose a tag from the list. Its summary, tracks, sheet music, and videos open here.", "Previous tag", "Next tag", "Showing".
- Assets: vector drawables and SF Symbols only; no shipping rasters, so no provenance embedding is due.
