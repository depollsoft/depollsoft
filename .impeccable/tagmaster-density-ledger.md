# Tag Master density and separate lists

## Requested now

The user approved pushing the restored-character correction. Next: improve information density and separate Favorites from Teachable Tags so overlapping memberships do not duplicate rows on Home or bury a section after a long feed.

Spatial thesis: discovery actions first, then Your lists as two equal disclosure rows with counts. Each opens one titled list. Home never grows with tag count. Catalog/saved rows prioritize title, ID and material availability; other metadata remains on the tag page. Native type scales and touch targets stay intact.

## Future only

The user plans multiple labels per tag and many named lists, including the two built-ins. Do not implement labels, custom lists, label editors, new storage types, migrations or sync fields. Use a repeatable list-entry presentation now, not fixed side-by-side sections or a two-position tab switcher. Membership may overlap; never deduplicate across the actual lists.

## Acceptance

- [x] Home shows exactly Favorites and Teachable Tags as peer list entries; no inline tag feeds, regardless of 0/1/100 saved tags.
- [x] Each destination shows its own title, count/empty state, existing order and removal/reordering behavior. Home's orphan Edit action moves to Favorites.
- [x] A tag in both memberships appears once in each separate destination and never twice on Home. Changing one membership leaves the other intact.
- [x] Compact catalog/saved rows share title/ID/material hierarchy, preserve alternate titles and full metadata on Detail, and expose materials as status rather than toggles.
- [x] Automatic row height, 44pt/48dp minimum targets, long titles/large text and tablet readable widths. Full barber-pole background and simple push/Back flow remain intact.
- [x] No model/migration/backend or custom-label feature changes; planned extensibility documented in scoped design records.
- [x] Focused hard tests and native populated-list captures on both platforms, one batched visual check and correction if needed.

Assessment: `.impeccable/review/tagmaster-density-assessment.md`. The native screen/code scan substitutes for the HTML/CSS detector, which does not apply.

## Verification and review

- iOS: 14 distinct unit checks and four distinct phone UI tests have passing incremental evidence; the populated flow also passed on iPad. The final continuation ran six executions with zero failures. Native default rows measure 61pt, and large text/long titles grow without clipping. Fixtures are Debug-only, bypass Firebase, and recover on ordinary next Debug launch after interruption.
- Android: 56 distinct JVM checks have passing evidence across the initial run and three failure-only reruns. Six native executions cover four distinct tests across phone light/dark-large and an 800dp runtime tablet configuration. Native-renderer unit geometry measures a 61dp default row.
- Independent Astra review disposition: ship for density/list separation, no material fixes. This is not whole-app release certification.
- New Favorites destinations and test sources are registered. Last four-file Swift/Kotlin diagnostics batch had zero findings; diff checks pass. List models, storage keys, sync, migrations, backend and Pitch Perfect source remain unchanged.
- Reports and compact representative captures are under `.impeccable/review/density/`. Saved-list data in captures is explicitly synthetic; Android Browse uses the live catalog. Full local logs/result bundles are not committed.

## Remaining limits

No hardware audio, signed-in sync, complete assistive-technology traversal or full device/OS matrix was rerun. Android preference archive hashes differ and do not prove byte-identical content restoration; the original backup is unavailable, so this uncertainty is preserved rather than reported as a match. Fixture membership changes were memory-only and account writes were disabled. Custom labels and additional lists remain future work.
