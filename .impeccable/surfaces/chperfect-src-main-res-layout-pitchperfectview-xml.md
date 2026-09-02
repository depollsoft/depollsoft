---
version: 1
slug: "chperfect-src-main-res-layout-pitchperfectview-xml"
primary_target: "Android/PitchPerfect/src/main/res/layout/pitchperfectview.xml"
related_targets: ["iOS/pitchperfect/pitchperfect/DPPitchPipeViewController.m"]
---

# Pitch Perfect — app surface (Android + iOS)

Scope: the whole Pitch Perfect app on both platforms; visitor mode Operate.
Audience/job: singers and directors who need a reference pitch immediately, often one-handed in a dark rehearsal hall or on stage; secondary jobs are note/key reference and saved song keys.
Direction: The Laboratory Instrument (seed 1d4ba7fb, bolder-register challenger fused with product truth; user-pinned grayscale). The pipe is a custom-drawn radial instrument face; every other screen is panel-mounted hardware in the same world. The original score artwork (treble/bass clefs, key signatures, staff, and notation) tiles full-bleed at low contrast on every screen. Transparent rows preserve continuity while semantic pressed-state foregrounds protect text legibility.
Memorable moment: pressing a glass cell answers with one crisp detent, ignites the panel's only light, and puts the live note and frequency in the hole; sustained notes breathe. Two-note chords identify their musical interval, and true multi-touch lets each finger release independently.
Constraints: grayscale only (one luminous value); ads stay in a fixed full-width bottom slot; inclusive C–C/F–F physical-pipe ranges preserve the existing saved boolean; ♯/♭ glyph cells (physical-pipe convention, user-pinned); NoteHedz/MusiQwik glyph faces preserved; Android song pencils/drag handles appear only in explicit Edit mode; iOS keeps native Liquid Glass over the full-bleed score, with stable template icons; its song-editor keyboard must always be dismissible; widgets on both platforms mirror the circular 13-cell instrument; Wear remains untouched.
Unresolved: iOS theme setting is device-local (not yet synced via Firestore).
