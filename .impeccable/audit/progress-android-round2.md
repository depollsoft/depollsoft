# Tag Master Android — round 2 progress

Date: 2026-09-07. Working tree only; nothing committed. Round-1 work left intact.

## Re-score (code + round-1 after-captures)

| Dimension | Audit | Now | Still holding it back |
| --- | --- | --- | --- |
| Accessibility | 1 | 3 | Hyperlinks have no role; loading spinner on saved rows unnamed; no TalkBack dump pass yet |
| Performance | 2 | 2 | Favorites/teachables still ItemsControl (non-virtualized); LoadingImageView queue unbounded; cache deletes on main thread on Refresh |
| Theming | 2 | 3 | Verify M3 in both appearances on device; legacy dark/darker colors unused but still declared |
| Conformance | 2 | 3 | Detail load failure is Toast+finish (no inline error/retry); ListView/retainInstance legacy |
| Adaptivity | 2 | 2 | No max content width, no landscape two-column, no sw600dp resources |

## Plan

1. Adaptivity: content max-width helper (640dp; 960dp wide for two-column land), layout-land summary + tracks.
2. Performance: RecyclerView + ListAdapter (ConcatAdapter home, direct list on teachables), bounded image queue, cache deletes off main.
3. Conformance: inline detail error state with retry.
4. A11y: link role, loading names, uiautomator pass.
5. Verify: build, unit, connected, screenshots.

## Log

- [start] Read audit, verification, references, all layouts/activities. Device: emulator-5554, night yes, font 1.0, no overrides.
- [code] Wrote: dimens (content_max_width 640, two_column_max_width 640/960 land), EdgeToEdge.applyContentInsets, SavedTagListAdapter (+RowDivider), StaticViewAdapter, home RecyclerView (ConcatAdapter header/favorites/footer), teachables RecyclerView, MeHeaderView lifecycle-owned cleanup + favorites empty state, TagDetailActivity inline error state + retry + off-main cache deletes + brand title while loading, layout-land summary/tracks, LoadingImageView bounded queue (DepollSoftCommon), Hyperlink button role (DepollSoftCommon), content insets on browse/detail tabs, query/videos/summary/misc/tracks fragments, search, settings. Tests: MeActivityTest/FavoritesFlowTest -> homeList + favoritesAdapter.
- [resume] Previous incarnation cut off before verification. Hyperlink button role was logged but never saved; added now (onInitializeAccessibilityNodeInfo -> Button class, DepollSoftCommon). Inspected all 31 existing round-2 captures + captured search-expanded, sheet-large, teachable-large, detail-error, changelog-large (settings-large blocked by first-run changelog dialog; retaking after rebuild).
- [findings] Home footer ran straight into Favorites (8dp); saved/browse row indicators wrapped "Learning tracks" mid-phrase at font 1.3; "(Favorite)" kicker centered above the summary title; Settings had two filled buttons (Log in + View Changelog); Detail Retry button under 48dp. Everything else (browse, search, tracks, videos, sheet, rating, teachable, expanded/landscape) passed the floor.
- [fix batch] meviewfooter paddingTop 32dp; tagitemview indicators -> ConstraintLayout Flow (wrap as whole items, packed start, 24dp gap); summary markers moved to a status line under the title block in portrait + land, strings "Favorite"/"Teachable tag"; changelogButton outlined; unused legacy colors dark/darker removed.
- [confirm] Rebuilt, reinstalled, recaptured home/browse/summary/settings (light, large, dark, landscape, expanded) plus summary-saved and settings-bottom. All fixes hold; no further round needed.
- [verify] assembleDebug + testDebugUnitTest + :PitchPerfect:assembleDebug exit 0, 262 unit tests / 0 failures. connectedDebugAndroidTest exit 0, 105/105 (coordinator's onResumed poll fix in NavigationTestFixture). APK + private data backed up before and restored after. git diff --check (cr-at-eol) clean.
- [restore] font_scale 1.0, night yes, wm size/density reset, accelerometer_rotation 1 / user_rotation 0 (pre-audit values). Temp images/scripts/backup under /tmp removed; Gradle logs kept at /tmp/r2-build2.log, /tmp/r2-unit.log, /tmp/r2-connected.log.
- [report] .impeccable/audit/round2-android.md written.
