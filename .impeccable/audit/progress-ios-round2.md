# Tag Master iOS round 2 progress

Started 2026-09-07. Worktree iOS only. No commits.

## Baseline

- Simulators before changes: iPhone C8B74E44 light/large; iPad 55A9555A light/large.
- Round-1 state read: audit, verification-ios.md, progress-ios.md.

## Plan (priority order)

1. Non-blocking loading + URLSession(15s) + streaming tracks.
2. Split view on regular width, readableContentGuide, showTag helper.
3. Inset-grouped Settings, UISearchController, large title Home, SF Symbol tabs, no Main header.
4. Adaptive launch screen, deep link tagmaster://tag/N, plist placeholder cleanup.
5. Build, unit + UI tests, screenshots, fix, confirm, restore sims.

## Log

- [ ] code changes
- [ ] build
- [ ] tests
- [ ] screenshots round 1
- [ ] fixes + confirm
- [ ] report

## 16:10 code complete, build green

- Changed: PageControllerBase (TMBusyIndicator, readable width, form cells), RemoteLocation (URLSession 15 s), XMLParser/DPTag.rate via helper (https),
  AppDelegate (iPad split view, showTagWithId:from:, tagmaster://tag/N), Home (large handwriting title, no Main header, Random Tag row spinner, footer copy),
  TagViewController (inline loading, nav-bar spinner, SF Symbol tabs), Browse (SF Symbol tabs), Summary (busy buttons, tinted Sheet Music), Tracks (AVPlayer streaming + cache),
  Videos (SFSafariViewController, a11y labels), Settings (inset-grouped table), Search (UISearchController + inset-grouped options), Details https link,
  LaunchScreen (system background + watermark), Info.plist placeholder strings removed, 21 legacy PNGs removed from project.
- Unit: full run 167 tests, 2 failed (rating button UIButtonConfiguration widened star column at AX5; search test needed table-row layout). Fixed both; TMPolishRegressionTests 12/12, TMSummaryLayoutRegressionTests 2/2.
- Next: UI tests on phone, then screenshots.

## 18:10 resumed after auth failure; full round-2 capture set taken on the 17:36 build

- Recaptured every screen (earlier sheet-music/tablet files were duplicates). Findings: Details link/arranger/singer buttons wrapped one character per line (configured button + multi-line titleLabel); page content on 8pt margins (bare container layoutMargins); video rows flat type; iPad detail tab bar hides Summary (stale item layout after column resize); Quick Look opened with chrome hidden; search empty state plain top-left label; Home Edit enabled with no favorites; track rows had no play affordance; sidebar footer links untinted; light inset groups invisible on Settings/Search.
- Fix batch applied to DPTagDetailController, DPTagPageControllerBase, DPTagVideoController, DPHomeViewController, DPTagTracksController, DPTagQueryViewController, DPSettingsController, DPSearchViewController, DPTagSummaryController, DPTagViewController, DPTagCell. Next: rebuild, recapture affected screens, tests.

## 18:40 confirm round

- Details fixed with TMWrappingButton (shared with the Summary key button) and single-line headings; empty search state, Home Edit, track play icons, sidebar link tint, light grouped backgrounds all confirmed in captures. Quick Look now pushed; bar visible from first frame, no stray toolbar after pop.
- Unit tests after fixes: 167/167 pass (/tmp/tm-r2-unit.log).
- iPad page tab bar: zoomed crop showed the pill continues under the floating sidebar (detail column extends beneath it on iPadOS 26). Trait override and itemPositioning had no effect and were removed; fix is pinning the bar to the safe area on iPad.

## 18:55 done

- Safe-area page tabs confirmed on iPad (all four visible). UI tests 39/39 (/tmp/tm-r2-ui.log), unit 167/167 (/tmp/tm-r2-unit.log), build green (/tmp/tm-r2-build.log). git diff --check clean. Simulators restored light/large. Report: round2-ios.md.
