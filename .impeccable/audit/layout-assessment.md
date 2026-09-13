# Tag Master layout assessment

Completed independent visual/layout assessment. Baseline `5c2c8448`, branch `tagmaster/impeccable-ux-polish`. No mechanical findings read. Assessment only: no production, test or configuration edits; no build, test suite, commit or push. The pre-existing formatting-only change in `FooterPitchRegressionTest.kt` remains untouched.

## Recommendation

Keep the current visual world and most layouts. Prioritize two bounded corrections:

1. **P1: iPhone Details at accessibility text sizes collapses metadata values into a nearly one-character column.** Change the local metadata topology when the columns no longer fit.
2. **P2: Android empty Tracks puts a disabled player, empty timeline and balance control before the explanation that no playable tracks exist.** Make the confirmed-empty state lead without removing any controls from playable, pending or failed-media states.

An optional P3 improvement would separate iOS Summary's materials from Lyrics/Notes. This is a small spacing change, not a reason to rework the screen. No other rendered defect warrants a new layout bundle from this assessment.

## Spatial thesis

Tag Master is a dense operating tool for live group singing. The path is title → identify the arrangement → sheet music, pitch or voice parts. Keep captions beside their values while there is room. Give unrelated sections more separation than related metadata. The sheet action leads; pitch remains immediately available; rating and provenance support the singing task.

Suggested semantic spacing roles, not a universal replacement constant:

| Role | Starting value | Application |
| --- | --- | --- |
| Screen/content edge | 16dp or pt | Existing readable content edges, plus native safe areas |
| Caption-to-value / related text | 4–8 | Metadata, track notes, small grouped labels |
| Related controls | About 8 | Where target sizes and native control insets permit |
| Section separation | 16–24 | Metadata to materials; materials to longer prose; Settings sections |
| Touch area | Native minimums | At least 48dp Android / 44pt iOS without making every caption a tall row |
| Footer | Preserve measured layout | About 128dp Android / 112pt iOS default; allow necessary accessibility wrapping |

Use a 4-unit base for future local values. Do not normalize existing native pixels, replace every 5dp gap mechanically, or enlarge the restored footer. Compact width may stack an individual metadata pair; expanded width should retain readable columns and the current native split layout. Four bottom destinations remain equal and span their available content width.

## Evidence, freshness and device safety

Read `AGENTS.md`, `PRODUCT.md`, the layout playbook and native Android/iOS references. `DESIGN.md` was not imposed on Tag Master. Binding product choices take precedence over generic platform advice about navigation rails, large titles or palettes.

Recent reports read: `.impeccable/audit/loading-parity-and-glass-summary.md`, `sheet-key-surface.md`, `footer-pitch-barberpole-summary.md`, plus relevant footer preservation notes. Recent targeted captures were inspected against current component source. Historical round1/round2 and unrelated after-captures were not used as current truth.

Fresh native evidence used existing built artifacts, not a new build loop:

- Android: `Android/TagMaster/build/outputs/apk/debug/TagMaster-debug.apk`, modified 2026-09-13 12:04:45, after the sheet-key resource at 12:04:38. Its SHA-256 is `9ef891c678456103aa35e9ec5fb21fdb37fb2488443705fc4eefdb5b192c01cd`. The originally installed APK differed, so it was backed up before installing this artifact.
- iOS: `/tmp/tm-dd/Build/Products/Debug-iphonesimulator/tagmaster.app`, executable modified 11:25:45. Executable SHA-256 `8fdfbd133962c65284e1a82469b88ad038527c5ad3a39ff709bf3ec14cb7ef4d`. Both original simulator apps differed and were backed up before installation. Current Summary, Home and glass-tab source agrees with the rendered components. This is source-correlated artifact evidence, not a claim of a new clean build from HEAD.
- Real catalog content was opened read-only: tag 7611, "Nothing About You". No ratings, share, sign-in, favorites or teachable changes were submitted. Search was focused but no new query or filters were entered.
- Native captures are JPEGs with maximum dimension 800px. Each inspected image was loaded independently through `pi.read`.

| Device | Recorded original state | Observed variants |
| --- | --- | --- |
| Android `emulator-5554` | 1080×2400, 420dpi, font 1.0, night yes, auto-rotate 1, user rotation 0 | Portrait dark; Details at font 2.0; landscape Browse/Search; portrait search IME |
| iPhone `C8B74E44-94F7-4CED-A47F-DFF98E34237B` | iPhone 17 Pro, iOS 26.5, dark, content size `large` | Fresh Home and Summary; recent hosted light/dark/AX5 component evidence |
| iPad `55A9555A-9714-4FE0-8F0D-1035652526F9` | iPad Pro 11-inch M5, iOS 26.5, light, content size `large` | Fresh split Home and Summary; recent expanded AX5 component evidence |

