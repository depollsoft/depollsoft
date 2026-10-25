---
target: Pitch Perfect on iPhone Duo
total_score: 21
max_score: 32
na_heuristics: 9,10
p0_count: 0
p1_count: 2
target_identity: "file:/Users/depoll/Code/depollsoft/.claude/worktrees/ios-iphone-duo/iOS/pitchperfect/pitchperfect/PitchInstrument.swift"
target_fingerprint: "sha256:3eb684914d6200f4648350f88b71ba097ad39fd2ee004bb2d55eb42cfd2189bb"
target_path: /Users/depoll/Code/depollsoft/.claude/worktrees/ios-iphone-duo/iOS/pitchperfect/pitchperfect/PitchInstrument.swift
timestamp: 2026-10-25T09-12-15Z
slug: erfect-pitchperfect-pitchinstrument-swift-a1afbac5
---
Method: eight independent reviews (Claude Opus and Codex Astra on each of Tag Master design, Pitch Perfect design, a native technical audit and a code review); design review only, the detector does not apply to native iOS. Scope: Pitch Perfect on iPhone Duo (PR #98).

## Design health (Opus / Astra)

| # | Heuristic | Score | Key issue |
|---|---|---|---|
| 1 | Visibility of system status | 3 / 3 | Raised display reports note, Hz, interval and chord; silent to VoiceOver |
| 2 | Match system / real world | 3 / 4 | Bench instrument with its display raised |
| 3 | User control and freedom | 3 / 3 | Folds reversible |
| 4 | Consistency and standards | 2 / 3 | Classic grid puts keys on the standing half; tabletop and book differ in size |
| 5 | Error prevention | 2 / 2 | 22 pt range rows; bottom cell's touch zone near the banner in tabletop |
| 6 | Recognition rather than recall | 2 / 2 | Icon-only tabs in the side bar |
| 7 | Flexibility and efficiency | 3 / 3 | Chords, toggle mode, classic grid |
| 8 | Aesthetic and minimalist design | 3 / 3 | Tabletop voids; idle window reads as an empty box |
| 9 | Error recovery | n/a / 3 | No new error states |
| 10 | Help and documentation | n/a / 2 | Nothing new to explain |
| | Total | 21/32 / 28/40 | Acceptable, near Good |

Audit (0-20): Opus 16, Astra 13.

## Design specificity verdict

Authored for this product: the raised display is built from the plate's own parts and One-Glow survives. The ring-derived window size (ac4a49a7) is right; tabletop read as two objects with a hinge between them.

## Priority issues

- [P1] Tabletop ring at 45 pt cells with the banner under it (bottom touch zone about 9 pt above the ad). Partly fixed (cells now 49 pt); banner placement open.
- [P1] A narrow split could leave 27 pt cells. Fixed: split refused under 44 pt targets.
- [P2] Tabletop display floated 94 pt above the crease. Fixed: seated just above it.
- [P2] Folded range rows 22-29 pt tall. Fixed: 44 pt targets.
- [P2] Raised display silent to VoiceOver. Fixed: "Display" element; layoutChanged on fold.
- [P2] Fold keep-out doubled (region frame includes margins). Fixed.
- [P2] Notes and Keys open with middle C on the crease in tabletop. Open (List ignores custom scroll anchors).
- [P2] Classic grid puts half its keys on the standing half in tabletop. Open (design decision).
- [P2] Book lists split each row's two halves across the crease. Open.

## Persona red flags

- Sam (VoiceOver): readout now spoken in folded poses; still silent on the flat face (inherited).
- Casey (one hand): icon-only tabs; Songs is a speaker glyph.
- Chorus director: book works; tabletop cells smallest of any pose and near the ad.

## Minor observations

- Idle window "— Hz" at about 2.4:1 reads like an empty field; A-sharp spelling of a barbershop seventh and the sharp glyph's fallback font; Settings sheet docked against the crease half open (system placement); classic range control slid during the crossfade (fixed).

## Questions

- Should the banner move to the top of the upper half in tabletop?
- Should the classic grid put all twelve keys on the lower half in tabletop?
