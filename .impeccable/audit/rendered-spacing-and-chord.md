# Rendered spacing and C7 chord

## Baseline audit, before production edits

One owner covers Android, iOS and shared notation. Scope is Summary, Details, Tracks and the quartet. No commit/push. Pending formatter-only changes in `CenteredPitchPipeButton.kt` and `DetailActionAlignmentTest.kt` are preserved in `/tmp/tm-spacing-chord/preexisting.patch`.

Read all three supplied reference copies. Original 1280×2856 and inferred 480dpi imply 426.67dp, not a confirmed source-device density. Reproduced on emulator-5554 at 1280×2856/480dpi, font 1.0, light. Production activity/fragments use an injected Tag 2-shaped model with no persistence or network writes. Unlike the reference, the fixture currently has no saved badges. Baseline captures and raw geometry are in `/tmp/tm-spacing-chord/baseline-captures` and `baseline-metrics.log`.

Measured native defects:

| Item | Before, logical units |
| --- | --- |
| Actual readable column | x=16 to 410.67, width 394.67dp |
| Both action faces | x=16 to 384, width 368dp; trailing shortfall **26.67dp** |
| Summary row heights | 19, 48, 19, 19, 19dp |
| Summary first-baseline strides | 37.33, 37.67, 23, 23dp |
| Details row heights | 19, 19, 48, 48, 19, 48dp |
| Details first-baseline strides | 23, 37.33, 64, 37.67, 49.33dp |
| Details caption/value baseline difference | 0px; alignment alone does not establish rhythm |
| Play and stop alpha-mask face bounds | 120×144px inside 144×144px view, x=12…132px; **40×48dp pill**, not circle |
| Transport content padding | 48px each side; 72px icon viewport begins after padding, center x=84px vs face center x=72px, **4dp whole-viewport shift** |

Causes: external 18.67dp pole width plus 8dp gap taxes both action faces. Metadata combines natural text heights with accessible link heights and conditional 4/16dp section gaps. Material icon styles add horizontal insets/padding despite 48dp outer wrappers. The supplied user's play triangle has canonical optical asymmetry; shifting its entire viewport is a separate defect.

Native conformance: native navigation and controls remain appropriate. P1 layout defects are the erratic metadata rhythm and off-center transport faces. P2 action width fails the intended readable column. The quartet is currently four rising separate notes with individual stems and vertical motion, not a C7 chord. No browser detector is used as native evidence.

## Acceptance ledger and intended structure

- [x] Details: uniform 48dp/44pt default rows, consistent gaps, exact first-baseline alignment; multiline grows with regular padding, whole-group stack fallback.
- [x] Summary: 28-unit compact facts together; separate accessible Rating unit, keep number/stars/Rate close.
- [x] Key/Sheet: visible faces fill actual 16-unit-inset column, pending pole inside stable symmetric content padding, unchanged bounds in all states, optional actions collapse.
- [x] Android transport: native bitmap alpha-mask square/center and icon viewport center across disabled/play/pause/stop/loading, preserving playback semantics; 8/16 group gaps.
- [x] Shared C7: C4/E4/G4/B-flat4, MIDI 60/64/67/70, common stem, C ledger, flat vector; stationary pitch and staggered opacity from one generated contract.
- [x] Targeted functional tests, then one full unit run per platform after known failures are fixed. Preserve loading IDs, query behavior, lifecycle and timed-stop ownership tests.
- [x] Actual default light/dark captures first, 360dp/large/landscape coverage, enlarged components and C7 pending on both platforms.
- [x] Original current apps/data/settings restored and byte-checked; backups retained.

Fresh backups completed and validated before baseline installation. The first baseline test failed because its launch ID still referenced the old fixture, so it could not find Summary. Correcting the test ID to `model.id` produced one passing native test and all measurements above. Both attempts restored Android data/APKs/settings with `ANDROID_RESTORE_VERIFIED`. iOS fresh backups are retained. Lens tools are not exposed; native compilers will supply diagnostics.

## Implemented changes

