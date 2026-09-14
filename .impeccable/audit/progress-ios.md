# Tag Master iOS polish progress

## Scope and acceptance ledger

Astra implementation, iOS only. No commits. Android changes belong to the other agent.
Read PRODUCT.md, repository AGENTS.md, iOS/UI_TESTING_SETUP.md, Fable's implementation brief and pertinent findings, Impeccable polish/craft-floor/iOS guidance. DESIGN.md is not Tag Master guidance.

- [x] Share and rate popovers anchored; iPad presentation and dismissal regression.
- [x] Tag load/refresh, random, query, rating, sheet music and track failures visible with recovery; loading counters balanced; nil sheet data never cached.
- [ ] Named toolbar, search/filter, share, pitch and rating controls. Important controls at least 44pt.
- [x] Dynamic Type labels and self-sizing tag rows, wrapping without height-cache disk reads; phone large-text regression.
- [x] Teachable Tags always discoverable with actionable empty guidance; favorites empty guidance.
- [x] Native phone home/detail/search/settings and iPad detail/share screenshots, light/dark/large text, one batched review and at most one confirmation.
- [x] Targeted unit and UI behavioral tests and requested Debug build; record actual failures.

## Source trace and decisions

Confirmed share has no popover anchor; rate has sourceView but no sourceRect. Detail nil load returns before decrementing busy count and silently pops. Exceptions in random, sheet music, rating and track are swallowed. Query error text is editable, exposes exception reasons and refresh fails to reset exhausted pagination. Tag cell heights use fixed cached metrics and main-thread disk reads. Teachable entry is conditional on count.

Keep native navigation, handwriting title, barber-pole backdrop, links, existing cache keys/flags, query parameters, preferences and account/sync untouched. No shared-library edits planned. Defer deep-link expansion, associated domains, entitlements, launch assets, navigation replacement and networking framework migration.

LSP navigation extension call failed: Unknown Fabric action extensions.lsp_navigation. Discovery for lsp_diagnostics returned no actions. Use compiler plus behavioral XCTest coverage; do not claim LSP passed.

## Device baseline before changes

Recorded with `xcrun simctl ui <UDID> appearance` and `content_size`.

- iPhone C8B74E44-94F7-4CED-A47F-DFF98E34237B: light, large.
- iPad 55A9555A-9714-4FE0-8F0D-1035652526F9: light, large.
Restore both after capture/testing. No other settings changes planned.

## Implementation and first verification

Implemented local share/rate anchors, recoverable tag/random/query/rating/sheet/track failures, nil sheet-data guard, named toolbar/filter/pitch/rating controls, local accessible pitch activation and touch cancellation, semantic rating/key colors, wrapping self-sizing tag rows, stacked search/settings fields and 44pt controls, always-visible Teachable Tags and empty guidance. No shared library changes.

Requested Debug build passed (`/tmp/tagmaster-ios-build.log`). First unit run failed to link a direct Quick Look class reference in tests; switched to runtime class lookup. Second run found three failures: real 200,073pt row heights from shared star-grid loading overlay, a phone-only popover expectation, and an overlong local-URL fixture cache key. Replaced only the Tag Master row overlay with a non-expanding semantic label; corrected the two fixture assumptions.

43 targeted unit tests now pass: 10 new polish regressions, 28 existing search tests, 5 existing app logic tests (`/tmp/tagmaster-ios-phone-tests.log`). Initial UI run failed because system Copy is not a Button. Fixed locator to match actual accessibility element. Phone UI now passes (`/tmp/tagmaster-ios-phone-ui.log`). iPad share-anchor unit test plus native UI share presentation/dismissal and rating path pass (`/tmp/tagmaster-ios-ipad-tests.log`). Screenshots attached to xcresults; batched visual inspection pending.

Now testing both devices in dark appearance at accessibility-extra-extra-extra-large. Original settings remain recorded above and will be restored.

## Batched visual review and corrections

Viewed reduced native captures for phone home/detail/search/settings in light and dark AX5, phone browse rows, iPad share and dark AX5 detail. Confirmed preserved handwriting and barber-pole backdrop. Found collapsed detail tabs, missing numeric rating, low-contrast metadata on the backdrop, and tiny segment values at AX5. Corrected these together: local minimum native-tab height, stack-based rating value/bar, label colors for text over the backdrop, and native menu alternatives at accessibility sizes that keep the original segmented indices as the source of truth.

Direct UI traversal then exposed recording notes taking the entire track page at AX5. Moved notes into the scrolling table header and made track rows self-sizing. Added regression coverage. Harness failures during this correction stage were fixed after inspecting their evidence: flush pending trait updates; assert visible menus instead of hidden segments; account for empty track states; tap outside the share popover away from the home gesture area. The initial empty-track hypothesis was disproved by the hierarchy, which led to the actual notes-layout fix.

Final phone default-light run: 44 unit tests and 1 strict UI journey passed (`/tmp/tagmaster-ios-final-phone-light.log`). Phone dark AX5 strict UI journey passed (`/tmp/tagmaster-ios-phone-final-ax5.log`). iPad dark AX5 share-anchor unit test and strict UI journey passed (`/tmp/tagmaster-ios-final-ipad-ax5.log`). Final iPad light capture and one visual confirmation remain. No changes to shared libraries, account providers, stored list formats, URL handlers, entitlements or credentials.

## Final status

Final requested Debug build passed. Latest phone light run passed 45 unit tests plus the strict native UI journey. Both phone and iPad dark AX5 journeys passed, including Videos after its fixed-height rows were converted to self-sizing metadata. iPad light journey and iPad share-anchor unit check passed. The separate maximum-text lyrics-reachability test passed on both devices.

One confirmation batch is complete. It still shows excessive Summary grid spacing after page changes. Lyrics remain reachable by scrolling; the spacing is explicitly deferred rather than described as polished. Standard navigation-bar icon AX frames remain UIKit-sized and effective hit margins were not measured. The named-control requirement is met, but the blanket 44pt ledger item remains partial for that reason.

42 reduced native after screenshots saved under `.impeccable/review/tagmaster-ios-after-*.png`. Both simulators restored to light/large. Source diff hygiene and protected contracts passed. Final results, actual failures, commands, limits and screenshot paths are in `.impeccable/audit/verification-ios.md`. No commits, shared-library edits, deployment or association-file claims.
