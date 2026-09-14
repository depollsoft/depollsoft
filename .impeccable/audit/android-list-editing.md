# Android saved-list editing

## Plan recorded before code edits

Scope: Favorites on MeActivity Home and TeachableTagsActivity only. Sole Android writer on `tagmaster/impeccable-ux-polish`; no commits or pushes. Preserve toolbar colors, full-width tabs, compact footer, row content/spacing, storage keys, cache, migrations and Firestore contracts. Impeccable context had already run and was not rerun. Read Operate and craft-floor. DESIGN.md is Pitch Perfect-only; Tag Master incumbent code/captures and the user's pinned direction govern.

Trace completed before edits:

- SavedTagListAdapter was a stable-tag-ID ListAdapter using asynchronous diffs and a one-shot Bindroid tracker. Home wraps it in ConcatAdapter with ordinary non-stable header/footer adapters; Teachable uses it directly.
- SavedTagItemView delegates loaded-row taps to TagItemView, failed-row taps to the catalog URL. Row long-click and overflow opened subclass context menus.
- ListModel collection notifications call storeValue, retaining `tagmaster.lists` and users/{uid} lists.favorite/teachable. fromFirestore suppresses writes and replaces backing stores. Existing migrations must not change.
- Read-only iOS reference uses explicit Edit/Done, delete and table move callbacks. Home protects its header section.

Planned checks: atomic model moves and persistence; both edit modes and row loading states; actual native pointer dragging/autoscroll/cancellation; concurrent source changes and pending delete; accessibility and responsive geometry; targeted and existing tests followed by one full unit run; backup and byte-verified restoration.

## Implementation

- Text Edit/Done is always in both toolbars, disabled empty. Home retains Settings. Mode survives recreation and automatically ends after the final removal.
- Back and Up retain native navigation. Pause, destruction, Done and pointer CANCEL discard unfinished previews. Editing itself remains enabled when returning to a surviving screen. Handle interception CANCEL is not mistaken for drag cancellation.
- SavedTagListAdapter now owns synchronous snapshots and a private mutable preview. No asynchronous differ can race ItemTouchHelper notifications. Stable IDs remain local to the saved adapter; Home ConcatAdapter keeps default NO_STABLE_IDS for its static adapters.
- Handle-only ItemTouchHelper movement uses bindingAdapterPosition and verifies holder adapter/ID/position. Only pointer UP commits, once. clearView never persists. Cancellation dispatches CANCEL to the attached RecyclerView before helper teardown, stopping its selected gesture and autoscroll runnable.
- ListModel Snapshot/reorder/move validates collection identity, revision, expected order and complete permutation. Rejected/no-op changes do not enter a transaction. Existing valid moveUp/Down behavior remains; missing IDs and boundary moves are now safe no-ops.
- External source updates cancel previews and refresh current data. One accessibility announcement reports an interrupted drag. Confirmation removes by ID from the current list, never a stale index or restored snapshot.
- Editing shows 48dp Material remove/handle targets. Normal mode has neither overflow nor drag controls. Cached/failed/pending row taps cannot navigate while editing. Native confirmation says removal affects this saved list, not the catalog.
- Accessibility actions expose remove/up/down and current position/list context; boundary actions are omitted. Alt+Up/Down on the handle is the keyboard alternative. Spoken positions refresh on drop instead of each crossed row. Cached holders refresh editing state when reattached.
- Existing row content, loader and availability indicators remain. Saved rows alone wrap metadata groups when editing controls leave insufficient width. Normal row layout is restored on Done; TagItemView and shared renderers/layouts are unchanged.
- Removed saved-row context-menu handlers and both dead menu XML files. Removed the unused overflow string. Disposed adapters clear callbacks and their RecyclerView reference so pending one-shot registrations cannot retain a destroyed screen.

## Acceptance ledger and actual results

