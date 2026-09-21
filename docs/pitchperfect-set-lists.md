# Pitch Perfect set lists

Pitch Perfect's Songs tab used to show one list. The data model has allowed
several lists per user for years (`users/{uid}/songLists/{listId}`); this
feature lets people use them as **set lists**: named, ordered lists of songs
and their keys for a rehearsal, a show, or an afterglow. The original list is
still the home list, called **My Songs**. It is always present, always first,
and cannot be deleted.

This document is the contract both platforms implement. Behaviour and
terminology are identical; only chrome is platform-native.

## Terminology

- **Set list**: any list, including My Songs. In copy: "set list", never
  "playlist" or "collection".
- **My Songs**: the display name of the list whose id is `default`. Its stored
  name is the legacy seed `"Default"` on every existing account; the UI shows
  `"My Songs"` whenever the stored name is blank or exactly `"Default"`, and
  shows whatever the person renamed it to otherwise.
- **Current list**: the list the Songs tab is showing. A per-device choice,
  never synced.

## Data model

Each list is one Firestore document `users/{uid}/songLists/{listId}`:

| field   | type   | notes |
|---------|--------|-------|
| `name`  | string | user-visible name; the default list's legacy value is `"Default"` |
| `songs` | array  | serialized `PitchedSong` objects, exactly as before |
| `order` | number | **new.** Position among custom lists, 0-based. Ignored for `default`, which is pinned first. Missing on legacy documents. |

Rules:

- **Ids.** `default` is reserved. New lists get a stable slug key made from
  the name plus four random base-36 characters (`saturday-show-k3f9`); a name
  with no slug-safe characters gives `list-k3f9`. A rename never changes the
  id, so songs never move.
- **Ordering.** `default` first; then custom lists by `order` ascending; lists
  with no `order` sort after all ordered lists, by display name. Reordering
  rewrites `order` on every custom list (0, 1, 2 …) so the sequence is dense.
- **Create.** Writes `{name, songs: [], order: <count of custom lists>}`.
- **Rename.** Updates `name` only.
- **Duplicate.** A new document with fresh id, name `"<name> copy"` (then
  `"<name> copy 2"`, `copy 3` … until unique), `order` after every existing
  list, and deep copies of every song **with fresh song ids**. Song ids are
  identity inside one list; copies across lists are independent songs.
- **Delete.** Deletes the document. Never allowed for `default`. If the deleted
  list was the current list, the current list becomes `default`.
- **Add from another list.** Deep copies of the chosen songs, fresh ids,
  appended to the end of the current list in their source order.
- **Clear all** (Settings). Empties `default` and deletes every other list.
- Local persistence (Android `Preferences` key `depollsoft.pitchperfect.SongLists`,
  iOS `UserDefaults` key of the same name) stores the same three fields per id.
- The current list id is stored locally under `depollsoft.pitchperfect.CurrentSongList`
  and falls back to `default` when missing or when it names a list that no
  longer exists.
- Remote wins on sign-in, as today. A remote `REMOVED` change drops the list
  locally; a remote `MODIFIED` change restores name, songs, and order.
- Older app versions read only `default` and write `{name, songs}` with merge,
  so `order` survives on the default document (where it is ignored anyway) and
  custom lists are untouched.

### Name validation

Shared by create and rename, on both platforms, in this order:

1. Normalize: collapse runs of whitespace to one space, trim.
2. Empty → "Give the set list a name."
3. Longer than 60 characters → "Keep the name under 61 characters."
4. Case-insensitively equal to `"default"` → "That name is reserved."
5. Case-insensitively equal to another list's **display** name (so "my songs"
   collides with the default list) → "You already have a set list with that
   name." Renaming a list to its own current name is allowed.

## The Songs tab

```
┌──────────────────────────────────────────────┐
│ ‹app bar›                       ✎   ⚙        │
├──────────────────────────────────────────────┤
│ ╭──────────┬─────────────┬──────────┬────╮   │  ← set list selector
│ │● MY SONGS│ SATURDAY SHOW│ AFTERGLOW│  + │   │
│ ╰──────────┴─────────────┴──────────┴────╯   │
│ Blue Skies                            C      │
│ ─────────────────────────────────────────    │
│ Shenandoah                            E♭     │
│ ─────────────────────────────────────────    │
│                                              │
│                                       (+)    │
└──────────────────────────────────────────────┘
```