`DetailCompositionLayout.kt` now measures the actual caption/value children into explicit roles. Details uses 48dp rows with zero inter-row gaps at ordinary size. Summary collects 28dp facts before a separate 48dp Rating row with an 8dp group gap. Wrapped and stacked rows grow from their content with regular padding. No TouchDelegates or reduced targets.

`TMDetailLayout.m` applies the analogous 44pt/28pt roles. Configured UIKit link titles receive real symmetric content insets, rather than centering only an outer wrapper. Whole-group stacking remains. The final visual review found an additional AX5 defect: the numeric rating appeared as `3.…`. `TMRatingUnit` now moves the entire Rate action below the number when their measured widths cannot fit. Number and action remain adjacent at ordinary sizes. At AX5 the number occupies 108pt for a measured 107.97pt string, the Rate target is 212.67×82.67pt, and the group gap is exactly 8pt. New assertions check full number/title widths, nonoverlap, native target height, and repeated page switches.

Both Android Summary XML variants and `DPTagSummaryController.m` remove the external pole rail. Key and Sheet faces occupy the complete readable action column. Sheet's pole overlays a stable trailing area inside the face, with 44-unit symmetric text padding and a 12-unit trailing inset. Idle/pending labels retain the operation name. Faces grow for wrapped titles and whole missing actions collapse.

`mediaplayerview.xml` explicitly zeros all four Material insets, supplies symmetric 12dp logical padding, and uses centered text-start icon gravity for icon-only 48dp transport controls. The 24dp viewport is centered; the canonical play triangle is unchanged. Timing has an 8dp gap, and the existing balance/parts grouping remains. Native iOS AVPlayer controls are untouched.

`shared/tagmaster/quartet-loader.json` defines C4/E4/G4/B-flat4, MIDI 60/64/67/70 and intervals 0/4/7/10. The 216×96 canvas has five staff lines at y=17/29/41/53/65; four fixed heads at x=108, y=77/65/53/41; one stem at x=114; the C ledger at y=77; a flat beside B-flat; and a shared vector C7 label. Each head pulses between 0.65 and 1 opacity on the original 2.8-second cycle. No pitch displacement, extra wait, sound, haptics or per-frame image creation. Shared masks keep staff/ledger ink out of pulsing heads. Native paths and motion samples are cached/generated, not independently redefined.

The generator's existing deterministic output format is retained, including one sample per line. `generate_quartet_artwork.py --check` passes on the actual final tree without formatter configuration overrides. The barrelpole generator/art are unchanged.

## Measured before and after

Android measurements are from native 1280×2856/480dpi captures, font scale 1.0. Raw measurements are retained in `/tmp/tm-spacing-chord/baseline-metrics.log` and `metrics-default-light.log`.

| Measurement | Before | After |
| --- | --- | --- |
| Key/Sheet face x edges | 16…384dp | **16…410.67dp**, both real column edges |
| Face width | 368dp | **394.67dp** |
| Trailing width tax | 26.67dp | **0dp** |
| Summary plain row heights | 19dp, interrupted by 48dp Rating | **28dp each**, Rating moved after facts |
| Summary plain baseline strides | mixed 37.33/37.67/23/23dp | **28/28/28dp** |
| Details row heights | 19/19/48/48/19/48dp | **48dp each** |
| Details baseline and row-center strides | 23/37.33/64/37.67/49.33dp baseline strides | **48dp each** |
| Caption/value baseline difference | 0px | **0px** |
| Play/Stop alpha-mask bounds | 120×144px | **144×144px**, square enclosing circle |
| Icon viewport center inside 144px control | x=84px vs center=72px | **x=72px** |
| Horizontal transport content padding | 48px each | **36px each**, enclosing centered 72px icon |

Final iOS ordinary native fixture is 427pt wide. Page edges are x=16 and x=411; both faces are **395×44pt**. All seven Details rows are 44pt high with **44pt baseline and center strides**. Caption baselines are 189.40, 233.40, 277.40, 321.40, 365.40, 409.40 and 453.40pt. Value/caption offsets are +0.29pt for labels and -0.05pt for configured link titles. These are visible text measurements, not just wrapper bounds. Source: `ios-rating-confirm.log`, `SPACING_IOS` records. No new iOS pre-fix native numeric baseline is claimed; the retained numeric before/after reproduction of the three supplied defects is Android.

