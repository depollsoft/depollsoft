# Tag Master density assessment

Method: focused single-context native screenshot/source review, not a full Impeccable critique. Root verified as `/Users/depoll/.local/share/pi-worktrees/20260907005637/tm-refine-worktree-20260907`. No implementation, builds, devices, CSS detector, or push. Only this report was written. Questions skipped because the user supplied the decision criteria and requested a bounded assessment.

## Decision

Use a Home **Lists** area with two equal, full-width disclosure rows: **Favorites** and **Teachable Tags**. Each pushes a separate, titled list. Remove both inline tag feeds from Home. Keep Find a tag, Browse, Random Tag and Open Tag ID, the restored barberpole, transparent rows and native push/back navigation.

This best matches the user's wording. Moving Teachable above inline Favorites is a smaller patch and fixes its buried entry point, but still gives Favorites a privileged, unbounded feed. It does not establish the peer-list structure the user wants for overlapping memberships and eventual many lists. Two destination rows cost Favorites one extra tap; in return Home never grows with saved-tag count, and the same tag cannot appear twice there. Do not deduplicate across the underlying lists: a tag legitimately belongs to both.

Keep both entries visible when empty. An optional trailing count belongs on the same row, not in another subtitle. Use one disclosure target, not a heading plus separate “See all.” Additional list names could later occupy additional rows in this visual pattern. Do not add expansion controls, labels, custom lists, editors, migrations or a generic list framework now.

## Evidence

Inspected each supplied image separately through native image read, at no more than 1000 pixels high:

- `character-correction/ios-phone-home.png`: empty Favorites, then Teachable Tags. This does not demonstrate populated iOS density. Source does: `DPHomeViewController.m:302–436` puts every favorite in section 1 and the Teachable destination in section 2. Its distance from the top therefore grows with Favorites. Home Edit currently edits/reorders favorites; that responsibility must move with the list.
- `character-correction/android-confirm-phone-light-home.png`: one favorite already consumes a title, two metadata lines and a large material-status line. Teachable starts well below it. `HomeFragment.kt` independently binds both complete collections; `homeview.xml` places both inside one scroll. Overlapping IDs will therefore render twice when both memberships are populated. The supplied image itself has no teachable items and does not visually prove duplication.
- `character-correction/android-confirm-phone-light-browse.png`: about five tag rows fit below the catalog controls. Adjacent separators are about 149 screenshot pixels apart, not a measured dp height. Repeated ID/Rating, Posted/DLs and checkbox-style material labels compete with titles. Every visible date reads 12/31/69; this review cannot determine whether that is fixture data or a date bug. It should not dominate scanning either way.
- Android catalog `TagItemView.kt` inflates `tagitemview.xml`; saved rows wrap that same view in `SavedTagItemView.kt` and `savedtagitemview.xml`. One compact row change reaches both. iOS `DPTagCell.m` similarly combines title, optional alternate title, ID/rating/downloads/date and two material markers.

Important scope correction: separate Teachable destinations exist on both platforms. A dedicated Favorites destination was not found; Favorites currently lives on Home. “Open existing list views” means reuse the existing saved-row and list behavior, not pretend two ready-made destinations already exist.

## Compact row priorities

1. Title first, native headline/title typography. Preserve wrapping and readable text size.
2. One quiet secondary line: short tag ID plus available Sheet music / Learning tracks indicators. Prefer small noninteractive material symbols with concise text over Android's checkbox-shaped status controls. Include explicit availability in accessibility labels; do not rely on color or announce these as toggles.
3. Alternate title only when nonempty and different, as an additional line when needed. Keep full names accessible; do not force an ellipsis to meet a height target.

Move rating, posted date and download count to Detail. Keep them available there and retain catalog sorting/filtering. Do not repeat “Favorite” within Favorites or “Teachable” within Teachable; the destination title supplies that context. Do not add membership pills now. If neither material exists, a brief “No materials” line is clearer than two disabled-looking controls.

Current iOS row padding is 12pt top/bottom, stack spacing 4pt, with an extra 8pt gap after facts. Android uses 12dp top/bottom and two 4dp gaps around metadata/material blocks; the status controls also contribute their native measured height. Android Home adds 32dp before each saved-list section and 8dp before each inline feed. Density is primarily a content problem, not just oversized margins.

Suggested default-size targets, not fixed heights:

