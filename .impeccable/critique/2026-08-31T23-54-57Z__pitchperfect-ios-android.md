---
target: Pitch Perfect on iOS and Android
total_score: 24
max_score: 40
na_heuristics: 
p0_count: 1
p1_count: 2
timestamp: 2026-08-31T23-54-57Z
slug: pitchperfect-ios-android
---
# Pitch Perfect — Cross-Platform Design Critique (iOS + Android)

Method: dual-agent (A: critique-assessment-a c33c1a76 · B: critique-assessment-b 31bb0c79)

## Design Health Score

| # | Heuristic | Score | Key Issue |
| --- | ----------- | ------- | ----------- |
| 1 | Visibility of System Status | 3 | Playing state shown on both; fragile deferred highlight on iOS |
| 2 | Match System / Real World | 4 | Ring layout, staff-notation glyphs, community terms |
| 3 | User Control and Freedom | 3 | Login skippable; no undo for song deletion on iOS |
| 4 | Consistency and Standards | 2 | Same product, two vocabularies |
| 5 | Error Prevention | 2 | Blank song titles savable; identical ♯/♭ buttons invite wrong taps |
| 6 | Recognition Rather Than Recall | 2 | All five black keys labeled identically "♯/♭" on both platforms |
| 7 | Flexibility and Efficiency | 3 | Android has widget + Wear; iOS has no fast path |
| 8 | Aesthetic and Minimalist Design | 2 | Ad banner above the instrument on every screen, both platforms |
| 9 | Error Recovery | 1 | iOS delete-account spinner never stops on failure |
| 10 | Help and Documentation | 2 | Good shared login copy; otherwise just a changelog |
| Total | | 24/40 | Acceptable — significant improvements needed |

## Design Specificity Verdict

The core screen is authored; everything around it is category-interchangeable. Both platforms arrange the 12 chromatic notes around the perimeter of a grid — an echo of a physical pitch pipe (DPPitchPipeViewController.m rowMap/colMap on iOS; four weighted rows in pitchpipeview.xml on Android, range selector nested in the hole of the ring). NoteHedz accidental glyphs on both, MusiQwik staff notation on iOS Keys. Notes/Keys/Songs are default table rows; Android settings is a bare stack of stock widgets. The identity lives on one screen out of five.

Deterministic scan: detector ran on both targets, exit 0, but its scannable extensions are web-only (.html/.css/.jsx…): it scanned ZERO files. No deterministic coverage, not verified-clean. Mechanical greps (Assessment B): Android layouts have 1 contentDescription vs 14 ImageButton/ImageView; iOS accessibility labels exist in 1 of 21 source files (DPCommon.swift); 45 hardcoded textSize literals in Android layouts (incl. 12dp in aboutfooter.xml). False positives: 4 layout hex literals are all cacheColorHint="#00000000" (legacy ListView idiom).

Visual overlays: not applicable — native apps, no URL.

## Cross-Platform Coherence

Agree: identical four-tab structure/order, the signature ring, pano background everywhere, middle-octave-centered lists, shared toggle/wake-lock behavior, verbatim-equivalent login copy.

Drifted:

| Dimension | iOS | Android |
| --- | --- | --- |
| Primary tab name | "Pitch Pipe" | "Quick Pitch" |
| Range selector | Segmented "C to B / F to E" | Two loose RadioButtons "From C to B…" |
| Major/minor switch | Segmented control in nav bar | Icon-only FAB (no label) |
| Auth | Email/Google/Facebook/Apple/phone | Email/Google/Facebook |
| Theme | None — no theme code | Default/Light/Dark picker |
| Settings | Minimal modal, 2 toggles | ~12 stacked controls, no sections |
| Song editor field | "Name:" | "Song Title" |
| Accent color | System tint / systemGray2 | Charcoal #474747 + holo blue |
| Fast paths | None | Home widget + Wear app |

Pattern: both agree on the 2012-era skeleton; every feature since landed on one platform or with different words. PRODUCT.md names cross-platform terminology consistency as a hard constraint; it has quietly failed.

## Priority Issues

1. [P0] Screen readers cannot name the instrument. Accidental buttons carry raw glyph text; no accessibility labels on any of the 24 pitch buttons on either platform; Android major/minor FAB announces as "button". Confirmed: 1 contentDescription / 14 image elements; 1 of 21 iOS files. Fix: per-note labels ("C sharp, D flat") at binding; label the FAB. → harden
2. [P1] Terminology has forked. "Pitch Pipe"/"Quick Pitch", "Log in"/"Sign in", "Name:"/"Song Title", divergent toggle copy. Fix: shared glossary (recommend "Pitch Pipe"), sweep both string surfaces. → clarify
3. [P1] Theme support exists only on Android; iOS has zero theme code while PRODUCT.md lists themes as a capability. Fix: overrideUserInterfaceStyle on iOS driven by the same synced setting. → harden
4. [P2] Identical ♯/♭ labels on every black key force spatial recall under pressure. Fix: engrave real names ("C♯/D♭"), keep the glyphs. → clarify
5. [P2] First-run and error moments betray trust: Android login dialog before first note; iOS delete-account spins forever on failure. Fix: defer login until after first pitch; add error handling. → onboard + harden

## Persona Red Flags

Alex (chorus director): login/changelog dialogs before the note (Android); ad container loads late and reflows the ring; five identical "♯/♭" buttons force counting around the ring; iOS has no widget/Wear fast path.

Sam (VoiceOver/TalkBack, low vision): core instrument effectively unlabeled; note buttons #55777777 at 0.8 alpha over photo texture — borderline contrast; center RadioButtons are wrap_content targets. Bright spot: songlistitemview.xml edit button IS labeled.

## Minor Observations

- iOS pattern-image + 0.5 alpha may not render as intended; iOS tiles PNG, Android center-crops vector — same asset, different look.
- keysignatureview.xml hardcodes paddingBottom=90dp to dodge the FAB.
- Center RadioButtons not in a RadioGroup; exclusivity hand-rolled.
- aboutfooter.xml links to dead market.android.com; 12dp text ignores font scaling.
- iOS nav titles internally inconsistent.
- No chosen brand accent; each platform defaults differently.

## Questions to Consider

1. The ring is the one authored signature — why does it stop at the app (widget/Wear/Lock Screen)?
2. When a feature ships to only one platform, who owns the decision? Which app is the real Pitch Perfect?
3. Cost of one mis-tapped ad during a live performance vs the CPM of that impression?