### Set list selector

One machined part with N positions, drawn exactly like the instrument's range
selector: a single round-rect frame (5px radius, 1.5px `plate-hairline`
stroke) whose positions are split by 1px interior hairlines. It sits above the
song rows with 16dp side margins, 12dp above and 8dp below, and is 48dp tall.
Positions share the whole frame between them while they fit (so one list is
never a label in an empty frame); once they overflow they scroll inside the
frame, which stays put, with the ends fading toward the plate as the scroll
cue. The selected position is scrolled into view whenever it changes.

- **Position label**: the list's display name, uppercased, in Oswald Medium
  13sp with 0.16em tracking, 20dp leading and 14dp trailing padding (room for
  the dot, so labels never shift), single line, truncated with an ellipsis at
  180dp.
- **Selected**: 10% `plate-ink` wash, full `plate-ink` text, and a 6dp
  `plate-lit` dot 8dp inside the leading edge (the range selector's indicator,
  which the design system already permits).
- **Unselected**: `plate-ink-secondary` text at 75% alpha, no wash.
- **Last position**: a "+" glyph (`plate-ink-secondary`, 20sp), 44dp wide.
  Tap → New set list.
- Tapping a position switches the current list immediately: the song rows
  swap, every sounding note stops, the FAB/+ now adds to that list. A selection
  tick haptic accompanies the switch (the same one the range selector uses).
- Accessibility: each position is a button named "<name>, N songs" (or "no
  songs"), reporting selected state; "+" is "New set list".
- Long-pressing a position is where a list is managed: a menu **titled with
  the list's name** offering Rename set list…, Duplicate set list, Delete set
  list… (omitted for `default`), Manage set lists…. Android shows a popup
  menu anchored to the position (the name is its first, inert row); iOS uses
  the button's native menu with SF Symbols (tap still switches). Deleting a
  list that is not on screen leaves the tab where it was. On Android the
  delete Snackbar offers Undo, which restores the list under its id.
- Screen readers get the same menu as a named custom action on the position.
- Always visible, even with only My Songs: the "+" is how the feature is found.

### Song rows, FAB, editor

Unchanged, except that every operation targets the **current** list: add,
edit, delete, reorder, sort, and the row press that sounds the key. The song
editor opened for an existing song looks that song up in the current list.

### Empty state

- Current list is `default`: the existing "NO SONGS ON FILE / Press + to add
  your first song and its key" (iOS already says "Tap +"; each platform keeps
  its verb).
- Any other list: "NOTHING IN THIS SET LIST" / "Press + to add a song, or tap
  the pencil to add songs from another set list". Same engraved style.

### Edit mode

The pencil already flips to a Done checkmark, reveals per-row pencil and drag
handle, and surfaces Sort. Edit mode is about the list's **contents**; the
list itself (rename, duplicate, delete) has exactly two homes, the selector
long-press and the Set Lists screen, so the same verbs never appear in three
places.

| action | Android (edit mode) | iOS (edit mode) |
|---|---|---|
| Sort all songs by title | sort icon action (as today) | in the More menu |
| Add songs from another set list… | overflow | More menu |
| Manage set lists… | overflow | More menu |

- Android: overflow items are only visible while editing on the Songs page,
  exactly as `sortMenuItem` is today; Settings stays where it is.
- iOS: left bar = Done (checkmark); right bar = Add (plus) and More
  (`ellipsis.circle`, `UIMenu`). Outside edit mode the bar is unchanged (Edit
  left, Settings right).
- "Add songs from another set list…" is disabled when no other list has a
  song that the current list lacks, and says why: Android retitles it
  "Nothing to add from other set lists"; iOS keeps the title and adds the
  subtitle "Nothing to add".

### New / Rename

