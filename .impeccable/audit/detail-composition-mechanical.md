# Detail composition: mechanical assessment

Read-only assessment on `tagmaster/impeccable-ux-polish`. No other assessment read, context rerun, builds, devices, tests, or commits. Only this report written in the repository. No visual direction selected.

## Evidence and coverage

Ran `impeccable detect --json --scope layout` **once**, targeting exactly Android `layout/tagsummaryview.xml`, `layout/tagmiscview.xml`, `layout-land/tagsummaryview.xml`, and iOS `DPTagSummaryController.m` / `DPTagDetailController.m`. Result `[]`; raw JSON: `/tmp/detail-composition-mechanical-detector.json`. Native XML/Objective-C rule coverage is **unproven**. This is not layout clearance.

Inspected latest located native phone captures under `.impeccable/review/`, each JPEG at most 800px:

- `tagmaster-ios-native-glass-phone-detail-light-large.jpg`: key begins at the value column but sheet begins at the page edge; their right edges differ.
- `tagmaster-ios-native-glass-phone-detail-details-light-large.jpg`: shared value edge in this fixture, except button text inset; link/name baselines sit below their captions. This is non-AX5 evidence, not proof of every adaptive state.
- `tagmaster-layout-before-android-summary-dark.jpg`: compact key, centered sheet action, rating action at the trailing edge.
- `tagmaster-layout-before-android-details-dark.jpg`: common value column, mixed text/link row heights. These are the newest located Android Summary/Details captures despite the filename's “before”; current source confirms the structural mechanisms, not full capture/build equivalence.

Current navigation verified: `TMPageViewController.m:8–12,30–33` owns real `UITabBarController`; Android `res/layout/tagdetailview.xml:131–135` uses fixed/fill tabs. System capsule margins are not a content-alignment defect.

## Source findings

Paths below use `A = Android/TagMaster/src/main`, `I = iOS/tagmaster/tagmaster`.

1. **Android columns and row sizing.** `A/res/layout/tagsummaryview.xml:16–23,123–218,276–318` uses one shared TableLayout caption column, a 5dp caption gap, and a stretched/shrinkable value column. Caption widths mix `match_parent` and `wrap_content`; value cells mix weights, fill, and wrap. Rating consumes a 48dp action row; key is 48dp; plain rows are content-height. Rating stars/number sit in a weighted child, separate from the reserved loader and trailing rate button. Centering is explicit, so baseline equality is not guaranteed. `layout-land/tagsummaryview.xml:138–200` moves centering to the parent but retains this width pressure.

2. **Android action-wrapper geometry survives missing content.** Portrait `tagsummaryview.xml:344–379` centers the sheet **button plus loader**, not the button. Loader footprint is about 18.76×32dp with 8dp start margin; idle is `INVISIBLE`, not `GONE` (`barberpole_dimensions.xml:3–4`, `BarberPoleLoadingView.kt:52–62`). `TagSummaryFragment.kt:147–152` hides only `sheetMusicLink`: its wrapper and 16dp top/bottom margins remain, reserving roughly 64dp even without sheet music. `savedStatusLayout` likewise keeps its 4dp top margin when both markers disappear (`tagsummaryview.xml:95–118`; fragment `154–165`).

3. **Android prose and landscape differ structurally.** Portrait lyrics/notes remain table value cells with no explicit inter-row gap (`tagsummaryview.xml:380–434`). Landscape uses equal weighted metadata/prose columns, outer 16dp margins and a 16dp combined gutter (`layout-land/tagsummaryview.xml:18–32,338–390`). Notes retain a 16dp top margin even when lyrics are absent; the right half remains allocated when both are absent. Sheet action is start-aligned in landscape, centered in portrait.

4. **Android provenance mixes row-height rules.** `A/res/layout/tagmiscview.xml:16–52,79–253`: shared caption column plus 5dp gap, but links/name views have minimum 48dp and centered content; dates/counts have natural height. Captions center vertically, while some multiline values fill their cells. Title-to-metadata gap is 16dp versus Summary's 5dp tag-ID margin. This explains ordinary-phone spacing differences without invoking font scaling.