## Native visual evidence

All deliverable JPEGs are in `.impeccable/review/`, maximum dimension 800px. They come from native screenshots or native renderer exports; there is no reconstructed UI. Forty-five files match the two requested naming prefixes.

Start with these full-screen captures, then their enlarged components:

- `tagmaster-spacing-audit-android-default-{light,dark}-{summary,details,tracks}.jpg`
- `tagmaster-spacing-audit-android-{light,dark}-{actions,transport}-close.jpg`
- `tagmaster-spacing-audit-ios-phone-ordinary-{light,dark}-{summary,details}.jpg`
- `tagmaster-c7-loader-{android,ios}-{light,dark}-pending.jpg`
- `tagmaster-c7-loader-native-phases-close.jpg`

Additional Android profiles are `narrow-light`, 360dp/font 1.0; `large-dark`, 426.67dp/font 1.5; and `landscape-light`, 952×426.67dp/font 1.0. Each includes Summary, Details, Tracks, Sheet pending and missing-action captures. Long-field geometry also passes at 360dp/font 2.0. iOS captures include 393pt AX5 Summary, full rating, Details and final link, plus 834×393pt wide composition. Adaptive geometry covers 320/393/834pt at default, XXXL and AX5.

The ordinary Android fixture reproduces the Tag 2 catalog shape with the requested title, 3.35 rating, four parts, Classic 1, Major:Eb, Daniel Gillis and Mac Huff. Favorite/Teachable badges are test-only visibility changes, not saved model writes. Dates use fixed fixture timestamps and each platform's real formatting; this fixture's posted date displays December 6 under the simulator timezone. iOS ordinary composition uses the same catalog shape but no fake saved badges. iOS AX5 stress content remains the existing Lost fixture. Pending C7 IDs are test IDs, not live catalog requests.

Visual review was default-size first, followed by dark, enlarged transport/actions, large text and C7. The sole resumed visual correction was the AX5 numeric rating. Final confirmation shows the full `3.49`, an intact Rate title, wrapping Sheet Music, and unchanged ordinary composition. The final C7 image sheet was refreshed after the last native mask captures; it is not the stale pre-mask sheet. Scrolled AX5 and short landscape screenshots can show content crossing the viewport boundary, not clipped content bounds. Full-resolution Android PNGs remain in `/tmp/tm-spacing-chord/after-captures/cache/detail-composition-captures/` for 1280×2856 inspection.

## Tests, failures and resolution

No earlier failed run is counted as passing.

| Check | Actual result |
| --- | --- |
| Shared quartet generator/drift suite | **5/5 passed**; final generator `--check` passed |
| Native C7 phase parity | **28/28 comparisons passed**, light/dark × 1×/10× × phases 0, .125, .25, .5, .75, 1 and still |
| C7 parity details | Zero interior RGBA mismatches and zero geometry outside the one-pixel AA boundary; 224 documented half-coverage boundary pixels differ only at the 127/128 cutoff. Loop endpoints and still match exactly within each renderer. |
| Android native composition | Five profile runs, **1/1 each**; additional long-field/font-2.0 run **1/1** |
| Android pitch/pending lifecycle | **3/3 passed**, Summary note binding, Sheet note binding and compact pending/terminal/lifecycle |
| Android C7 pending journeys | Final light and dark runs **1/1 each**, followed by successful scoped export/cleanup |
| Android full units | Initial run **304 tests, 3 failures** in `BrandStyleRegressionTest` after controls changed to content-fitting height. Replaced fixed-layout-height assumptions with actual accessible target and text-fit assertions. Focused class passed, then **final full 304/304 passed**, no skips/errors. |
| iOS earlier focused run | 54 tests included two failures in the compact-home assertion path. The later focused correction passed **3/3**; the earlier full run then passed **222/222**. |
| iOS final visual correction | Ordinary plus adaptive composition tests **2/2 passed**, including full numeric rating and Rate widths at all nine size/category combinations. |
| iOS full run on final production code | **222 tests executed, 221 test cases passed**. One test, `testLongAX5RowsHugTextAfterTwoPageSwitches`, had four assertion failures because its aggregate height formula still assumed horizontal Rating. |
| iOS final failed-class resolution | Updated that formula to count the stacked number, progress bar and exact 8pt group gap. Retained the existing blank-space budget and added lexical-width/page-switch assertions. **Both Summary regression tests passed, 2/2**, including all four previously failing checks. No production edits followed the full run. |