All originals are restored. Android APK, private data and external app files were read back and byte-compared; the six recorded display/rotation values match. iOS app bundles and stopped data matched; post-boot private data matched excluding OS-owned SplashBoard snapshots and container-manager registration metadata. Appearance and content size match. A first iOS restore attempt encountered transient retired container registration; waiting for valid registration resolved it. No reset or reauthentication was used.

Backups and restoration comparisons remain at `/tmp/tm-layout-assessment.j38pKo`. No original backups were deleted. No motion or transparency preferences were changed. Simulator/emulator navigation is not physical-device or assistive-technology certification.

The LSP tool was advertised but unavailable to this worker's Fabric registry. Bounded source reads and searches supplied the code evidence. No diagnostics or build claims are substituted for visual review.

### Capture naming and rejected navigation attempts

All capture names below live in `.impeccable/review/`. In the matrices:

- `A(name)` means `tagmaster-layout-before-android-name.jpg`.
- `I(name)` means `tagmaster-layout-before-ios-name.jpg`.
- Other names are written in full.

Fresh Home and Summary captures suffixed `current` use the newer installed artifacts. `A(home-dark)` actually shows the original app's initial changelog, not an unobstructed Home. Two misnamed attempts were corrected to `A(home-return-dark)` and `A(browse-landscape)`. `A(search-ime-landscape)` shows Search after rotation dismissed the keyboard; it is not landscape-IME evidence. `A(search-keyboard-dark)`, `A(settings-dark)` and `A(settings-filters-dark)` are rejected navigation attempts, not evidence for the screens their names suggest. No layout findings rely on these three files.

## Answers to the layout questions

### Reading order and squint test

The default layouts pass. Fresh Android and iOS Summary make the title and blue Sheet Music action easy to distinguish, with smaller metadata between them: `A(summary-dark)`, `I(iphone-summary-current)`, `I(ipad-summary-current)`. Home's wordmark leads the navigation list; Favorites follows as its own group, with small credits below: `A(home-dark-current)`, `I(iphone-home-current)`. Preserve this hierarchy. The exception is Android empty Tracks: disabled playback geometry receives the first and strongest area of attention while the useful explanation sits below it.

### Grouping

Catalog rows correctly keep title, alternate title, catalog metadata and material availability together. Separators distinguish tags without card shells: `A(browse-dark)` and `tagmaster-ios-glass-tabs-phone-browse.jpg`. Android Summary's captions remain close to their values. iOS Summary's Sheet Music action almost touches Lyrics, so materials and prose read as one group. `DPTagSummaryController.m:329–351` places them in adjacent grid rows without a section gap. The narrower width of the sheet button is intentional reserved loader space, not an alignment bug: `DPTagSummaryController.m:414–433`.

### Rhythm

Android Settings already has useful contrast between tight label/value pairs, related filter controls and 24dp section starts: `A(settings-top-current)`, `A(settings-lower-current)`, `settingsview.xml:40–99,151–171`. Home credits remain compact and do not compete with saved tags. Do not spread metadata into oversized rows. The optional iOS material/prose gap should create one clear section break rather than increase every interval.

### Structure

Lists are appropriate for catalog, saved tags and settings. Metadata columns are appropriate at normal sizes. The topology fails on compact iPhone AX5 Details, where an unbreakable heading column leaves almost no value width. The iPad AX5 fixture still fits, so the fix should depend on available width and scaled content rather than change all tablets or all native screens. Preserve four equal bottom tabs, iPad split navigation, native Back and existing readable-width containers.

### Density

The default screens suit frequent operation. Android Browse shows several identifiable tags, availability and useful provenance without losing the next action. Default iPad Summary's unfilled lower area is not a missing dashboard or a reason to add placeholder cards. Tracks with no playable material is the one confirmed density error: empty transport controls consume space but convey no usable choice. Large accessibility footers legitimately grow and scroll; the recent AX5 Home capture is not grounds to force a fixed compact height.

### Adaptation

