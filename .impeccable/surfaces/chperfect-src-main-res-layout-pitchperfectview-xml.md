---
version: 1
slug: "chperfect-src-main-res-layout-pitchperfectview-xml"
primary_target: "Android/PitchPerfect/src/main/res/layout/pitchperfectview.xml"
related_targets: ["iOS/pitchperfect/pitchperfect/DPPitchPipeViewController.m"]
---

# Pitch Perfect — app surface (Android + iOS)

Scope: the whole Pitch Perfect app on both platforms; visitor mode Operate.
Audience/job: singers and directors who need a reference pitch immediately, often one-handed in a dark rehearsal hall or on stage; secondary jobs are note/key reference and saved song keys.
Direction: The Laboratory Instrument (seed 1d4ba7fb, bolder-register challenger fused with product truth; user-pinned grayscale). The pipe is a custom-drawn radial instrument face; every other screen is panel-mounted hardware in the same world. The original score artwork (treble/bass clefs, key signatures, staff, and notation) tiles full-bleed at low contrast on every screen. Translucent plate rows preserve continuity while protecting text legibility.
Memorable moment: pressing a glass cell ignites the panel's only light and the hole reads the live note name and frequency; sustained notes breathe. True multi-touch lets singers hold chords, with each finger releasing independently.
Constraints: grayscale only (one luminous value); ads stay in a fixed full-width bottom slot; ♯/♭ glyph cells (physical-pipe convention, user-pinned); NoteHedz/MusiQwik glyph faces preserved; Android song pencils/drag handles appear only in explicit Edit mode; iOS keeps native Liquid Glass over the full-bleed score, with stable template icons; its song-editor keyboard must always be dismissible; widget and Wear untouched.
Unresolved: iOS theme setting is device-local (not yet synced via Firestore); tablet/iPad layout pass; iOS Notes/Keys/Songs/Settings screens inherit the world but have not had a dedicated redesign pass.
