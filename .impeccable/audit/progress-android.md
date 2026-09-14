# Tag Master Android — implementation progress log

Lead: Fable (Claude Code). Audit: `.impeccable/audit/2026-09-07__tagmaster-android.md`. Working tree only; nothing committed.

## Plan

- Prereqs (lead, done): Material 3 theme skeleton (`res/values/themes.xml`, `res/values-night/themes.xml`, colors), shared toolbar include `res/layout/app_toolbar.xml`, insets helper `EdgeToEdge.kt`. `values-night-v21/themes.xml` removed (minSdk 24).
- Wave 1 (4 parallel astra agents, disjoint files):
  - astra-correctness: Bundle A (dates, PDF pages, https thumbnails, deep links) + PitchPipeButton performClick + sheet music screen toolbar + LoadingImageView cache + status indicators in tag rows/videos + typeset for those layouts.
  - astra-home: theme tuning, home screen (Material list rows, Open Tag dialog, random-tag progress/snackbar, teachable always visible, overflow on saved rows), teachable tags screen, footer targets, typeset.
  - astra-detail: tabs instead of bottom nav (browse + detail), Up affordance, progress indicators, snackbars, rating dialog, media player rebuild, summary touch targets, typeset, Espresso id updates.
  - astra-forms: search + settings screens (text field, exposed dropdowns, switch, toggle group), labelFor, Up, snackbars, cache size on IO, apply().
- Wave 2: review fixes, string consolidation, adaptivity (sw600dp max width), landscape check.
- Wave 3: device screenshots (light/dark/large), fix batch, Espresso run.

## Deferred (product decisions / out of refinement scope)

- Dynamic color (Material You): would replace the charcoal identity with wallpaper colors; left off.
- RecyclerView for favorites/teachables (F2): larger refactor of the Bindroid adapter path; main-thread I/O fixed instead (F1).
- NavigationRail / list-detail on expanded width (G2/G3).

## Log

- [start] Baseline `:TagMaster:assembleDebug` OK. Emulator-5554 online, night mode on, font scale 1.0.
- [Astra resume] Read PRODUCT.md, repository instructions, polish/craft-floor/Android references and audit bundles. Existing 63 tracked Android modifications reviewed by path and critical execution paths. No active Android workers. DESIGN.md is not Tag Master authority.
- [baseline] Required Tag Master debug build/unit tests and Pitch Perfect debug build passed, exit 0, `/tmp/tagmaster-android-integration-build.log`. Kotlin tests are present in XML results, not NO-SOURCE. LSP extension is not registered in this session (`Unknown Fabric action`); Gradle diagnostics used instead.

## Integration acceptance ledger

- [x] Safe HTTP/HTTPS routing for exact accepted hosts; null/missing query, malformed/negative/overflow IDs and shared browser predicate covered.
- [x] Shared pitch button touch down/up/cancel, accessibility toggle, timed click, replacement/detach covered without changing public contracts. Pitch Perfect build required.
- [x] Feed dates and absent dates covered; cache disk reads stay off main thread, persistence/API/share contracts unchanged; remove unrelated Tag.kt formatting churn.
- [x] Distinct PDF pages and media loading/stop/detach/stale completion covered by direct regression tests.
- [x] Home discovery, toolbar/Up/insets, search/settings, detail tabs/rating/tracks exercised on emulator; repair integration blockers.
- [x] Run connectedDebugAndroidTest once with 1500s timeout. Preserve and report actual failures, inspect genuine regressions and rerun only focused fixes.
- [x] Native after captures for home/search/detail/tracks/settings, dark/large and landscape/expanded when feasible. One inspection batch, at most one confirmation.
- [x] Final verification report with counts, commands, logs, failures, deferrals and release dependencies. No association files published or App Links verification claims.

## Device baseline before changes

`emulator-5554`: physical 1080x2400, density 420, no size/density override; `font_scale=1.0`; night mode yes; `accelerometer_rotation=1`; `user_rotation=0`; `window_animation_scale=1.0`; `transition_animation_scale=1.0`; `animator_duration_scale=null`. Restore these after tests and captures. No account or saved-data reset authorized.

## Integration results so far

