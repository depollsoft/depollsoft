# Tag Master mechanical layout assessment

Assessment complete on `tagmaster/impeccable-ux-polish`. Source-only, isolated from the visual assessment. No devices, emulators, captures, builds, production/test edits, commits or pushes. Only this report was written in the repository.

## Acceptance ledger

- [x] Read AGENTS.md, PRODUCT.md, layout.md and Android/iOS platform references. Did not apply Pitch Perfect DESIGN.md to Tag Master or rerun context.
- [x] Run the requested detector once; retain actual exit status and output.
- [x] Check native sizing, wrapping, roots, adaptive widths, insets, row heights, clipping, repeated padding, landscape variants and hit areas.
- [x] Verify candidates against native XML, Kotlin/Java and Objective-C context; separate source facts from unobserved runtime failures.
- [x] Record false positives and coverage gaps.
- [x] Confirm the sole tracked diff remains the pre-existing `Android/TagMaster/src/androidTest/java/depollsoft/tagmaster/FooterPitchRegressionTest.kt`. It was not edited by this assessment.

## Detector evidence, separate from source findings

The command ran once:

```sh
/Users/depoll/.pi/agent/skills/impeccable/scripts/impeccable detect --json --scope layout Android/TagMaster/src/main/res iOS/tagmaster/tagmaster
```

Help confirmed multiple positional targets. An earlier failed orchestration did not launch this command, as verified by absent output/status files before the actual invocation.

- Actual exit status: `0`.
- `/tmp/tagmaster-layout-detect-before.json`: `[]`, 3 bytes.
- `/tmp/tagmaster-layout-detect-before.stderr`: empty.
- `/tmp/tagmaster-layout-detect-before.status`: `0`.

The command succeeded with no findings. Native detector coverage is **unproven**: help describes HTML/CSS/browser analysis and regex matching for other files; the result reports no native file counts. Do not treat `[]` as native-layout certification. The exact requested command may load local design context, so any Pitch Perfect-specific rules would not be authoritative for Tag Master. No such findings were emitted. No auto-fix, drift repair, configuration edit or second scan was run.

Pi-lens LSP/ast-grep actions were unavailable through discovery in this isolated agent. Bounded source search/read was the fallback. `xmllint --noout` passed for all 29 XML files across `res/layout` and `res/layout-land`, exit 0. This checks XML structure, not Android measurement or runtime constraint satisfaction.

## Priority shortlist

All three are source-backed candidates for focused native validation, not screenshot-confirmed defects. No scene rewrite is justified by this scan.

### M1. iOS Search has no width-based fallback for seven Parts segments

Priority P2. High confidence in the geometry condition; actual device frames were not measured.

- `iOS/tagmaster/tagmaster/DPSearchViewController.m:67-80` creates seven Parts segments and sets a 44pt minimum height, but no per-segment width floor.
- `iOS/tagmaster/tagmaster/DPTagPageControllerBase.m:63-66` switches to the existing menu only for accessibility content-size categories. It does not consider available width.
- `DPTagPageControllerBase.m:165-180` pins the control stack to an inset-grouped cell's layout margins.

Mechanical criterion: seven separate targets need at least `7 × 44 = 308pt` of control width to satisfy the supplied native touch-target floor. Below 308pt, at least one segment necessarily falls below 44pt; a 280pt control gives 40pt per equal segment. The cell margins further reduce the phone/window width available to this control. A 44pt overall height does not solve the horizontal deficit.

Focused follow-up: measure the Parts control at the smallest supported phone/window width at default text size. If its width is below 308pt, use the already-present menu fallback based on measured fit, retaining all seven options and their index mappings. Do not replace the filter contract.

### M2. iOS Details reserves an uncompressible, single-line header column

Priority P2. High confidence in the constraint policy; the first failing text category/window size is unmeasured.