5. **iOS Summary uses three distinct action widths.** `I/DPTagSummaryController.m:338–411` fixes the caption gap at 8pt and pins the key to the entire value column, sheet wrapper across all columns, and rating inside the value column. Shared `iOS/pitchperfect/pitchperfectlib/pitchperfectlib/DPPitchPipeButton.m:94–104` adds 2pt key padding. The sheet button ends before its reserved loader slot plus 8pt (`DPTagSummaryController.m:454–471`). Rating adds 4pt vertical padding; equal button heights do not equalize wrapper heights. Prose has a conditional 16pt break and 8pt lyrics-bottom padding (`:248–258,377–389`). Unlike Android, missing sheet hides the complete action wrapper.

6. **iOS provenance can produce multiple value axes.** `I/DPTagDetailController.m:18–38,244–256` calculates each row independently: use widest visible caption if `shared + 8 + usableValue <= width`, otherwise own caption; stack only if that also fails. Algebra probe at width 300, shared caption 160: own caption 50/value budget 44 starts values at 168; the same caption/value budget 144 starts at 58; caption 160/value budget 144 stacks. These are synthetic inputs, not measured glyph widths. Mixed horizontal axes and mixed 4/16pt row separation are therefore possible. Ordinary capture does not demonstrate that failure. Separately, top-aligned captions beside minimum-44pt buttons and 2pt button text insets explain its visible offsets (`:34,185–200`).

7. **Page margins already have shared owners.** `I/DPTagPageControllerBase.m:251–291` supplies readable width, 16pt side margins, 4pt top/bottom. Android Summary/Misc use `applyContentInsets` (`TagSummaryFragment.kt:39`, `TagMiscFragment.kt:22`, `EdgeToEdge.kt:88–120`); ordinary phone caps both resolve to 640dp, landscape Summary to 960dp versus Details 640dp. Do not compensate for native tab-bar margins or accidentally change Tracks/Videos through these helpers.

## Binding/test contracts to retain

- Android `TagSummaryFragment.kt:38–171`: title/AKA/version, tag ID, rating value/stars, parts/type, `playKeyNoteButton` Note/Activated/Text, optional key/classic/lyrics/notes rows, sheet enabled/loading, rating loading, saved markers, click handlers. `TagMiscFragment.kt:21–104`: title, refresh/download, link and provenance values/URIs, optional row visibility. Static probe: all referenced IDs exist in both Summary variants and Misc; all three XML files parse.
- Android tests under `src/androidTest/java/depollsoft/tagmaster`: `TagDetailActivityTest.kt:40–45,71–103` requires key, provenance/classic IDs, scrolling, page order and recreation; `CompactLoadingRegressionTest.kt:266–293` requires loader IDs and pending/terminal behavior; `FooterPitchRegressionTest.kt:129–166` requires note-bound appearance, release/cancel/disabled and accessibility playback.
- iOS `DPTagSummaryController.m:234–240,292,331,396,532`: preserve `summary.key`, `sheet.key`, “Play key note…”, “Rate tag”, “Rating out of 5”, Sheet Music action and accessibility activation. Details `:116–155,220` preserves URLs, link-versus-static-text traits and caption/value accessibility order. `tagmasterUITests/TagDetailUITests.swift:346–367,465–482,591–602,625–628` covers native `page-*` tabs, Details Tag ID, sheet key, pitch hit height and rating sheet. These IDs are current native-tab identifiers, not evidence for restoring custom tabs.

## Mechanical acceptance ledger for implementation

- One declared caption/value-axis rule per metadata group, including transitions, localization and hidden rows; no row-dependent horizontal shifts unless explicitly selected by main.
- Compare ordinary phone Summary/Details first, then narrow/landscape and large text/AX5. Assert measured text/action bounds and no clipping; screenshots alone cannot prove touch bounds.
- Preserve at least 48dp Android / 44pt iOS action targets. Compare idle/pending/terminal button frames; loaders must not shift buttons or overlap rating values.
- Exercise sheet absent, lyrics-only, notes-only, neither prose field, all optional provenance absent, and both saved markers absent. No orphan wrapper gaps or unexplained empty landscape column.
- Retain all mapped IDs, visibility conditions, links, models, accessibility order and pitch behavior. Preserve colors, brand fonts, vector art, matched motion, compact footer, opaque sheet-key face and blue playback feedback.
- Keep native navigation and shared-page inset behavior unchanged. Runtime layout/test coverage remains pending; no builds/devices/tests were run here.