- Safe predicate accepts only `barbershoptags.com`, `www.barbershoptags.com`, and `tags.depoll.com` with HTTP/HTTPS and positive Int IDs. Shared browser uses an explicit internal route. No hosted association verification performed.
- Pitch accessibility toggle works; owned holds/timed notes stop on replacement/detach. Media Stop remains enabled while loading or paused; leaving Tracks stops playback.
- `Tag.kt` reviewed against HEAD, functional changes restricted to synchronized memory cache and background disk loading, plus safe absent-directory handling. Automatic ktlint kept re-expanding the file. `Android/.pi-lens.json` disables mutation only, not diagnostics, to retain the narrow 27/30-line non-whitespace diff and prevent unrelated Android reformatting. iOS configuration untouched.
- Unit tests: 260 passed across 22 classes. Real native PDF test passed with two differently colored pages. Resource-backed unit tests initially exposed Java 21 SDK-default and Firebase app-initialization failures; resolved with SDK 28 and isolated Application test defaults under required Java 17.
- Full connected run: 101 tests, 55 passed, 46 failed, no skips; `/tmp/tagmaster-android-connected-run.log`, exit 1. Source verification shows legacy tests launch MeActivity while expecting a pager, tabs or off-screen settings. Existing HEAD MeActivity has neither pager nor bottom navigation. Failures preserved, not suppressed. Initial invocation failed at compile due PdfDocument not implementing Closeable; fixed explicit close before the runnable suite.
- Focused native form probes: 3/3 passed, `/tmp/tagmaster-native-probes2.log`. Initial probe assumptions were corrected: real dropdown selection, no keyboard-back dismissal of the dialog, and a fresh rating dialog per invocation. No app assertions weakened.
- Visual inspection batch found invisible light-theme menu icons, clipped large-text tab labels/Recording Notes, and zero-height landscape part selector. Repair batch adds semantic icon tints, scrollable tabs and one ScrollView covering the whole Tracks path. Expanded 1600x2560 at density 240 was inspected as an emulated size override, not tablet-hardware validation. Confirmation pending.

## Additional deferrals

- LoadingImageView uses bounded worker concurrency but an unbounded pending queue. Queue redesign deferred; existing cache/resize improvements retained.
- Hosted Digital Asset Links remains a release dependency for both first-party and BarbershopTags hosts. Local intent routing does not prove verified App Links.

## Final handoff

- Ledger completed as checks performed, not a claim that legacy connected tests are green. Final unit results: 262/262, 23 classes, no skips. Focused native flows: 4/4. Real two-page PDF passed. Full suite remains 55/101 with 46 documented wrong-screen failures.
- Final confirmation inspected light menu visibility, dark/large text, real Lead playback, search results, landscape parts and expanded Tracks. No further visual iteration. Device settings restored and read back exactly; only synthetic test cache fixtures removed.
- Commands, counts, screenshot paths, failures and deferrals: `.impeccable/audit/verification-android.md`.

## Native test navigation follow-up, 2026-09-07

- Repaired the three wrong-screen classes without production/config/iOS edits. Retained 22 Home, 24 Search and 31 Detail tests. Home uses explicit activity routes; Search exercises real dropdowns, submission/results and row-to-Detail navigation; Detail uses a deterministic disk-cache fixture and asserts actual fragment content, part selection, tabs/swipes and recreation. Removed the three classes' swallowed/tautological assertions.
- First targeted run: 74/77, `/tmp/tagmaster-navigation-targeted-1.log`. Fixed empty Favorites adapter expectations and a duplicated video-preview matcher; focused rerun 3/3, `/tmp/tagmaster-navigation-targeted-2.log`.
- One full connected integration run: 102/105, `/tmp/tagmaster-navigation-connected-final.log`. All repaired classes passed 77/77; PDF passed. Preserved XML in `/tmp/tagmaster-navigation-connected-final-xml/`.
- Inspected the other three failures and repaired test-only first-install prompt handling, missing favorite-row data, and rating-dialog root selection. Final affected-class run: Favorites 23/23 and Polish 4/4, `/tmp/tagmaster-navigation-isolation-final.log`. No production bug found. No second full rerun: 105 tests have passing evidence across runs, not a claimed single 105/105 result.
- Every connected invocation used timeout 1500 seconds and `settle:true`. No redundant unit/build run. Test counts and test-source whitespace check passed. Full commands/counts and remaining coverage caveats are appended to `verification-android.md`.
- Restored the exact original installed APK and private data after runner uninstall. Recursive private-data comparison and original device-settings comparison both passed. Backups and readback evidence: `/tmp/tagmaster-navigation-backup/`; restore log: `/tmp/tagmaster-navigation-restore.log`. No screenshots or commits.