Fresh Android font-2.0 Details wraps its date and site name without the iPhone failure: `A(details-dark-large)`. Browse centers readable content in landscape while retaining the four equal tabs: `A(browse-landscape)`. Portrait Search resizes above the keyboard and keeps its action visible: `A(search-ime-dark)`, `tagsearchview.xml:29–40,154–172`. Fresh iPad uses a Home sidebar and a separate detail pane. Current iOS page content uses `readableContentGuide` inside safe-area scrolling: `DPTagPageControllerBase.m:221–265`. The iPhone AX5 Details failure is confirmed. Intermediate widths, all locales, assistive focus order and landscape with an actively reopened IME remain gaps, not passes.

### Extremes

Empty Home, empty Teachable, empty Tracks/Videos, real multiline Lyrics, Android large text and Open Tag with numeric keyboard were rendered. Recent evidence covers iOS AX5 credits and Details, native glass wrapping, inline loaders and Android floating-key appearance. Long contributor names, long video metadata, many saved tags with reordering, rating chooser extremes and an actual chart through rotation remain unverified. Blank sheet fixtures show the key face but cannot establish full real-sheet visibility. Letterboxing is not a layout bug; never crop or stretch a score to remove it. Static screenshots cannot certify motion, playback cancellation or accessibility traversal.

## Full screen/state inventory

"Keep" means no defect in the rendered state, not full scenario certification. "Gap" means source-supplemented or unrendered. Source paths below are relative to `Android/TagMaster/src/main/` for Android and `iOS/tagmaster/tagmaster/` for iOS.