The latest iOS whole-suite log is therefore not a green run. Its only failing test is corrected and passes in the final affected-class run; the other 221 cases passed on the same production code. A third unchanged full sweep was deliberately not repeated. There is no known unresolved test failure.

Evidence is under `/tmp/tm-spacing-chord/`: `android-full-final.log`, `ios-full.log`, `ios-full-final.log`, `ios-summary-final.log`, `ios-rating-confirm.log`, `android-behavior.log`, `android-long.log`, `c7-confirm-*.log`, `c7-parity-final.json` and `final-checks.json`. Native compilers supplied diagnostics because Lens/LSP tools were not exposed by discovery. Xcode logs include existing appearance-transition warnings; Android native graphics logs include the existing image-decoder diagnostic despite passing tests. No signing/network configuration was changed.

## Safety and preservation

- Retained original Android APKs, private data, external data and manifests in `/tmp/tm-spacing-chord/android/`. Guarded runners restored them and compared extracted file contents and APK bytes. Latest native restoration log ends `ANDROID_RESTORE_VERIFIED`. Final readback shows physical 1080×2400/420dpi, no size/density override, font 1.0; original dark mode and animation/rotation settings are restored.
- Retained both original iOS app bundles/data in `/tmp/tm-spacing-chord/ios/{device-id}/`. Runners resolve current registered containers after installation/reboot. Latest restoration ends with verified phone and iPad IDs and `ALL_BACKUPS_RETAINED_AND_READBACK_VERIFIED`. App bytes and user data were checked; current container-manager metadata and regenerated OS SplashBoard snapshots are excluded from running-container comparisons. Appearance, content size and Reduce Motion preferences match backups.
- No real favorites, rating, sharing or sign-in mutation. No shared PitchPerfect/depolllib, tool configuration, signing, network or live data edits.
- Mechanically compared both Summary XML ID sets against HEAD: all existing IDs survive in both orientations. Existing Xcode source registrations include `TMDetailLayout.m`, `TMQuartetArtwork.m` and the app-logic tests. Android layout/test compilation confirms its existing custom view and test discovery paths.
- `CenteredPitchPipeButton.kt` and `DetailActionAlignmentTest.kt` diffs byte-match `/tmp/tm-spacing-chord/preexisting.patch` exactly. Charcoal chrome, platform navigation, native fonts, full-width Android tabs, UIKit glass tabs, barrelpole art, opaque floating Sheet key, and timed-stop ownership behavior are preserved.
- Final `git diff --check` and generator drift checks pass. No commit or push.

Native conformance remains native, not a web substitute. This is a scoped rendered-layout and loader audit, not a new whole-app accessibility/performance score. No browser detector result is presented as native proof.

## Coordinator final review

Reviewed the default-profile Android Summary, Details and Tracks against the supplied issues, including an enlarged transport crop, plus ordinary iOS Details and paired C7 phase renders. Visible action faces now reach the actual content edges, provenance rows have a regular cadence, and Play/Stop glyph viewports sit centrally in square faces. The chord is C4/E4/G4/B-flat4 with a shared stem, flat, ledger and C7 label; only notehead opacity changes.

After the corrected height-budget assertion, the coordinator ran the complete final iOS unit target: **222/222 passed**, zero failures, `/tmp/tm-spacing-chord/ios-main-verified.log`. This supersedes the earlier not-green full-run status. The inspected restore script restored original apps/data/preferences and verified readback. Current Android XML results confirm **304/304 passed**, zero failures/errors/skips. All **17** artwork-generator/drift tests pass; both generators verify their current outputs.

Final scoped layout detector returned `[]`; this is supplementary only, not proof of native geometry. Native bounds, raster, behavior and capture evidence above supply that proof. Final source whitespace check passes. No physical-device certification or blanket full-UI-suite claim is made.