- [x] Model boundaries, first/middle/last, invalid/missing ID/destination, empty/single/no-op, complete permutations, stale source identity/revision, insertion/removal/reset/backing-store/collection replacement, and serialized order/reload.
- [x] Ten model tests pass. Static mocks verify exactly one Preferences and one Firestore write per committed drop, existing keys/field paths, no additional writes for rejected/no-op drops, and no Firebase access during fromFirestore replacement.
- [x] Ten new instrumentation tests pass on both actual production screens. Tests cover Edit/Done, removal cancel/confirm/last, recreation, cached/failed/pending taps, normal loaded navigation, custom accessibility actions, keyboard moves, invalid holders and row long-press/handle no-op behavior.
- [x] Native SOURCE_TOUCHSCREEN events drag first to last and back across the viewport. Tests observe preview order before release, zero collection callbacks during movement, one callback per drop, serialized/model/visible order agreement, and intact Home header/footer adapters.
- [x] Real gestures cover CANCEL, Done and pause, plus external insert/remove/reset/collection/backing-store changes. Late clearView cannot commit. Pending deletion retains external additions and removes only the named ID.
- [x] Four responsive native probes pass on both screens: light/font1.3 portrait, dark/font2 portrait, dark/font2 landscape, expanded light/font2 at 1600x2560 density240. Assertions verify actual font/orientation/width, 48dp targets, full title and ID/rating/date/download text bounds. Fixtures include long titles and green sheet-music availability.
- [x] Existing FavoritesFlowTest, MeActivityTest and the updated saved-list LayoutRegressionTest: OK, 46 tests. The legacy layout test now uses the guarded production-screen Edit/drag fixture rather than a phantom overflow view or model-only reorder.
- [x] One full TagMaster unit run: 314 tests, zero failures/errors/skips. This ran after the known model/native lifecycle fixes. Later saved-row metadata sizing and disposal refinements were checked with the final focused 14-unit run and native confirmation batch, not a second full unit run.
- [x] Final focused unit run: 10 model + 2 compact loading binding + 2 polish layout tests, zero failures/errors/skips. Final native confirmation: OK, 10 tests, plus four OK, 1-test responsive runs.
- [x] Mechanical checks confirm Snapshot/reorder/move, SavedListEditor wiring in both activities, savedlistmenu/editSavedList, savedTagRemove/savedTagDragHandle and drawable/string resources. No savedTagMoreOptions, old saved-list context-menu names, showContextMenu/onCreateContextMenu or old overflow-string references remain in TagMaster/src. git diff --check passes.

## Failures found and fixed

- Lens extension navigation was unavailable, so compiler/test diagnostics were used. Initial unit fixture compilation and cached Robolectric Preferences-context errors were fixed.
- Native cancellation exposed an ItemTouchHelper autoscroll null-RecyclerView crash after detach. Explicit gesture cancellation before detach fixed it.
- Done/Edit exposed unchanged offscreen RecyclerView holders retaining old controls. Rebinding editing state on attachment fixed it.
- Native fixture races required explicit dialog-root matching and settled/current handle geometry. Shell/UiAutomation rotation alone did not change activity orientation; responsive fixtures now request and verify native activity orientation.
- An interrupted pointer run and an unintended external window disrupted a diagnostic run. The external window was dismissed without authenticating or signing out. All final guarded fixtures passed signed-out checks.
- Restoration handled an empty external archive and Android's refusal to restore that directory's timestamp. There were no external files to change; file-byte verification is exact.

## Safety and restoration

Before native testing, validated original APK ZIP and private/external TAR archives were saved at `/tmp/tagmaster-list-editing-backup-20260907`, with SHA256SUMS. The baseline was emulator-5554, 1080x2400, density420, font1.0, night yes.

Native setup fails before mutations if Firebase has a current user. No user was signed out and no live signed-in/cloud writes were performed. Fixtures restore original collection references and their fake cached file/memory entries. Preferences/Firestore write assertions use unit mocks rather than a live account.

Restoration is complete:

- Original APK is byte-identical, verified by cmp after pulling the reinstalled APK.
- All 19 original private files have matching SHA256 manifests; introduced test files were removed, without clearing/resetting app data.
- External storage contained zero files before and after testing.
- Original font, accelerometer/user rotation, and all three animation scales match recorded values. Display is again 1080x2400 density420, font1.0, night yes.
- Backups, post-test archives and verification manifests are retained. The emulator has the original app APK, not the new implementation. Build outputs remain available in the worktree.

## Evidence and limits

Four JPEGs, each at most 800px, are retained in `.impeccable/review/`:

- `android-list-editing-home-before.jpg`
- `android-list-editing-home-edit.jpg`
- `android-list-editing-home-dropped.jpg`
- `android-list-editing-teachable-font2.jpg`

The batched light/dark/responsive captures are retained at `/tmp/tagmaster-list-captures`. There was one confirmation capture batch after the full-metadata refinement. Representative native images were displayed using Pi read unchanged.