- `iOS/tagmaster/tagmaster/DPTagDetailController.m:136-141` forces every metadata heading to one line with required horizontal compression resistance, including `Last Refreshed`.
- `DPTagDetailController.m:175-179` retains an intrinsic heading column, an 8pt gap and a star-sized value column at every width.
- `iOS/tagmaster/tagmaster/DPTagPageControllerBase.m:185-193` scales those headers with Dynamic Type. Lines 228-250 give content 16pt side margins and lock the container to the scroller width.
- `iOS/depolllib/depolllib/DPGridLayout.m:124-201,301-329` pins the column sequence and each cell's horizontal edges. It does not provide a narrow-width reflow.

Mechanical criterion: the layout must fit `uncompressed heading width + 8pt + usable value width` inside the readable content width. Larger Dynamic Type increases the first term without a width fallback. Wrapping the value button's height cannot restore horizontal space taken by the heading. If a heading alone exceeds the available width minus the gap, required constraints cannot all hold with nonnegative value width.

Focused follow-up: test Details with all metadata present, long names/dates, maximum accessibility text and narrow portrait/window widths. Check both value readability and Auto Layout conflict logs. If the condition fails, reflow only these metadata rows when they no longer fit. Preserve links, default-density layout and current identity.

### M3. Android initial tag-load error has no scroll escape in short windows

Priority P2. High confidence in missing overflow accommodation; no clipping was observed on a device.

- `Android/TagMaster/src/main/res/layout/tagdetailview.xml:85-112` places `detailErrorState` in a centered, non-scrolling vertical `LinearLayout`, with 32dp padding on all sides, message text, a 16dp action gap and a minimum 48dp Retry button.
- `Android/TagMaster/src/main/java/depollsoft/tagmaster/TagDetailActivity.kt:114,133-153` fills the message and exposes this state after an initial-load failure. Existing-content refresh failure instead uses a Snackbar and is not this finding.
- `Android/TagMaster/src/main/res/values/strings_detail.xml:5` supplies the multi-sentence connection/retry message.

Mechanical criterion: the error needs at least `128dp + measured message height`, before any button growth. When that exceeds the remaining frame height, the non-scrolling parent has no way to expose overflow. Large fonts and short landscape/multi-window frames increase the risk. The adjacent loading state already has a ScrollView; the error state is outside it. Tabs are hidden for initial failure, so do not incorrectly subtract tab height when reproducing.

Focused follow-up: force an initial-load failure without cached content at the shortest supported landscape/window height and largest font scale. Verify the entire message and Retry remain reachable. If not, give this error content scroll accommodation without changing Retry, native Back or loading artwork.

## Verified false positives and preserved behavior

