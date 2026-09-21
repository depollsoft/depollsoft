---
target: the set lists changes (Songs tab, both platforms)
total_score: 20
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
target_identity: "file:/Users/depoll/Code/depollsoft-set-lists/Android/PitchPerfect/src/main/res/layout/songlistview.xml"
target_fingerprint: "sha256:058169fa506b640869279cd9a87f4d61324b809580a05faf0d4dae2083b56d12"
target_path: /Users/depoll/Code/depollsoft-set-lists/Android/PitchPerfect/src/main/res/layout/songlistview.xml
timestamp: 2026-09-21T05-36-21Z
slug: fect-src-main-res-layout-songlistview-xml-9a0e2597
---
# Critique: Pitch Perfect set lists (Android + iOS Songs tab and its management screens)

Method: dual-agent (A: design review with emulator screenshots · B: detector scan). Android evidence from the PiMobile emulator (dark, light, font scale 1.3); iOS judged from source only.

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2 | Add Song / Add songs never name the target list; Set Lists never marks the current list |
| 2 | Match System / Real World | 3 | "Set list", "My Songs", counts read right; "tap the pencil" understates a three-step path |
| 3 | User Control and Freedom | 2 | Deleting a set list is one confirm and irreversible; no undo |
| 4 | Consistency and Standards | 1 | Chromatic error red and a lit control while nothing sounds both break DESIGN.md |
| 5 | Error Prevention | 2 | Set Lists row tap opens Rename, not switch; long-press menus never name their target |
| 6 | Recognition Rather Than Recall | 2 | "+" is 13sp secondary ink in mostly empty frame; picker rows show no unselected affordance |
| 7 | Flexibility and Efficiency | 2 | Switch is one tap; no select-all in the picker; no switch from Set Lists |
| 8 | Aesthetic and Minimalist Design | 2 | Frame 60 % empty with one list; breaks on overflow (Android) |
| 9 | Error Recovery | 2 | Validation copy excellent, rendered in Material red |
| 10 | Help and Documentation | 2 | Empty-state instruction disappears after a switch (bug) |
| Total | | 20/40 | Needs work before release |

## Design Specificity Verdict

The selector is authored: the range selector's construction on a new axis; one-tap switch with the selection tick is right for a singer with one thumb. The instrument thinking stops at the frame's edge: name dialog, error state, long-press menus, Set Lists, and Add songs are stock Material/UIKit on the plate, and two Android surfaces break named design-system rules. With one list the part is two-thirds empty frame; on overflow the Android frame breaks.

Detector: 2 findings, both `design-system-font` on the Oswald reference (`songlistview.xml:30`, `addsongsfromlistview.xml:38`); both false positives, DESIGN.md declares Oswald Medium as the display face. Swift accepted with zero findings but the detector does not parse UIKit idioms. No browser overlay (native).

## Priority Issues

- [P1] Empty state never returns after switching lists (Android). `SongListFragment.kt` binds visibility once via Bindroid on `currentList.songs[0]`; `currentListId` is a non-trackable preference so the binding never re-evaluates. Fix: set visibility imperatively in `applyEmptyStateCopy()`. iOS is correct. (/impeccable harden)
- [P1] Name validation renders in chromatic red (Android). `TextInputLayout` takes `colorError` from the theme; `ThemeOverlay.Plate.Dialog` does not set it. Fix: `colorError`, `errorTextColor`, `boxStrokeErrorColor`, null `errorIconDrawable`; also disable the elevation overlay so the dialog surface is `plate_surface`. (/impeccable polish)
- [P1] Add songs confirm button fully lit while disabled (Android). `Widget.Plate.PrimaryButton` has a flat `plate_accent` tint with no disabled state; the app-bar checkmark too. Fix: ColorStateList with a disabled branch, or steel surface. (/impeccable polish)
- [P2] The machined part breaks under its real states (Android): 60 % empty frame with one list; on overflow the frame's edges vanish behind the selected position and a label draws through the rounded corner; no scroll affordance. iOS stretches and clips correctly. Fix: last position fills the row, frame drawn above clipped content, end fades. (/impeccable layout)
- [P2] Long-press menus never name their target; management has three homes (selector long-press, edit overflow, Set Lists rows). Fix: title the menu with the list name; selector as primary home; cut the edit overflow to Sort, Add songs from another set list, Manage. (/impeccable distill)
- [P2] Set Lists row tap renames instead of switching; no current-list marker. Fix: tap switches and pops; Rename in the row overflow; lit dot on the current row. (/impeccable clarify)
- [P3] Selector positions 40dp and Set Lists row controls 44dp (under 48dp); lit dot touches the label's first glyph. Fix: 48dp row; pad the selected label by dot + 6dp. (/impeccable adapt)

## What's Working

- The switch: one tap, rows swap, notes stop, selection tick.
- The copy: delete confirmation and validation lines name the thing and count the cost.
- The dialog button fix: Cancel/Create legible in dark mode (surface still tinted by the elevation overlay).

## Persona Red Flags

- Performing singer, dim stage, one hand: unselected labels ~3.5:1 at 13sp; 40dp targets; no scroll affordance; long-press menu does not name its list.
- First-time user with one list: mostly empty outlined box with a faint "+" reads as a search field or glitch; the rescuing empty state disappears on the next switch; "tap the pencil" is a three-step path.
- Screen-reader user: positions are correct; Add songs rows have no checkbox role/state, check icon described as "Add", no disabled cue on Add, no TalkBack custom action for the long-press menu.

## Minor Observations

- Selector labels start at 30dp, song titles at 20dp.
- iOS "+" at 20pt vs Android 13sp.
- iOS long-press actions have no SF Symbols; destructive alert actions render system red.
- DESIGN.md Song Editing still says Settings folds into the overflow on Songs; the build keeps the gear.
- Disabled "Add songs from another set list…" gives no reason.
- 13sp labels in a fixed 40dp frame will clip at font scale 2.0.
- Delete/Cancel carry identical weight; Snackbar undo would make delete recoverable.
- Changelog does not mention set lists.

## Questions to Consider

- What if the frame were only as wide as its positions, like the range selector?
- Does management need three homes?
- What would the name dialog look like engraved into the plate rather than floating as a Material card?