A name prompt (Android: `DialogFragment` with a Material text field and inline
error; iOS: `UIAlertController` with a text field, the confirm action disabled
and the message line carrying the error while the name is invalid). Titles
"New set list" / "Rename set list", confirm "Create" / "Rename", cancel. The
new-list prompt shows the hint "For example “Saturday show”" until the name
needs correcting. Rename is prefilled with the display name. Creating switches
the Songs tab to the new (empty) list and leaves edit mode.

### Duplicate

No prompt. Creates the copy, switches to it, stays in edit mode (the natural
next step is to prune it), and announces "Duplicated as <name>" for
accessibility (Android: a Snackbar; iOS: `UIAccessibility.post(.announcement)`).

### Delete

Confirmation: title "Delete “<name>”?", message "This removes the set list and
its N songs. My Songs is not affected." (N=0: "This set list is empty."; N=1:
"its 1 song"). Confirm "Delete" (destructive), cancel. After deleting, the tab
shows My Songs and leaves edit mode.

### Add songs from another set list

A picker screen (Android: an Activity; iOS: a modal `UITableViewController` in
a navigation controller, sheet on iPhone, popover/form sheet on iPad) titled
"Add songs":

- One section per **other** list that has at least one addable song, in list
  order, headed by the list's display name in the section-header style.
- A row per song: title (condensed 20sp) and key readout (mono 18sp), a
  checkmark accessory when selected. Tapping toggles. Songs whose title and key
  already exist in the current list (title compared case-insensitively) are
  **omitted**.
- The confirming action reads "Add" and is disabled until something is
  selected; "Add 3 songs" once selected (singular "Add 1 song"). Android: a
  full-width bottom button, plus a checkmark app-bar action; iOS: the right bar
  item. Cancel/Close on the left.
- On confirm: deep copies appended to the current list, screen closes, the
  Songs tab stays in edit mode.

### Manage set lists

A screen titled "Set Lists" (Android: Activity; iOS: pushed onto the Songs
navigation stack). Rows: list display name (condensed 20sp) with the song count
(mono 14sp, secondary: "12 songs", "1 song", "No songs"). Hairline dividers,
transparent rows over the score, as every other list.

- My Songs is the first row, has no drag handle, cannot be deleted. The
  current list's row carries the selector's lit dot at its leading edge.
- Custom rows have a trailing drag handle; dropping persists the new order
  once (`order` rewritten for every custom list).
- Tap a row → it becomes the current list and the screen returns to Songs,
  the same tap-to-switch as everywhere else.
- Each row has a trailing menu button (⋮ on Android, `ellipsis.circle` on
  iOS) titled with the list's name: Rename / Duplicate / Delete (with the
  same confirmation; Delete omitted for My Songs). iOS also keeps swipe
  actions. Row controls are 48dp.
- A "+" (Android FAB, iOS bar button) opens New set list.
- Live: remote changes re-render the rows.

### Settings

Android's "Clear Song List" becomes **"Clear all songs"** and empties My Songs
and deletes every other set list, after a confirmation that says so: "This
empties My Songs and deletes your other set lists on every synced device."
iOS Settings has never had a clear control; the model's `clearAll()` exists on
both platforms for when it grows one.

## Sync behaviour

- The list set can change under the UI at any moment (another device). The
  Songs tab re-renders the selector and rows on every change; if the current
  list disappears, the tab falls back to My Songs without leaving the page.
- Reorder is committed on drop, never per step.
- While a Firestore snapshot is being applied, no write goes back (the
  existing restore guard on both platforms), including for `order`.

## Tests

- Unit: name validation, slug ids, ordering rules, duplicate naming and deep
  copy, delete fallback, clear-all, "addable songs" filtering, display-name
  fallback for `"Default"`.
- Screen (Robolectric / hosted XCTest): the selector renders and switches, the
  FAB/Add targets the current list, edit-mode actions appear and act, delete
  falls back to My Songs, empty states.
- Emulator (skips when nothing listens on `localhost:8080` / `:9099`, and on
  iOS when the build is unsigned, since Firebase Auth needs the keychain):
  `scripts/firestore-emulator.sh pitchperfect` starts Firestore + Auth for the
  reserved project `demo-pitchperfect`. Each test creates a fresh user and two
  clients: local edits produce the documented documents; remote create,
  rename, reorder, and delete update the local model.