- Fixed 24dp icons, 48dp icon buttons, 80×60 thumbnails and generated barberpole/quartet geometry are not fixed-height text rows. XML icons sit inside larger interactive containers. Do not resize or replace shared artwork on this evidence.
- iOS Home, query, Teachable, Tracks and Videos use automatic table row heights. `DPHomeViewController.m:442`, `DPTagQueryViewController.m:277`, `DPTeachableTagsController.m:112`, `DPTagVideoController.m:193`, and `DPTagTracksController.m:53,71-79` also show header remeasurement where relevant. Android saved rows use wrap-content and a 56dp minimum, not an exact row height.
- The 56pt iOS tab height is an initial floor, not a fixed large-text cap. `TMPageViewController.m:125-145,194-198` measures labels; line 32 uses equal distribution. Android `FullWidthTabLayout.kt:16-39` likewise measures labels into equal slots. Keep four full-width equal bottom tabs on both platforms. The generic expanded-width rail recommendation does not override the brief.
- `TMPageViewController.m:45-58` retains native Liquid Glass/material and Reduce Transparency/Motion behavior. No custom material replacement is warranted.
- Android `EdgeToEdge.kt:88-125` combines side insets and content-width capping. Search applies IME padding to its content container and width capping to its scroller. Apparent duplicate system-bar padding is not confirmed: `Android/DepollSoftCommon/src/main/java/depollsoft/lib/activity/RichApplication.java:61-90` consumes those bar/cutout insets after padding the outer content while passing IME through. Do not remove child listeners from static appearance alone. Runtime keyboard/nav-mode transitions remain untested.
- Both Android landscape Summary/Tracks variants are live resource alternatives, inflated by `TagSummaryFragment.kt:38` and `TagTracksFragment.kt:26`. Their equal columns are not unused files. Orientation-only two-column choice remains worth a narrow-window/large-text probe, but no failure is established here. No exhaustive dead-resource claim is made.
- The footer is content-sized, not a fixed oversized row: Android `meviewfooter.xml` uses Flow wrapping and 48dp link targets; iOS `DPHomeViewController.m:194-209` measures legal/credit rows and changes their axis only when required. Preserve the supplied default measurements of roughly Android 128dp/iOS 112pt; this assessment did not remeasure them.
- Android spacing includes 4/8/16/24dp and local 5dp metadata gaps/20dp indents. Hardcoded values alone do not demonstrate bad layout. No taste-based token rewrite is recommended. XML parsing found no duplicate attributes; this does not prove absence of all runtime duplicate constraints.
- Android sheet music uses PhotoView; iOS `DPTagSummaryController.m:465-525` uses Quick Look with a minimum-44pt app-owned key control. Aspect-preserving letterboxing is valid. Keep the opaque Android sheet-key background and blue pressed feedback. Full chart visibility and rotation cannot be certified without native rendering.

## Coverage and gaps

| Screen/state family | Source evidence inspected | Still unevidenced |
| --- | --- | --- |
| Home, 0/1/many favorites | Header/footer composition, saved-row sizing, automatic rows | Actual empty/one/many layouts and long saved titles |
| Browse modes; results/loading/empty/error/pagination | Shared query roots, four-mode wiring, automatic rows, Android list padding and loading footer | Live pagination, failures, status wrapping, scroll position |
| Search/filter/keyboard | Android scroll/IME path; iOS keyboardLayoutGuide, grouped rows and menu fallback | Keyboard transitions and actual segment frames |
| Teachable empty/populated/reordering | Automatic row heights, Android list insets and saved-row shell, iOS reorder callbacks | Empty-state rendering and reorder drag geometry |
| Summary/Details/Tracks/Videos | Native roots, text constraints, missing-content visibility, live landscape resources, measured track header | Long-content renders and native playback controls/presentation |
| Sheet music/key/rotation | PhotoView/Quick Look entry and key-control sizing | Actual chart occlusion, aspect and rotation outcomes |
| Rating/Open Tag | Android wrap-content native dialog layouts; iOS native action sheet/alert and iPad rating source anchor | Large-font dialog bounds, keyboard presentation |
| Settings/account/private build/changelog | Android scrolling settings/theme controls and native changelog message dialog; iOS grouped settings, private rows, sign-in page-sheet wrapper | Runtime account/cache/private-build states; iOS changelog/theme/cache UI presence not established by this bounded scan |

All phone portrait/landscape, expanded/iPad, light/dark and font-scale/Dynamic Type combinations remain **render-unevidenced** in this role. Native Back, persistence, API, links, ratings and account flows were not exercised or mutated. Third-party FirebaseUI screens were excluded; the iOS AuthPicker wrapper and Android sign-in launch were read only. No backups/restoration were needed because no device or user-data changes were made. Existing review captures and the independent visual report were not read.

## Native applicability

DOM/focus-order rules, CSS gap/margin prescriptions, browser overflow/stacking contexts, viewport/zoom emulation and web density variables do not directly evaluate UIKit or Android Views. Assess their native equivalents through measured bounds, Auto Layout/MeasureSpec, scroll reachability, safe-area/IME insets and accessibility target frames. Branded colors, Wickhop lettering, green availability, shared mark/motion, bottom navigation and compact footers are binding, not detector violations. No production change is proposed until the main assessment combines these source candidates with independent native evidence.
