---
target: "iPhone Duo tabletop: Tag Master music stand and Pitch Perfect tabletop"
total_score: 25
max_score: 36
na_heuristics: 10
p0_count: 0
p1_count: 2
target_identity: "file:/Users/depoll/Code/depollsoft/.claude/worktrees/ios-iphone-duo/iOS/tagmaster/tagmaster/TMMusicStand.swift"
target_fingerprint: "sha256:f0b37cda7e1362447f02e01204826c25945037f6d9ce75be2fe3c1a449aa98e8"
target_path: /Users/depoll/Code/depollsoft/.claude/worktrees/ios-iphone-duo/iOS/tagmaster/tagmaster/TMMusicStand.swift
timestamp: 2026-10-29T23-43-30Z
slug: ios-tagmaster-tagmaster-tmmusicstand-swift
---
Method: design review only (detector not applicable, native platform)

Scope: the iPhone Duo tabletop experiences on the Duo simulator (hinge set with hingectl, inner display upright, 130°): Tag Master's music stand (Summary, Details, Videos, Tracks idle and playing; light, dark, XXXL text) and Pitch Perfect's tabletop (all four tabs and Settings; ring and classic grid, at rest and sounding, light and dark). Evidence: scratchpad pass/before.

## Design health score

| # | Heuristic | Score | Key issue |
|---|-----------|-------|-----------|
| 1 | Visibility of system status | 3 | PP's raised display reports every note; TM's player names the playing part, but the parts deck never marks which row is playing |
| 2 | Match system / real world | 3 | Music stand and bench instrument are the right metaphors; TM's deck held two 44 pt page buttons where a stand needs something to play |
| 3 | User control and freedom | 3 | Standing up or lying down keeps the page, scroll position and a playing part |
| 4 | Consistency and standards | 2 | TM decks capped at 480 / 520 pt and left-aligned under full-width pages; dark Sheet Music white on #5AC8FA (about 1.9:1) where the sounding key uses black |
| 5 | Error prevention | 3 | Nothing drawn or touched in the fold's 40 pt region; 44 pt targets throughout |
| 6 | Recognition rather than recall | 3 | "Tap a part below to play it." bridges the halves; Key caption names the pad |
| 7 | Flexibility and efficiency | 3 | Decks scroll at large text; PP folded cells about 62 pt |
| 8 | Aesthetic and minimalist design | 2 | TM's deck floated mid-half with a ragged right edge; the standing page sliced its last line in half at the fold |
| 9 | Error recovery | 3 | Missing tracks and videos keep their messages; Sheet Music shows its barber pole while opening |
| 10 | Help and documentation | n/a | A posture layout adds no task that needs help |
| **Total** | | **25/36** | **Acceptable (69%)** |

## Design specificity verdict

Pitch Perfect's tabletop is authored for this product: the readout rises into a machined display window above the crease and the ring lies alone under the fingers, One-Glow intact, cells grown to about 62 pt because the banner moved up. Tag Master's stand had the right idea (page faces the singers, controls under the fingers) but its deck was the page's own button stack dropped into a half it was never sized for, so it read as leftovers rather than a control surface.

## Priority issues

- **[P1] Tag Master's deck stops short and floats.** Key and Sheet Music were capped at 480 pt and left-aligned (about 170 pt short of the right margin) and two 44 pt buttons hung in the middle of a 365 pt half. Fix: two pads side by side on the page's readable width, centred on the half, 0.32 of the deck tall up to 120 pt, Title 3 titles, stacked when large text won't fit them. /impeccable layout
- **[P1] The standing page cuts a line in half at the fold.** Summary's third lyric line was sliced by the page's edge. Fix: fade the page's last 24 pt into the fold and give scroll pages 24 pt more room at the end. /impeccable polish
- **[P2] Tracks' parts stop short.** The deck's rows were capped at 520 pt and their rules stopped about 130 pt from the margin. Fix: the same readable column as the page. /impeccable layout
- **[P2] Dark Sheet Music fails contrast.** White on the dark accent (#5AC8FA) reads at about 1.9:1, and as a pad it's the largest thing on the deck. Fix: black title on the dark accent, as the sounding key already uses. /impeccable colorize

## Persona red flags

- **Casey (distracted, device on a table at an afterglow):** reaches for the key with a thumb and finds a 44 pt strip floating mid-half; the near edge of the deck is empty.
- **Sam (low vision, dark mode):** Sheet Music's white-on-pale-blue title is the hardest text on the stand to read.
- **Riley (XXXL text):** the standing page shows only the first lyric line, cut hard at the fold; the deck held up.

## Minor observations

- The parts deck doesn't mark the playing part; the player above names it.
- Pitch Perfect's Notes and Keys open with middle C at the crease (List ignores custom anchors; known).
- Pitch Perfect's Settings sheet and Tag Master's Home scroll across the crease, as continuous content does.
- Pitch Perfect's classic grid's bottom row sits about 7 pt above the floating tab bar, as on iPhone.

## Questions to consider

- Should Sheet Music open on the standing half in tabletop, so the chart faces the group while the deck keeps the key and parts?
- Should the playing part light its row on the deck, as the open tag lights its row in the book?
