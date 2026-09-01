---
name: Pitch Perfect
description: A grayscale precision laboratory instrument for pitch — one luminous glow where sound exists.
colors:
  plate-ground: "#DADBDC"
  plate-surface: "#E7E8E9"
  plate-ink: "#1C1E20"
  plate-ink-bar: "#141618"
  plate-ink-secondary: "#55585C"
  plate-hairline: "#B7B9BC"
  plate-lit: "#141618"
  plate-on-lit: "#F2F3F4"
  plate-ground-dark: "#0E0F10"
  plate-surface-dark: "#16181A"
  plate-ink-dark: "#D9DBDD"
  plate-ink-bar-dark: "#0A0B0C"
  plate-ink-secondary-dark: "#898D92"
  plate-hairline-dark: "#2C2F33"
  plate-lit-dark: "#F2EFE6"
  plate-on-lit-dark: "#101214"
typography:
  display:
    fontFamily: "Oswald Medium, sans-serif-condensed"
    fontWeight: 500
    letterSpacing: "0.16em"
  body:
    fontFamily: "sans-serif-condensed (Android) / system condensed (iOS)"
    fontSize: "20-24sp"
  mono:
    fontFamily: "monospace (Android) / SF Mono system monospaced (iOS)"
    fontSize: "14-16sp"
    letterSpacing: "0.04em"
  label:
    fontFamily: "monospace"
    fontSize: "12sp"
    letterSpacing: "0.14em"
rounded:
  plate: "2dp"
  machined: "5px"
components:
  list-row:
    backgroundColor: "{colors.plate-ground}"
    textColor: "{colors.plate-ink}"
  list-row-lit:
    backgroundColor: "{colors.plate-lit}"
    textColor: "{colors.plate-on-lit}"
  fab:
    backgroundColor: "{colors.plate-surface}"
    textColor: "{colors.plate-ink}"
  section-header:
    textColor: "{colors.plate-ink-secondary}"
    typography: "{typography.label}"
---

# Design System: Pitch Perfect

<!-- Scope: the Pitch Perfect app only (Android/PitchPerfect + iOS/pitchperfect). Tag Master is a different world. -->
<!-- Direction contract: "The Laboratory Instrument", seed 1d4ba7fb, recorded atop Android/PitchPerfect/src/main/res/layout/pitchperfectview.xml. Recorded from the built code (ground truth), not the plan. -->

## Overview

**Creative North Star: "The Laboratory Instrument"**

Pitch Perfect is drawn as a piece of precision lab hardware, not a grid of platform buttons. The light variant is a bench-aluminum plate; the dark variant is blackened steel. Both are grayscale to the bone — PRODUCT.md pins this as a brand commitment ("neutral monochrome surfaces carry the interface, with at most a single luminous emphasis for the sounding note"). Every surface carries machined texture: brushed grain, an etched five-line staff engraved into the panel, hairline anode rings inside each glass cell.

The one moment of light is functional, not decorative: when a note sounds, its cell ignites white-hot with a radial bloom, the note name and live frequency digits appear in the ring's hole, and the glow breathes on a slow 4-second cycle. Release, and the panel goes dark again.

**Key Characteristics:**

- Grayscale only; zero chromatic color anywhere in first-party UI
- One luminous element maximum, and only while sound exists
- Engraved condensed caps (Oswald Medium, wide tracking) for labels; monospace for every measurement
- Hairline strokes (1–1.5px) as the primary structural device; near-square 2dp corners
- Identical tokens and instrument geometry on both platforms; chrome stays platform-native

## Colors

Nine grayscale roles, each with a light (bench-aluminum) and dark (blackened-steel) value; the frontmatter is normative. Both Android (`plate_*` in `values/colors.xml` + `values-night/colors.xml`) and iOS (`DPTheme.swift` dynamic colors) resolve the same hex pairs.

### Primary

