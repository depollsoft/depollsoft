# Copyright notices

- [x] Android's visible `© 2021` footer now formats the current Gregorian year, currently 2026.
- [x] The XML-registered `CopyrightTextView` refreshes when its window becomes visible. Ownership text, links and styling remain unchanged.
- [x] iOS already generates its displayed copyright year in `DPHomeViewController.m`; no iOS change was needed. Historical source-file copyright comments were left intact.
- [x] Five `FooterPitchLayoutTest` tests passed, including initial year rendering, refresh after visibility changes and footer geometry at widths 320/360/600dp with font scales 1/1.3/2.
- [x] LSP diagnostics and `git diff --check` passed. No simulator installation or app-data changes were needed.

Test log: `/tmp/tagmaster-copyright-tests.log`.