Logs remain outside the repository: `/tmp/tagmaster-list-full-unit.log`, `/tmp/tagmaster-list-final-unit.log`, `/tmp/tagmaster-list-existing-native.log`, `/tmp/tagmaster-list-rich-native-final.log`, and `/tmp/tagmaster-list-rich-*.log` for responsive probes.

Limits: custom accessibility actions and keyboard behavior were exercised natively, but TalkBack speech was not manually audited with the service enabled. No live signed-in sync round trip was attempted. Model editing is a main-thread contract, consistent with activity and Firestore callback use. No iOS, shared-library, signing, backend, auth-schema, unrelated layout or renderer changes. No commits or pushes; model/controller changes are ready for Main's review.

## Final follow-up: editing-row default click action

Review found that the failed-row listener installed by `SavedTagItemView.init` left the outer row clickable during editing, exposing a useless default accessibility click action. `setEditing` now sets `isClickable = !editing`. Focusability, screen-reader focusability and custom actions are unchanged. Clickability is not gated on `failedToLoad`, so a pending row that fails after Done can still open the catalog. The only other production edits collapse the two awkward multiline position conditions.

Acceptance checks completed:

- [x] Extended `cached_failed_pending_rows_do_not_navigate_in_edit_mode` on a middle row. Cached, failed and pending editing states each assert outer-row clickability is false, ACTION_CLICK is absent, and Remove/Move Up/Move Down remain available. Existing native pointer taps still cannot navigate.
- [x] Done restores outer-row clickability and ACTION_CLICK before failure occurs. A subsequent failure emits the expected catalog ACTION_VIEW URI, intercepted with Espresso Intents so no browser opens. Restored loaded content still opens the actual TagDetailActivity with the correct ID.
- [x] Focused native run: **2 tests passed**, each running on Home Favorites and Teachable Tags, for **4 screen executions**. This includes `accessibility_moves_and_keyboard_keep_boundary_actions_current`. Zero failures. Runtime signed-out guards passed before fixture model/cache mutations.
- [x] `:TagMaster:assembleDebug` and `:TagMaster:assembleDebugAndroidTest` passed. Focused unit run: **4 tests passed**, 2 CompactLoadingBindingsTest and 2 PolishLayoutRegressionTest, zero failures/errors/skips. No model changes required another model or full-suite run.
- [x] Mechanical source checks confirm the clickability assignment, unchanged focus settings, both single-line conditions and native assertions. `git diff --check` passes. Lens navigation/diagnostic tools were unavailable in this session; Kotlin edit checks and actual compilation passed.

Device preservation was verified before installation and again after testing using `/tmp/tagmaster-list-editing-backup-20260907`. Both APK installs used `adb install -r -t`, without clear or uninstall. The exit-trapped restore reinstalled the original APK with `-r -d`, restored private files with the existing validated restore script, and removed only introduced files. No sign-out or live authenticated writes occurred.

Restoration proof: pulled original APK passes `cmp`; every private file matches the original SHA256 manifest and no extra files remain; external files remain zero. Font1.0, accelerometer rotation1, user rotation0, all three animation scales1.0, 1080x2400 density420 and night=yes match the recorded baseline. The original app is restored and stopped.

File-count correction: the previous report's 21-file count used newline-delimited paths. One original filename contains two embedded newlines. Filename-safe counting confirms **19 actual files**, matching all 19 original SHA256 entries. The earlier restoration statement above now uses the corrected count; no files were lost.

Logs: `/tmp/tagmaster-list-followup-build-unit.log`, `/tmp/tagmaster-list-followup-native.log`, `/tmp/tagmaster-list-followup-install.log`, `/tmp/tagmaster-list-followup-restore.log`. The restore command is retained at `/tmp/tagmaster-list-followup-restore.sh`; pre/post archives, pulled APKs and comparison manifests are under the backup's `followup/` directory.

Only SavedTagItemView.kt, SavedListEditingTest.kt and this report were edited for this follow-up. No visual changes, new screenshots, commits or pushes. Existing captures are retained.

## Coordinator review

Reviewed the controller, synchronous preview notifications, model snapshot validation, source-change handling, resource registrations and both activity lifecycles. Inspected native Favorites editing and font-scale-2 Teachable screenshots. Confirmed real touchscreen injection and storage mocks in the tests. The follow-up removes the inert editing-row click action without changing custom accessibility actions or normal navigation.

Code and evidence are ready for publication to [#44](https://github.com/depollsoft/depollsoft/pull/44). This update changes Android only, matching the existing iOS editing flow. Native TalkBack speech and live signed-in synchronization remain untested.