| Screen/state | Android evidence and assessment | iOS evidence and assessment | Severity / narrow action or preserved contract |
| --- | --- | --- | --- |
| Home, zero favorites | `A(home-dark-current)`; `res/layout/meview.xml:23–33`. Clear action list, empty copy and compact credits | `I(iphone-home-current)`, `I(ipad-home-current)`; `DPHomeViewController.m:64–105`. Clear empty Favorites and useful empty detail pane | Keep; retain Wickhop wordmark, original art and compact footer |
| Home, one favorite | Recent `tagmaster-android-footer-pitch-after-light-home.jpg`, source-correlated footer region | Recent `tagmaster-ios-footer-pitch-after-phone-light-AX5.jpg`; current footer `DPHomeViewController.m:68–110` | Keep inspected row/footer grouping; no new native fixture mutation |
| Home, many favorites | Gap. Current recycled list plus header/footer described at `meview.xml:23–33` | Gap. Automatic row height at `DPHomeViewController.m:64–67` | Do not call scrolling/reordering stress tested; preserve persistence and list order |
| Browse Latest / Rating / Downloads / Classic | `A(browse-dark)` renders Latest with real rows and all four equal tabs; `res/layout/tagmasterview.xml:45–66` | `tagmaster-ios-glass-tabs-phone-browse.jpg` renders one Latest result with all modes | Keep topology. Other three selected modes, their datasets and selection restoration are gaps |
| Search form and filters | `A(search-form-dark)` and `A(search-ime-dark)`; `res/layout/tagsearchview.xml:29–172` | Source only: `DPSearchViewController.m:40–119`, native search controller and filter rows | Android Keep. iOS Gap, no visual defect asserted |
| Search/filter keyboard | Portrait keyboard visible in `A(search-ime-dark)`. Rotated Search visible in `A(search-ime-landscape)`, keyboard dismissed | Source keyboard guide at `DPSearchViewController.m:94–101`; no current rendered keyboard capture inspected | Keep confirmed Android portrait placement. Landscape active IME and iOS keyboard remain gaps |
| Query results | Real Browse results in `A(browse-dark)`; separate Search results not submitted | Recent single-row Browse fixture only | Keep inspected catalog rows. Long search result sets and title extremes remain gaps |
| Query loading and pagination | Recent artwork comparison `tagmaster-consistent-loading-android-dark-comparison.jpg`; query hosts exist at `res/layout/tagqueryview.xml:30–44` | Recent inline loading host evidence and prior loading report; no new pending query rendered | Gap for full-screen initial/pagination transition. Preserve matched pole art/motion and native refresh |
| Query empty/error/retry | No deterministic network or empty-query state forced | No deterministic network or empty-query state forced | Gap. Do not manufacture an empty/error layout finding or change APIs to obtain one |
| Teachable, empty | `A(teachable-current)` shows centered explanation with intact native Back | Source only: `DPTeachableTagsController.m:69–85` supplies empty guidance and Browse action | Android Keep; iOS Gap. Preserve account-optional local lists |
| Teachable, populated/reordering | Not rendered, to avoid mutating saved lists | Source only: `DPTeachableTagsController.m:87–118`, automatic cells and native reorder path | Gap; preserve reorder, delete and sync contracts |
| Summary, ordinary/available materials | `A(summary-dark)` real title, alternate title, rating, key, sheet action and multiline Lyrics | `I(iphone-summary-current)`, `I(ipad-summary-current)`; `DPTagSummaryController.m:268–351` | Keep Android. Optional iOS P3 section separation; keep blue sheet/pitch and reserved loader slot |
| Summary, missing or long content | Real Android multiline Lyrics; absent recording media; extreme titles/notes not forced | Recent Lost fixture has no sheet action, `tagmaster-ios-glass-tabs-phone-light-summary.jpg`; very long Notes unrendered | Keep observed omission and wrapping. Extremes Gap; do not introduce placeholder fields |
| Details, default/large text | `A(details-dark)`, `A(details-dark-large)`; `res/layout/tagmiscview.xml:15–24,32–104` | `tagmaster-ios-glass-tabs-phone-light-ax5.jpg` fails; `tagmaster-ios-glass-tabs-ipad-light-ax5.jpg` fits. `DPTagDetailController.m:136–142,176–182` | iOS P1 adaptive metadata pairs. Android Keep observed font 2.0; narrow 320dp/long names remain Gap |
| Tracks, no playable tracks | `A(tracks-empty-dark)`; `res/layout/tagtracksview.xml:42–65`; `TagTracksFragment.kt:187–197` | No full empty page inspected | Android P2 confirmed-empty layout. iOS Gap; do not extrapolate Android behavior |
| Tracks, populated/loading/playback | Full current populated screen unrendered | `tagmaster-consistent-loading-ios-phone-dark-track-row.jpg` shows a large-type Tenor/loading row, not whole player | Keep inspected inline host. Whole player, long notes, seek/balance, extra parts and landscape remain Gap; no control loss |
| Videos, empty | `A(videos-dark)` shows User Submissions and concise absence copy | Source only: `DPTagVideoController.m:75–92` | Android Keep; iOS Gap |
| Videos, populated/long/missing metadata | Unrendered | Source only: `DPTagVideoController.m:96–161`, 80×60 thumbnail, 4pt metadata spacing and multiline rows | Gap; preserve teaching/user distinction, attribution and actual video destinations |
| Sheet music / floating key / rotation | Recent `tagmaster-android-sheet-key-surface-light-before.jpg`, `...dark-released.jpg` show fixture page and opaque key face | Source `DPTagSummaryController.m` owns preview; recent pitch report is secondary evidence only | Preserve fixed opaque face and aspect-correct chart. Actual score, zoom and rotated chart visibility Gap; no crop-to-fill recommendation |
| Rating dialog and pending rating | Chooser not opened; recent report documents pending host | `tagmaster-consistent-loading-ios-phone-dark-rating.jpg` shows pending inline pole; chooser source `DPTagSummaryController.m:388–409` | Gap for chooser layout. No real rating submitted. Keep numeric rating and native modal semantics |
| Open Tag dialog | `A(open-tag-current)` shows complete title, input, Cancel/Open and numeric keyboard; `res/layout/dialog_open_tag.xml:7–26` | Source only: `DPHomeViewController.swift:31–45`, native alert with numeric entry | Android Keep. iOS/error-message/large-type variants Gap; preserve ID/link validation |
| Settings, filters/theme/cache | `A(settings-top-current)`, `A(settings-lower-current)`; `res/layout/settingsview.xml:40–320` | `DPSettingsController.m:48–92,114–144` has Account, Saved Tags and Random Tag Filters. No matching cache/theme sections in this inspected controller | Android Keep. iOS visual Gap; do not invent a missing cross-platform setting or move account contracts |
| Settings, account prompt / owned sign-in entry | Log-in explanation and button rendered in `A(settings-top-current)`; button not invoked | Source only: `DPSettingsController.m:138–171`, `DPSettingsController.swift:203–230` | Keep Android entry hierarchy. Modal/confirmation variants Gap. Third-party FirebaseUI is excluded |
| Settings, private build | Hidden in captured normal debug build; registration at `res/layout/settingsview.xml:323–360` | Conditional section at `DPSettingsController.m:114–123,179–187` | Gap. No private-build configuration changed; no diagnostic log or secret content read |
| Changelog | `A(home-dark)` is original-app dialog, clear scroll region and anchored OK; host `MeActivity.kt:45–46` | No counterpart established in inspected Settings controller | Provisional Android Keep only; not a freshly rebuilt changelog check. iOS availability/layout Gap |

## Actionable fix bundles

### Bundle I1: iOS compact-width Details metadata, P1

