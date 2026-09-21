---
target: our changes (Tag Master custom tag lists)
total_score: 25
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 2
target_identity: "file:/Users/depoll/Code/depollsoft-tag-lists/Android/TagMaster/src/main/res/layout/meviewheader.xml"
target_fingerprint: "sha256:c9b11c14dd2755d9feb188c0fe20711502bf68d6be1bd88dd6028cc4b10da2b4"
target_path: /Users/depoll/Code/depollsoft-tag-lists/Android/TagMaster/src/main/res/layout/meviewheader.xml
timestamp: 2026-09-21T05-40-32Z
slug: ster-src-main-res-layout-meviewheader-xml-95db11c6
---
Method: dual-agent (A: design review sub-agent · B: detector and device evidence sub-agent). Target: Tag Master user-defined tag lists (Home Lists group, list screens, detail chips, picker) on branch tagmaster/custom-tag-lists.

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Toggling an existing list in the Android picker gives no confirmation; nothing says a list reached the cloud |
| 2 | Match System / Real World | 2 | Community vocabulary appears in one helper string; the rest is office-software wording |
| 3 | User Control and Freedom | 3 | Undo only for Android chip removal; deleting a list is unrecoverable |
| 4 | Consistency and Standards | 2 | Chip "Teachable" vs "Teachable Tags"; Android Home EDIT edits favorites only while iOS Edit covers lists |
| 5 | Error Prevention | 3 | Inline name validation, reserved names blocked, delete confirmation counts tags |
| 6 | Recognition Rather Than Recall | 2 | Android Home list rows have no affordance for the long-press that holds Rename/Move/Delete |
| 7 | Flexibility and Efficiency | 2 | No way to add tags from inside a list; Android reorders lists one step per long-press |
| 8 | Aesthetic and Minimalist Design | 3 | "Lists" heading is the same size as its rows and lighter |
| 9 | Error Recovery | 3 | Specific name errors; no sync-failure state |
| 10 | Help and Documentation | 2 | Nothing explains how a list differs from Favorites or Teachable Tags |
| **Total** | | **25/40** | **Adequate** |

## Design Specificity Verdict
Mostly category-interchangeable; the only authored moment is the name hint "For example “Afterglow set”". The sing-through scene is served by code (chevron stepping) but never named. New lists append to the bottom. Favorites is a list in picker/chips but not in the Lists group. iOS chips are tinted fills, Android chips neutral outlines.
Deterministic scan: 6 design-system-font warnings (list_row.xml:40; meviewheader.xml:36,69,107,159,207), all false positives (DESIGN.md is Pitch Perfect-scoped; 4 of 6 pre-date the branch). Detector accepts one path per run and does not recurse directories. Browser overlays not applicable (native apps); emulator evidence (dark, 1.3x font, landscape, tablet) substituted.

## Priority Issues
- [P1] Android Home list rows hide their only management path (long-press, no affordance; EDIT edits favorites only; popup anchored flush to screen edge). Fix: extend Home edit mode over the Lists group as iOS does, or add a trailing overflow button per row. (/impeccable adapt)
- [P1] No way to add tags from inside a list screen. Fix: "Add tags" action (FAB / + bar button) opening Browse or Search in pick-into-list mode; Random Tag in the empty state. (/impeccable shape)
- [P2] Chip removal under-target on Android (~34×32dp close icon) and invisible on iOS (context menu only). Fix: 48dp remove target; visible remove affordance on iOS. (/impeccable audit)
- [P2] Cross-platform parity: chip label, picker row icons/counts, chip fill vs outline, undo on iOS, list reorder mechanism. Fix: one spec across both. (/impeccable polish)
- [P2] Large text + landscape: Android picker clips rows and hides New list… with no scroll; single-line Home row names; fixed 220dp chip cap. Fix: scrolling picker body, two-line names, font-scaled chip cap. (/impeccable adapt)

## Persona Red Flags
- First-timer at an afterglow: Add to list collapses into overflow on Android phones; 4–6 taps plus keyboard to add to a new list.
- Teacher with 15 lists on iPad: 17-row popover picker without search; newest list always last; Favorites pushed below the fold.
- Screen-reader user: well served overall; Android picker rows announce only "In X"/"Not in X"; iOS has no heading for the Lists group.
- Older singer with large text: landscape picker clipping, single-line names, fixed chip cap.

## Minor Observations
- Teachable Tags count now shown on both platforms (screenshots predated the fix).
- Android list overflow popup uses the pale lilac Material popup surface (theme-level, pre-existing).
- "This list is empty." delete message is a statement, not a confirmation.
- Picker counts describe the list, beside a checkmark that means membership.
- iOS swipe-to-delete on a list row reloads then alerts, looking like a failed swipe.
- Pre-existing: search FAB overlaps Home footer links in dark theme, at 1.3x, and on tablet (footer row missing there).
- Long list names scroll out of the single-line Android name field.

## Questions to Consider
- Why is "Sing through this list" not the primary action on a non-empty list?
- Should Favorites simply be the first, undeletable list in the Lists group?
- Should lists be shareable, given the afterglow is a group decision and tags already are?