| Element | iOS | Android |
| --- | --- | --- |
| Home list entry | roughly 48–52pt, 8pt vertical padding | roughly 48–56dp, 8dp vertical padding |
| Two-line tag row | roughly 60–68pt, 8pt top/bottom, 4pt line gap | roughly 64–72dp, 8dp top/bottom, 4dp line gap |
| Minimum interactive target | 44 × 44pt | 48 × 48dp |

Use automatic dimension / wrap_content with minimums, never height caps. Keep existing Dynamic Type and sp styles. Long titles, alternate titles and large fonts must increase height; secondary information may wrap or stack. Whole rows are tappable. Noninteractive status glyphs need not become separate 44pt/48dp controls. Preserve adequate spacing for any independent actions.

Replace the two large saved-feed section gaps with one Lists heading separated from discovery actions by about 16pt/dp. Keep 8pt/dp between heading and entries. Do not globally shrink spacing tokens or footer link hit areas to squeeze the entire Home onto one screen. Footer attribution remains below the entries.

## Long lists and tablets

Home always has the same two list entries regardless of item count. Each pushed screen owns its title, scroll and existing removal/reordering behavior; Back restores Home, and return from Detail should preserve the list position. This solves losing the section break without sticky pseudo-tabs or an index bar.

Keep a single centered readable-width column on tablets, not side-by-side Favorites/Teachable feeds. Android already uses 16dp gutters and a 600dp maximum, changing to 24dp/640dp at w600dp. iOS already follows readable-width cell margins. Retain these behaviors and the full-page barberpole. No sidebar, new tabs, opaque cards or tablet-only navigation invention. Large-font and tablet outcomes remain recommendations, not runtime-verified claims.

## Small implementation footprint for the next task

Prefer a narrowly scoped Favorites-only destination that reuses the existing row and favorite operations. Do not change data models.

Existing files needing the main work:

- `iOS/tagmaster/tagmaster/DPHomeViewController.m`: two peer destinations, remove inline favorites and Home Edit.
- `iOS/tagmaster/tagmaster/DPTagCell.m`: shared compact row and matching spoken information.
- `Android/TagMaster/src/main/java/depollsoft/tagmaster/HomeFragment.kt`: replace both feed bindings with destination actions.
- `Android/TagMaster/src/main/res/layout/homeview.xml`: Lists heading and two peer rows.
- `Android/TagMaster/src/main/res/layout/tagitemview.xml` and `Android/TagMaster/src/main/java/depollsoft/tagmaster/TagItemView.kt`: compact presentation plus corresponding binding changes, not hidden legacy metadata retaining space.

Small destination addition required, rather than an existing Favorites screen:

- Suggested `iOS/tagmaster/tagmaster/DPFavoritesViewController.swift`, reusing `DPTagCell` and the existing favorite APIs; register in `iOS/tagmaster/tagmaster.xcodeproj/project.pbxproj` if required by its project setup. Preserve the refresh notification behavior currently in `DPHomeViewController.swift`.
- Suggested `Android/TagMaster/src/main/java/depollsoft/tagmaster/FavoritesActivity.kt` and `Android/TagMaster/src/main/res/layout/favoritesview.xml`, reusing `FavoriteTagItemView`; register in `Android/TagMaster/src/main/AndroidManifest.xml`.
- Localized Lists/material/empty-state copy may require `Android/TagMaster/src/main/res/values/strings.xml` and the existing iOS localization resources. Exact localization/project registration was not audited in this bounded pass.

Reuse `DPTeachableTagsController.m` and `TeachableTagsActivity.kt` as behavior references, not as a generic framework. Check `savedtagitemview.xml`'s invisible sizing row when implementing density; it must not preserve a taller blank placeholder. No need to change models, sync, backend, migrations, Detail data or the restored background/navigation code.

## Acceptance checks for later implementation

- Home with 0, 1 and 100 saved tags still has exactly two peer list destinations and no inline tags.
- One tag in both memberships appears once in each separate destination, never twice on Home.
- Existing favorite and teachable ordering, removal, loading/failure states and Detail navigation survive; Home no longer exposes an orphan Edit action.
- Catalog and saved rows share the compact hierarchy; full metadata remains in Detail.
- Long titles and large fonts grow rows without clipping; minimum targets remain 44pt/48dp.
- Phone/tablet and light/dark preserve the barberpole and native push/back behavior. No labels/custom lists or virtual tabs ship in this pass.
