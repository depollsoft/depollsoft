---
target: Tag Master on iPhone Duo
total_score: 25
max_score: 36
na_heuristics: 9
p0_count: 0
p1_count: 1
target_identity: "file:/Users/depoll/Code/depollsoft/.claude/worktrees/ios-iphone-duo/iOS/tagmaster/tagmaster/TMShell.swift"
target_fingerprint: "sha256:25b2534769c4f4aaad250cbaef5261d7c9e6abab3d0b1eef2c9a9687f9408eba"
target_path: /Users/depoll/Code/depollsoft/.claude/worktrees/ios-iphone-duo/iOS/tagmaster/tagmaster/TMShell.swift
timestamp: 2026-10-25T09-12-15Z
slug: ios-tagmaster-tagmaster-tmshell-swift
---
Method: eight independent reviews (Claude Opus and Codex Astra on each of Tag Master design, Pitch Perfect design, a native technical audit and a code review); design review only, the detector does not apply to native iOS. Scope: Tag Master on iPhone Duo (PR #98).

## Design health (Opus / Astra)

| # | Heuristic | Score | Key issue |
|---|---|---|---|
| 1 | Visibility of system status | 3 / 3 | Lit row and stepper state show where you are; status clock ink wrong on the band |
| 2 | Match system / real world | 3 / 4 | Book = set list on a music stand; tabletop ignores the fold |
| 3 | User control and freedom | 3 / 3 | Tag, page and playing part survive every fold change |
| 4 | Consistency and standards | 2 / 3 | Browse sorts icon-only in the side bar; header bands step |
| 5 | Error prevention | 2 / 3 | Next tag beside the list toggles in one capsule; Sheet Music on the hinge in tabletop |
| 6 | Recognition rather than recall | 3 / 3 | Seven unlabelled side-bar glyphs |
| 7 | Flexibility and efficiency | 3 / 3 | Steppers keep the page; Cmd-Up/Down |
| 8 | Aesthetic and minimalist design | 3 / 2 | Busy side bar, uneven bands |
| 9 | Error recovery | n/a / 3 | No new error states |
| 10 | Help and documentation | 3 / 2 | "Pick a tag" explains the empty page |
| | Total | 25/36 / 29/40 | Acceptable, near Good |

Audit (0-20): Opus 16, Astra 14.

## Design specificity verdict

Authored for Tag Master: the book keeps the list and the lit row beside the tag, and the tag, its page and a playing part carry across opening and closing. Specificity runs out at tabletop, which is the phone stack unchanged.

## Priority issues

- [P1] A fold the divider cannot sit on (window beside another app) put the crease through the tag. Fixed: keeps the stack.
- [P2] Full-screen sheet music half open kept a 40 pt inset at the display's left edge. Fixed.
- [P2] Crease clearance doubled (region frame already includes margins). Fixed: 20 pt per side.
- [P2] Seven side-bar items in one capsule, Next tag beside the list toggles. Fixed: three groups.
- [P2] Lit row wash stopped 40 pt short of the divider half open. Fixed.
- [P2] Tabletop puts Sheet Music on the hinge and leaves the lower half empty. Open (design decision).
- [P2] Status clock black on the charcoal band after picking a tag in the book. Open (status cluster straddles the band).

## Persona red flags

- Sam (VoiceOver): focus after a fold change unverified; stepping announces nothing.
- Casey (one hand): Browse's sorts are unlabelled icons in the closed side bar.
- Afterglow teacher: book is strong; tabletop puts Sheet Music on the hinge.

## Minor observations

- Home's large title and the tag's inline title step in the book; Browse row half under the band after a handoff's reveal; watermark behind the tag's title block (about 3.4:1); "here." orphan half open; dark Sheet Music button 1.9:1 (inherited).

## Questions

- Should tabletop be a music stand (page above the fold, parts and key below)?
- Should the membership toggles become one Lists menu?
