# Tag detail composition assessment

Visual/spatial pass only, on `tagmaster/impeccable-ux-polish`. No mechanical scan, parallel report, production/test edits, builds, device operations or commits. Context was not rerun.

## Evidence and limits

Viewed individually through `pi.read`, all JPEGs with maximum dimension 800, under `.impeccable/review/`:

- `tagmaster-ios-native-glass-phone-detail-light-large.jpg`
- `tagmaster-ios-native-glass-phone-detail-details-light-large.jpg`
- `tagmaster-ios-native-glass-phone-detail-details-dark-ax5.jpg`
- The corresponding three `ipad` captures.
- `tagmaster-layout-before-android-summary-dark.jpg`, ordinary phone, font scale 1.0.

These iOS captures show the real native tab bar, not the rejected custom tabs. Its system capsule margins are not a page-alignment defect. Android retains four full-width bottom destinations. The Android capture is existing artifact evidence: the freshness section of `layout-assessment.md:37–46` records the artifact and real catalog content, not a clean build from current HEAD. Its visible Summary structure agrees with current XML. Android Details and landscape conclusions below are source-based, not newly observed renders. No older round3 images were needed.

## What looks wrong

The title and blue Sheet Music action pass the squint test. The trouble lies between them and below them. More isolated gaps would preserve the underlying mismatch.

- **Reading order and grouping.** Ordinary Android Summary separates the rating stars/value from a rate icon at the far right. Its Sheet button floats centrally below a left-positioned Key. Current `Android/TagMaster/src/main/res/layout/tagsummaryview.xml:146–216,278–377` explains that composition. iOS keeps Rate nearer its value, so it is less orphaned, but Key begins at the value column while Sheet begins at the page edge. Both ordinary iPhone and iPad show their right edges disagreeing. `iOS/tagmaster/tagmaster/DPTagSummaryController.m:367–408,454–471` places them in different spans and reserves a trailing loader slot outside Sheet's visible face.
- **Title grouping.** Android's visible alternate title is indented; version and saved status share that inset in source, `tagsummaryview.xml:40–119`. iOS's alternate title already aligns with the title. Do not describe iOS as having the Android indentation problem.
- **Structure and density.** Both phone Summaries force Lyrics and Notes into the metadata value column. The text remains readable in these examples, but unnecessarily inherits a form layout. See Android `tagsummaryview.xml:379–435` and iOS `DPTagSummaryController.m:379–390`. The empty lower page is not itself a defect; stretching rows to fill it would worsen scanning.
- **Rhythm.** Ordinary iOS Details has aligned value starts, but Link, Arranged By and Sung By text sit lower than their captions. Short numeric rows are tight; touch-height links create apparently arbitrary larger intervals. `DPTagDetailController.m:185–226` combines minimum-height buttons with top-aligned pairs. Android `tagmiscview.xml` uses center-aligned captions and links, so that specific visual defect is not established there. Both pages currently read as one undifferentiated metadata run.
- **Adaptation and extremes.** iPhone AX5 mixes inline Tag ID with stacked later fields. iPad AX5 also changes value starts between rows. This matches the per-pair decisions in `DPTagDetailController.m:18–38`, not one consistent group fallback. Android landscape already gives prose its own vertical blocks, but uses equal side-by-side columns based on orientation, `layout-land/tagsummaryview.xml`; that alone does not establish adequate room at enlarged text. Long localized labels, empty combinations, pending actions and final-row clearance were not visually established in this pass. Do not infer clipping from a still capture ending mid-scroll.

## Recommended bounded anatomy

Use one reading path across platforms: identity, rating and short facts, performance actions, then prose. Details keeps identity followed by source information and contributor/date pairs.

1. Align title, alternate title, version and existing status content to one page-leading edge. Keep supporting type quieter and close to the title.
2. Keep the rating value, indicator and Rate action together as a compact unit. Give short metadata a shared caption/value alignment policy across Summary and Details, rather than a fresh width for each row. Align caption text with the value's first visible line, including links, without shrinking touch regions.
3. Pull Key out of the short-fact form into the Sheet/Key action group. Give both visible control faces the same leading and trailing edges within that group. Sheet remains primary and Key secondary. Put the existing Key caption above its control if needed. Reserve loading space within the action's stable footprint so idle and busy states do not shift its face or detach its label.
4. Make Lyrics and Notes full-width reading blocks with their existing labels above the text. Separate source facts from contributor/date pairs through proximity, not new cards or headings. Keep related rows tight, with larger intervals only between meaningful groups.
5. On narrow panes, enlarged text or long labels, switch the entire metadata group to label-above-value. Reflow the rating/action group as a unit. Use two columns only when both complete groups retain useful reading width, not merely because the device is landscape or an iPad.

Keep existing readable page insets. Do not align page content to the iOS tab capsule. Preserve every existing field, action, link and omission rule; colors, brand fonts, vector artwork, matched motion, compact footer, opaque sheet Key face and blue pitch feedback remain unchanged. Tracks/Videos need no expansion of this pass.

Planning checks covered ordinary phone composition, iPad pane behavior, AX5 grouping, both platform sources and landscape structure. The main pass should choose the final shared anatomy before any editing; this report is not implementation verification.