Evidence: `.impeccable/review/tagmaster-ios-glass-tabs-phone-light-ax5.jpg` shows ID 1809 vertically as four single digits; the site name wraps to fragments. The long "Last Refreshed" caption sets a required single-line column width. The same recent expanded fixture does not collapse. Current `iOS/tagmaster/tagmaster/DPTagDetailController.m:136–142` explicitly enforces this heading behavior; the value column receives only the remaining width at lines 176–182.

Narrow fix: in this controller, retain the normal two-column grid when a useful value column fits. At compact width with large scaled headings, stack caption above value per metadata item. Decide from actual container width and scaled content, not device name alone. Keep 4–8pt within a pair and roughly 16pt between distinct pairs. Preserve all values, links, omission rules, ordering and 44pt link targets. Do not edit shared `DPGridLayout` or Pitch Perfect libraries. Do not change `TMPageViewController.m` or its glass/tab behavior to recover content space.

Targeted regressions: render 320/393pt phone content at default, XXXL and AX5; expanded pane at default and AX5; resize between fitting and stacked states. Include long contributor names, dates, link text, absent optional metadata and a long title. Assert nonzero readable value width, full text wrapping, no one-character numeric column, reachable final row and link behavior. Check VoiceOver sequence caption → value and native Back. No persistence/API changes.

### Bundle A1: Android confirmed-empty Tracks, P2

Evidence: `.impeccable/review/tagmaster-layout-before-android-tracks-empty-dark.jpg` puts disabled play/stop, 0.0/0.0s, seek and balance above the no-tracks explanation. The layout declares the player before the empty label at `Android/TagMaster/src/main/res/layout/tagtracksview.xml:42–65`. `Android/TagMaster/src/main/java/depollsoft/tagmaster/TagTracksFragment.kt:187–197` binds the empty explanation to the track collection but only disables the player when no selection exists.

Narrow fix: give a confirmed empty playable-track collection its own presentation, with recording notes if present and the existing explanation first. Suppress the unusable player region only for that settled empty state. Do not equate no selection, loading or download failure with no tracks. Keep the complete existing player, timeline, balance and all available part choices in playable/pending/error states. No change to the reusable `MediaPlayerView` audio behavior or shared libraries.

Targeted regressions: zero tracks with/without recording notes, one track, all standard parts, extra parts, no selection yet, remote pending, remote failure/retry, track replacement and returning via native Back. Confirm controls return with unchanged behavior, no empty-state player focus stops, correct scroll reach at font 1.0/2.0 and portrait/landscape. Use silent/deterministic media fixtures rather than real playback/account mutation.

### Optional bundle I2: iOS material/prose section break, P3

Evidence: fresh iPhone/iPad Summary captures show Lyrics starting immediately beneath the Sheet Music button. `iOS/tagmaster/tagmaster/DPTagSummaryController.m:329–351` has no section separation there. Add one conditional 16pt section gap before the first visible Lyrics/Notes block, not 16pt to each metadata row. Keep the layout collapsed when neither exists. Preserve the intentionally reserved loading accessory width at lines 414–433, compact key/rating controls, equal-height pitch feedback and full-width bottom tabs.

Targeted checks: sheet present/absent, Lyrics only, Notes only, neither, long prose and AX5. Button and loading-host bounds must remain unchanged between idle/pending/held/released states. This file is disjoint from I1 and all Android work; defer it unless the two higher-priority fixes are accepted.

## Preserve and handoff

Keep charcoal/nav colors, blue actions, green availability, the original Wickhop wordmark with its clipping fixes, the current shared vector shapes/palette and identical generated barber-pole/quartet motion. Preserve native `UIGlassEffect`, the older system-material fallback and the opaque Reduce Transparency fallback at `TMPageViewController.m:31–60`. Keep four equal full-width bottom tabs on phones and within the active iPad pane. Keep the compact footer, blue pitch pressed feedback and opaque Android `sheet_key_surface`.

No recommendation here changes cache, account, privacy, synchronization, links, native Back or sheet aspect ratio. No top-navigation return, monochrome redesign, placeholder cards, taller footer rows or reduced playback controls is proposed.

This is an all-screens inventory with explicit evidence limits, not a claim that every screen/state/device combination was rendered. Main should synthesize this report with the separate mechanical assessment before authorizing edits. The immediate implementation scope supported by visual evidence is I1 and A1; I2 is optional. Reordering, populated media extremes, deterministic errors and the remaining native device/type/theme combinations need targeted follow-up evidence rather than a whole-scene rewrite.