- **Plate Lit** (`plate-lit`, light #141618 / dark #F2EFE6): the sole "accent." In dark mode it is the warm white-hot glow of the sounding cell; in light mode it is near-black ink fill. Paired with **Plate On-Lit** (#F2F3F4 / #101214) for content on a lit surface. Also the pressed state of list rows and the range selector's indicator dot.

### Neutral

- **Plate Ground** (#DADBDC / #0E0F10): window background and the instrument panel itself.
- **Plate Surface** (#E7E8E9 / #16181A): resting glass cells, the range-selector frame, FABs, Material `colorSurface`.
- **Plate Row** (82% Plate Ground): translucent list/table-cell fill. It protects text contrast while keeping the full-bleed score visible behind every row.
- **Plate Ink** (#1C1E20 / #D9DBDD): primary text, natural-note engravings, icons.
- **Plate Ink Secondary** (#55585C / #898D92): accidental engravings, frequency readouts, section headers, nameplate captions, empty states.
- **Plate Ink Bar** (#141618 / #0A0B0C): action bar, bottom nav, system bars — the darkest band framing the panel on both themes.
- **Plate Hairline** (#B7B9BC / #2C2F33): every structural stroke — grain lines, staff etching, cell rims, list dividers, the range selector's frame and split line.

### Named Rules

**The One-Glow Rule.** The sounding note is the only luminous element on screen. Nothing else blooms, tints, or lights up; all other state is expressed with ink, hairlines, and surface shifts. Silence means a dark (or matte) panel.

**The Grayscale Rule.** No chromatic color, ever, in first-party UI. The seed contract and PRODUCT.md both pin it. Third-party ad creative inside the ad slot is the only place color may appear, and it is quarantined there.

## Typography

**Display Font:** Oswald Medium (bundled: `res/font/oswald_medium.ttf` on Android, `Oswald-Medium.ttf` on iOS; fallback sans-serif-condensed / system condensed)
**Body Font:** platform condensed sans (`sans-serif-condensed` on Android; system faces on iOS)
**Mono Font:** platform monospace (`Typeface.MONOSPACE` / `UIFont.monospacedSystemFont`)
**Glyph Fonts:** NoteHedz (`NoteHedz170.ttf`) for note-head glyphs; MusiQwik / MusiQwikB for key-signature staff glyphs

**Character:** Engraved instrument lettering. Display text is condensed caps with wide tracking, as if milled into the plate; every number that measures something is monospaced.

### Hierarchy

- **Display / engraving** (Oswald Medium): action-bar title (19sp, 0.16 letter-spacing, from `TextAppearance.Plate.ActionBarTitle`); cell engravings and center note readout (sized relative to ring geometry); nameplate caption ("CHROMATIC PITCH REFERENCE", 0.34 tracking, uppercase); range-selector labels (uppercase, 0.16 tracking); empty-state text (15sp, 0.12 tracking).
- **Body** (condensed sans, 20–24sp): list-row primary text — note names, song titles, key signatures.
- **Mono / data** (monospace, 14–16sp, 0.04–0.06 tracking): frequencies ("261.6 Hz"), song keys, anything measured.
- **Label** (monospace, 12sp, 0.14 tracking, secondary ink): section headers (`TextAppearance.Plate.SectionHeader`).
- **Musical glyphs**: NoteHedz for note heads/accidentals in lists and buttons; MusiQwik for key-signature notation. These are content glyphs, not UI icons.

### Named Rules

**The Mono-Measurement Rule.** Any value with a unit or a key — Hz, octaves, song keys — is set in monospace at secondary weight. Names get the condensed face; numbers get the mono face.

## Layout

The pitch-pipe screen is a single custom-drawn view (`PitchInstrumentView.kt` / `DPPitchInstrumentView.swift`) with identical geometry on both platforms: face center at 44% of height; ring radius = min(w, h × 0.82) × 0.365; 12 cells on a true circle starting at −90° in 30° steps; cell radius = ring radius × 0.245; the range selector seated in the ring's hole (width = ring × 0.72, row height = ring × 0.145, top at center + ring × 0.20). The nameplate caption sits at the panel's bottom edge.

Screen chrome stacks vertically: action bar (ink-bar) → instrument/content panel → "Tired of Ads?" line → fixed bottom ad slot → bottom navigation (ink-bar). Lists use 56–64dp rows with 16–20dp horizontal margins, 1px hairline dividers, and 90dp bottom padding so content clears the FAB.

## Elevation & Depth

No shadows. Depth is conveyed by material: the brushed-metal grain (1px hairlines every 4px at 3–8% alpha), the original score artwork tiled full-bleed at low contrast (treble/bass clefs, key signatures, staff, and notation), the anode ring inset at 86% of each cell's radius, and the one radial bloom (radius = cell × 2.4, lit color fading 60% → 0%) that leaks light across the panel under a sounding cell. Surfaces separate by tone (ground vs. surface vs. ink-bar), never by drop shadow.

**The Etched-Not-Cast Rule.** Depth reads as engraving into one solid plate — hairlines, grain, and inset rings — never as layers floating above it. The only light source is the sounding note.

## Shapes

Machined-part geometry. Circles for cells and indicator dots; near-square 2dp corners on Material components (`ShapeAppearance.Plate`); the range selector is one round-rect frame (5px radius, 1.5px hairline stroke) split by an interior 1px hairline — one machined part with two positions, not two buttons. Strokes are hairline-first: 1px structural, 1.5px frames, 3px only on the lit cell's rim.

## Components

### Instrument Cell (signature)

- **Resting:** surface-filled circle, 1.5px hairline rim, 1px anode ring at 86% radius, engraved label (naturals: full-size letter in ink; accidentals: smaller "♯/♭" glyph in secondary ink).
- **Sounding:** fills with `plate-lit`, 3px lit rim, bloom underneath, label flips to `plate-on-lit`; opacity breathes with the 4s cycle.
- **Behavior:** press-and-hold to sound; true multi-touch lets each finger own a cell independently so chords remain sounding as other fingers release. Sliding one finger retunes only that pointer; toggle mode latches. Only rendered state changes — no ripple, no platform ink.

### Range Selector (machined)

- One 5px round-rect frame in the ring's hole containing two rows (C TO B / F TO E, uppercase Oswald, 0.16 tracking). Selected row gets a 10%-ink wash, a small `plate-lit` dot at its left, and full-ink text; unselected text is secondary at 75% alpha. Switching range stops all sound.

### Center Readout

- One note sounding: note name + octave in display face, live frequency in mono ("%.1f Hz"). Chord: all sounding note names share the display and the mono line reads "<n> NOTES". Idle: a dimmed mono "— Hz". Nothing else occupies the hole besides the range selector.

### List Rows

- 56–64dp rows on 82%-opaque `plate-row`, over explicitly transparent ListView/RecyclerView/UITableView surfaces, with 1px hairline dividers. Primary text condensed 20–24sp ink; trailing datum mono 14–16sp secondary. Pressed state = the lit treatment (`row_lit.xml`: background flips to `plate-lit`, text to `plate-on-lit` via `pitch_button_text` / `pitch_row_secondary` selectors) — a row lights the way a cell does.

### Song Editing

- Android normal mode keeps rows clean. A persistent one-tap **Edit Songs** control switches to **Stop Editing**, reveals each row's pencil and drag handle, and exposes Sort. Dragging uses RecyclerView/ItemTouchHelper and persists once on drop.
- Add/Edit Song keeps the one-tap app-bar checkmark and also supplies a full-width **Save Song** button. Empty titles are blocked inline.
- iOS uses its native Edit/Done table mode. In the song editor, Return is Done, a keyboard accessory Done button is always present, and tapping outside dismisses the keyboard so the key picker is never trapped.

### FAB

- Steel, not accent: `plate-surface` background with `plate-ink` icon, 16dp margin, bottom-end.

### Section Headers

- Engraved labels: monospace 12sp, 0.14 tracking, secondary ink (`TextAppearance.Plate.SectionHeader`).

### Navigation

- Platform-native chrome in plate colors. Android: Material `BottomNavigationView` on the primary-surface (ink-bar) with selector tints (#F2F3F4 checked / #9AA0A6 unchecked). iOS uses opaque grayscale HIG navigation/tab bars with stable template icons and opts out of iOS 26 Liquid Glass because its morphing/glow conflicts with the instrument world. Action bars carry the Oswald title. System bars match `plate-ink-bar`.

### Ad Slot

- A fixed full-width container above the bottom nav, preceded by the small italic "Tired of Ads?" link. On iOS, the banner loads only after the actual view width is known so no side gaps or background seams appear. Third-party creative lives only here.

### Motion

- One motion: the 4-second sinusoidal breath (phase 0→2π, linear) modulating bloom alpha (0.82 + 0.18·sin) and lit-cell fill (0.9 + 0.1·sin) while any note sounds. Android drops it when animator duration scale is 0; iOS honors Reduce Motion. No other animation beyond platform defaults.

### Accessibility

- The instrument exposes virtualized elements: Android `ExploreByTouchHelper` (12 cells + 2 range rows as virtual Buttons with bounds), iOS `UIAccessibilityElement` containers. Spoken names carry full note names ("C sharp, D flat, octave 4") even where the engraving shows only "♯/♭"; range rows report selection; a screen-reader tap sounds the note for 1.5s.

## Do's and Don'ts

### Do

- **Do** keep every first-party pixel grayscale, drawing from the eight `plate-*` role pairs.
- **Do** reserve `plate-lit` for the sounding note, its bloom, the pressed row, and the range dot — light means "this is live."
- **Do** set measured values (Hz, keys, octaves) in monospace and labels in condensed caps with wide tracking.
- **Do** build structure from 1–1.5px hairlines and tone shifts; keep corners at 2dp (5px for machined frames).
- **Do** mirror instrument tokens and geometry exactly across platforms while keeping nav/tab/bar chrome native.
- **Do** tile the original score artwork edge-to-edge as a low-contrast background on every screen. Use translucent plate rows so the score remains continuous without competing with text.
- **Do** honor reduce-motion settings by stopping the breath entirely, and give every custom-drawn control a virtualized accessibility element with a full spoken note name.

### Don't

- **Don't** introduce chromatic color, saturated themes, or a second glow — one luminous element, only while sound exists.
- **Don't** use drop shadows or elevation overlays; depth is etched (grain, staff, anode rings), never cast.
- **Don't** replace the engraved "♯/♭" cell glyphs with full note names on the face; full names belong to the center readout and accessibility tree.
- **Don't** style the FAB or any control with an accent fill; steel surface + ink icon is the ceiling.
- **Don't** let anything but third-party ad creative occupy the ad slot's color exemption, and don't move the slot from its fixed position above the nav.
