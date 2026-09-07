---
version: 1
slug: "id-tagmaster-src-main-res-layout-tagmasterview-xml"
primary_target: "Android/TagMaster/src/main/res/layout/tagmasterview.xml"
related_targets: ["Android/TagMaster/src/main/res/layout/meview.xml", "Android/TagMaster/src/main/java/depollsoft/tagmaster/MeActivity.kt", "iOS/tagmaster/tagmaster/DPHomeViewController.m", "iOS/tagmaster/tagmaster/TMRootController.swift"]
---

## Scope and mode

Tag Master on iOS and Android, all screens. Operate mode. Pitch Perfect is out of scope.

## Audience and task

Singers choosing and singing tags together, including afterglows. Preparation and discovery remain supported. A singer must reach search, a random tag, a tag ID, favorites or teachable material without signing in.

## Chosen structure

The singing desk established task priority, seed a858fb14, candidate 6. The user rejected the first refresh's faded footer artwork and layered tabs. The approved correction restores the original page-scale barber-pole background and familiar Home -> Browse/Search -> Tag -> material pushes. No global tabs, sidebar or navigation bar whose meaning changes on detail. Tablets can show Summary beside selected material; narrow windows and accessibility text use one column. Preserve charcoal/blue, the handwriting wordmark and native body text.

Next refinement: increase useful information density and keep Favorites and Teachable Tags as separate list experiences. Future user-defined labels and additional named lists are planned but explicitly not implemented in this work; do not change the storage model or add a label-management interface.

## States and continuity

Consider loading, no results, no saved tags, missing media, failed requests, signed out, large text, dark appearance, multitasking and phone/tablet rotation. Preserve storage, optional sync, deep links, playback, chart sharing, videos, ratings and BarbershopTags.com attribution.

## Memorable moment

Meaningful saved-state or selection feedback helps a singer collect the next tag without interrupting the group. No decorative delays or automatic audio.

## Verification

Code-led native implementation. No approved raster comp. Native iPhone/iPad and Android phone captures were reviewed; Android tablet evidence uses an explicit 800dp runtime simulation. Independent verdicts scored the listed material fixes resolved. Hardware audio, full assistive-technology traversal and signed-in account/sync flows remain unverified.

Built design rules: `Android/TagMaster/DESIGN.md` and `iOS/tagmaster/DESIGN.md`. The root `DESIGN.md` belongs to Pitch Perfect and was not changed. The acceptance ledger records test scope and follow-up checks.
