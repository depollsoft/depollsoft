import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

/// The Songs tab's rules, through its model.
@MainActor
final class SongListModelTests: PitchPerfectTestCase {
    private var model: SongListModel!
    private var store: DPSongsModel { .sharedInstance }

    override func setUp() async throws {
        try await super.setUp()
        model = SongListModel()
    }

    override func tearDown() async throws {
        model = nil
        try await super.tearDown()
    }

    private var names: [String] { store.currentList.songs.map(\.name) }

    func testTheEmptyStateSpeaksForTheListShowing() throws {
        XCTAssertTrue(model.songs.isEmpty)
        XCTAssertEqual(model.emptyText, "NO SONGS ON FILE\n\nTap + to add your first song and its key")
        XCTAssertEqual(model.emptyAccessibilityLabel, "No songs on file. Tap Add to add your first song and its key.")
        let list = try customList(named: "Saturday show")
        model.select(listId: list.id)
        XCTAssertTrue(model.emptyText.hasPrefix("NOTHING IN THIS SET LIST"))
        XCTAssertEqual(model.emptyAccessibilityLabel,
                       "Nothing in this set list. Tap Add to add a song, or tap Edit to add songs from another set list.")
    }

    func testSortingDeletingAndMovingRewriteTheListAndStoreIt() {
        seedSongs(["Shenandoah", "blue Skies", "Down Our Way"])
        model.sort()
        XCTAssertEqual(names, ["blue Skies", "Down Our Way", "Shenandoah"])
        model.moveSongs(from: IndexSet(integer: 2), to: 0)
        XCTAssertEqual(names, ["Shenandoah", "blue Skies", "Down Our Way"])
        model.deleteSongs(at: IndexSet(integer: 1))
        XCTAssertEqual(names, ["Shenandoah", "Down Our Way"])
        let stored = UserDefaults.standard.dictionary(forKey: "depollsoft.pitchperfect.SongLists")?["default"] as? [String: Any]
        XCTAssertEqual((stored?["songs"] as? [Any])?.count, 2, "each edit is stored at once")
    }

    func testAddingASongOpensTheEditorOnANewSongInC() throws {
        model.addSong()
        let request = try XCTUnwrap(model.editor)
        XCTAssertTrue(request.isNew)
        XCTAssertEqual(request.song.key.friendlyName(), "C")
        request.song.name = "Blue Skies"
        request.completion(true)
        XCTAssertEqual(names, ["Blue Skies"])
        XCTAssertEqual(model.pendingScrollTarget, .row(request.song.rowID, anchor: .center))
    }

    func testCancellingAnAddLeavesTheListAlone() throws {
        seedSongs(["Blue Skies"])
        for _ in 0..<3 {
            model.addSong()
            try XCTUnwrap(model.editor).completion(false)
            XCTAssertEqual(names, ["Blue Skies"])
        }
    }

    func testEditingASongStoresItOnSave() throws {
        let song = seedSongs(["Blue Skies"])[0]
        model.editSong(song)
        let request = try XCTUnwrap(model.editor)
        XCTAssertFalse(request.isNew)
        XCTAssertTrue(request.song === song)
        song.name = "Blue Skies (tag)"
        request.completion(true)
        let stored = UserDefaults.standard.dictionary(forKey: "depollsoft.pitchperfect.SongLists")?["default"] as? [String: Any]
        XCTAssertTrue("\(stored ?? [:])".contains("Blue Skies (tag)"))
    }

    func testPressingARowSoundsItsKeyAndLightsOnlyThatRow() {
        let songs = seedSongs(["Blue Skies", "Shenandoah"])
        model.press(songs[0])
        XCTAssertTrue(songs[0].key.note.isPlaying)
        XCTAssertTrue(model.isLit(songs[0]))
        XCTAssertFalse(model.isLit(songs[1]), "the other row shares the key but was not pressed")
        model.release(songs[0])
        XCTAssertFalse(songs[0].key.note.isPlaying)
        XCTAssertFalse(model.isLit(songs[0]))
    }

    func testAToggledRowStaysLitWhileItsNoteSounds() {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let song = seedSongs(["Blue Skies"])[0]
        model.press(song)
        model.release(song)
        XCTAssertTrue(song.key.note.isPlaying)
        XCTAssertTrue(model.isLit(song))
        model.press(song)
        model.release(song)
        XCTAssertFalse(model.isLit(song))
    }

    func testSwitchingListsSilencesTheRowsAndRemembersWhereEachWas() throws {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let songs = seedSongs((0..<5).map { "Song \($0)" })
        let list = try customList(named: "Saturday show")
        model.press(songs[0])
        model.release(songs[0])
        model.rowAppeared(songs[3], at: 3)
        model.rowAppeared(songs[4], at: 4)
        model.select(listId: list.id)
        XCTAssertFalse(songs[0].key.note.isPlaying)
        XCTAssertEqual(model.currentListId, list.id)
        XCTAssertEqual(model.pendingScrollTarget, .top, "a list shown for the first time starts at the top")
        model.pendingScrollTarget = nil
        model.select(listId: DPSongsModel.defaultListId)
        XCTAssertEqual(model.pendingScrollTarget, .row(songs[3].rowID, anchor: .top), "My Songs comes back where it was left")
        model.select(listId: DPSongsModel.defaultListId)
        XCTAssertEqual(model.currentListId, DPSongsModel.defaultListId)
    }

    func testCreatingAListSwitchesToItAndLeavesEditMode() {
        model.edit()
        model.promptNewSetList()
        XCTAssertEqual(model.prompts.kind, .create)
        model.prompts.name = "Saturday show"
        model.prompts.confirmName()
        let created = store.orderedLists.last!
        XCTAssertEqual(store.displayName(for: created), "Saturday show")
        XCTAssertEqual(model.currentListId, created.id)
        XCTAssertFalse(model.isEditing)
    }

    func testDuplicatingSwitchesToTheCopyAndStaysInEditMode() throws {
        seedSongs(["Blue Skies"])
        model.edit()
        model.duplicate(store.defaultSongList)
        let copy = store.currentList
        XCTAssertNotEqual(copy.id, DPSongsModel.defaultListId)
        XCTAssertEqual(copy.songs.map(\.name), ["Blue Skies"])
        XCTAssertTrue(model.isEditing)
    }

    func testDeletingTheListOnScreenReturnsToMySongsAndLeavesEditMode() throws {
        let list = try customList(named: "Saturday show")
        model.select(listId: list.id)
        model.edit()
        model.confirmDelete(list)
        XCTAssertEqual(model.prompts.kind, .delete(list))
        XCTAssertEqual(model.prompts.title, "Delete “Saturday show”?")
        model.prompts.confirmDelete()
        XCTAssertNil(store.songLists[list.id])
        XCTAssertEqual(model.currentListId, DPSongsModel.defaultListId)
        XCTAssertFalse(model.isEditing)
    }

    func testDeletingAnotherListLeavesTheTabAsItWas() throws {
        let list = try customList(named: "Saturday show")
        model.edit()
        model.confirmDelete(list)
        model.prompts.confirmDelete()
        XCTAssertNil(store.songLists[list.id])
        XCTAssertTrue(model.isEditing)
    }

    func testMySongsCannotBeDeleted() {
        model.confirmDelete(store.defaultSongList)
        XCTAssertNil(model.prompts.kind)
    }

    func testAddingFromAnotherListIsOfferedOnlyWhenThereIsSomethingToAdd() throws {
        seedSongs(["Blue Skies"])
        XCTAssertFalse(model.canAddFromAnotherList, "nothing is addable while there is only one set list")
        let list = try customList(named: "Saturday show")
        model.select(listId: list.id)
        XCTAssertTrue(model.canAddFromAnotherList, "My Songs has a song this list lacks")
    }
}

/// Set-list naming and deleting, as the prompts present them.
@MainActor
final class SetListPromptTests: PitchPerfectTestCase {
    func testTheNewListPromptHintsThenExplainsWhyANameCannotBeUsed() {
        let prompts = SetListPromptModel()
        var created: String?
        prompts.create { created = $0 }
        XCTAssertEqual(prompts.title, "New set list")
        XCTAssertEqual(prompts.actionTitle, "Create")
        XCTAssertEqual(prompts.message, "For example “Saturday show”")
        XCTAssertFalse(prompts.canConfirm, "an empty field cannot create a set list")
        prompts.name = "My Songs"
        XCTAssertFalse(prompts.canConfirm)
        XCTAssertEqual(prompts.message, "You already have a set list with that name.")
        prompts.name = "  Saturday   show "
        XCTAssertTrue(prompts.canConfirm)
        XCTAssertEqual(prompts.message, "For example “Saturday show”")
        prompts.confirmName()
        XCTAssertEqual(created, DPSongsModel.normalizeName("  Saturday   show "))
        XCTAssertNil(prompts.kind)
    }

    func testRenamingStartsFromTheNameAndMayKeepIt() throws {
        let list = try customList(named: "Saturday show")
        let prompts = SetListPromptModel()
        var renamed: String?
        prompts.rename(list) { renamed = $0 }
        XCTAssertEqual(prompts.title, "Rename set list")
        XCTAssertEqual(prompts.actionTitle, "Rename")
        XCTAssertEqual(prompts.name, "Saturday show")
        XCTAssertNil(prompts.message)
        XCTAssertTrue(prompts.canConfirm, "a list may keep its own name")
        prompts.name = "Afterglow"
        prompts.confirmName()
        XCTAssertEqual(renamed, "Afterglow")
    }

    func testAnInvalidNameNeverCommits() {
        let prompts = SetListPromptModel()
        var committed = false
        prompts.create { _ in committed = true }
        prompts.name = ""
        prompts.confirmName()
        XCTAssertFalse(committed)
    }

    func testTheDeletePromptCountsTheSongsThatLeave() throws {
        let list = try customList(named: "Saturday show")
        XCTAssertEqual(SetListPromptModel.deleteMessage(songCount: 0), "This set list is empty.")
        XCTAssertEqual(SetListPromptModel.deleteMessage(songCount: 1),
                       "This removes the set list and its 1 song. My Songs is not affected.")
        XCTAssertEqual(SetListPromptModel.deleteMessage(songCount: 3),
                       "This removes the set list and its 3 songs. My Songs is not affected.")
        let prompts = SetListPromptModel()
        var deleted = false
        prompts.delete(list) { deleted = true }
        XCTAssertEqual(prompts.message, "This set list is empty.")
        prompts.confirmDelete()
        XCTAssertTrue(deleted)
    }
}

/// The Songs tab through its real controls.
@MainActor
final class PitchPerfectSongManagementTests: PitchPerfectTestCase {
    private func songsTab() throws -> HostedApp {
        let app = try launch()
        app.show(tab: 3)
        return app
    }

    private func alert(_ app: HostedApp) throws -> UIAlertController {
        settle { app.topPresented is UIAlertController }
        return try XCTUnwrap(app.topPresented as? UIAlertController)
    }

    func testTheEmptyListSaysHowToStartAndRowsSayTheirKey() throws {
        let app = try songsTab()
        XCTAssertEqual(app.ui.label(id: "songs.empty"), "No songs on file. Tap Add to add your first song and its key.")
        seedSongs(["Blue Skies", "Shenandoah"])
        ScreenCatalog.settle(0.2)
        XCTAssertFalse(app.ui.exists(id: "songs.empty"))
        XCTAssertTrue(app.ui.exists(label: "Blue Skies, C"))
        XCTAssertTrue(app.ui.exists(label: "Shenandoah, C"))
    }

    func testEditModeSwapsTheBarForDoneAddAndMore() throws {
        seedSongs(["Blue Skies"])
        let app = try songsTab()
        XCTAssertEqual(app.ui.label(id: "pencil"), "Edit")
        XCTAssertEqual(app.ui.label(id: "gearshape"), "Settings")
        app.ui.tap(id: "pencil")
        XCTAssertTrue(app.songs.isEditing)
        XCTAssertEqual(app.ui.label(id: "checkmark"), "Done")
        XCTAssertEqual(app.ui.label(id: "plus"), "Add")
        XCTAssertEqual(app.ui.label(id: "ellipsis.circle"), "More")
        XCTAssertFalse(app.ui.exists(id: "gearshape"))
        XCTAssertTrue(app.ui.exists(id: "song.edit"), "each row offers its editor")
        app.ui.tap(id: "checkmark")
        XCTAssertFalse(app.songs.isEditing)
        XCTAssertTrue(app.ui.exists(id: "gearshape"))
        XCTAssertFalse(app.ui.exists(id: "song.edit"))
    }

    func testAddingASongThroughTheEditorShowsItInTheList() throws {
        let app = try songsTab()
        app.ui.tap(id: "pencil")
        app.ui.tap(id: "plus")
        settle { app.topPresented !== app.host }
        let sheet = app.sheet
        XCTAssertTrue(sheet.exists(label: "Song title"))
        XCTAssertEqual(sheet.label(id: "xmark"), "Close")
        app.typeSongTitle("Blue Skies")
        sheet.tap(label: SongEditorSpeech.name(for: (DPKey.majorKeys() as! [DPKey])[7], minor: false))
        sheet.tap(id: "checkmark")
        settle { app.topPresented === app.host }
        XCTAssertEqual(DPSongsModel.sharedInstance.defaultSongList.songs.map(\.name), ["Blue Skies"])
        XCTAssertEqual(DPSongsModel.sharedInstance.defaultSongList.songs.first?.key.friendlyName(), "G")
        XCTAssertTrue(app.ui.exists(label: "Blue Skies, G"))
    }

    func testTheEditorRefusesABlankTitleAndCancelChangesNothing() throws {
        seedSongs(["Blue Skies"])
        let app = try songsTab()
        app.ui.tap(id: "pencil")
        for _ in 0..<2 {
            app.ui.tap(id: "plus")
            settle { app.topPresented !== app.host }
            app.sheet.tap(id: "checkmark")
            XCTAssertTrue(app.sheet.exists(id: "songTitleError"), "a blank title says it is required")
            XCTAssertTrue(app.topPresented !== app.host, "and the editor stays open")
            app.sheet.tap(id: "xmark")
            settle { app.topPresented === app.host }
            XCTAssertEqual(DPSongsModel.sharedInstance.defaultSongList.songs.map(\.name), ["Blue Skies"])
        }
    }

    func testTheDisclosureButtonEditsThatSong() throws {
        seedSongs(["Blue Skies"])
        let app = try songsTab()
        app.ui.tap(id: "pencil")
        app.ui.tap(id: "song.edit")
        settle { app.topPresented !== app.host }
        XCTAssertEqual(app.descendants(of: UITextField.self, in: app.topPresented.view).first?.text, "Blue Skies")
        XCTAssertTrue(app.sheet.exists(label: "Edit Song") || app.navigationTitles.contains("Edit Song"))
    }

    func testTheSelectorShowsEachListAndANewPosition() throws {
        seedSongs(["Blue Skies"])
        let list = try customList(named: "Saturday show")
        let app = try songsTab()
        XCTAssertEqual(app.ui.label(id: "setlist.default"), "My Songs, 1 song")
        XCTAssertTrue(app.ui.isSelected(id: "setlist.default"))
        XCTAssertEqual(app.ui.label(id: "setlist.\(list.id)"), "Saturday show, no songs")
        XCTAssertFalse(app.ui.isSelected(id: "setlist.\(list.id)"))
        XCTAssertEqual(app.ui.label(id: "setlist.new"), "New set list")
        app.ui.tap(id: "setlist.\(list.id)")
        XCTAssertEqual(DPSongsModel.sharedInstance.currentListId, list.id)
        XCTAssertTrue(app.ui.isSelected(id: "setlist.\(list.id)"))
        XCTAssertTrue(app.ui.label(id: "songs.empty")?.hasPrefix("Nothing in this set list") == true)
        app.ui.tap(id: "setlist.default")
        XCTAssertTrue(app.ui.exists(label: "Blue Skies, C"))
    }

    func testThePlusNamesAListLiveAndSwitchesToIt() throws {
        seedSongs(["Blue Skies"])
        let app = try songsTab()
        app.ui.tap(id: "setlist.new")
        let prompt = try alert(app)
        XCTAssertEqual(prompt.title, "New set list")
        XCTAssertEqual(prompt.message, "For example “Saturday show”")
        XCTAssertEqual(prompt.actions.map { $0.title ?? "" }, ["Cancel", "Create"])
        XCTAssertEqual(prompt.textFields?.first?.accessibilityIdentifier, "setlist.name.field")
        let create = try XCTUnwrap(prompt.actions.last)
        XCTAssertFalse(create.isEnabled, "an empty field cannot create a set list")
        prompt.pp_type("My Songs")
        XCTAssertFalse(create.isEnabled)
        XCTAssertEqual(prompt.message, "You already have a set list with that name.")
        prompt.pp_type("Saturday show")
        XCTAssertTrue(create.isEnabled)
        XCTAssertEqual(prompt.message, "For example “Saturday show”")
        prompt.pp_fire("Create")
        settle { app.topPresented === app.host }
        let created = try XCTUnwrap(DPSongsModel.sharedInstance.songLists.values.first { $0.name == "Saturday show" })
        XCTAssertEqual(DPSongsModel.sharedInstance.currentListId, created.id)
        XCTAssertTrue(app.ui.isSelected(id: "setlist.\(created.id)"))
    }

    func testDeletingAListConfirmsFirst() throws {
        seedSongs(["Blue Skies"])
        let list = try customList(named: "Saturday show")
        DPSongsModel.sharedInstance.copySongs(DPSongsModel.sharedInstance.defaultSongList.songs, to: list)
        let app = try songsTab()
        app.songs.confirmDelete(list)
        let confirmation = try alert(app)
        XCTAssertEqual(confirmation.title, "Delete “Saturday show”?")
        XCTAssertEqual(confirmation.message, "This removes the set list and its 1 song. My Songs is not affected.")
        XCTAssertEqual(confirmation.actions.last?.style, .destructive)
        confirmation.pp_fire("Delete")
        settle { app.topPresented === app.host }
        XCTAssertNil(DPSongsModel.sharedInstance.songLists[list.id])
        XCTAssertFalse(app.ui.exists(id: "setlist.\(list.id)"))
    }

    func testAddingSongsFromAnotherListAppendsCopies() throws {
        seedSongs(["Blue Skies", "Shenandoah"])
        let list = try customList(named: "Saturday show")
        DPSongsModel.sharedInstance.currentListId = list.id
        let app = try songsTab()
        app.songs.addSongsFromAnotherList()
        settle { app.topPresented !== app.host }
        let sheet = app.sheet
        XCTAssertEqual(sheet.label(id: "xmark"), "Close")
        XCTAssertEqual(sheet.label(id: "setlist.addSongs.confirm"), "Add")
        XCTAssertFalse(sheet.isEnabled(id: "setlist.addSongs.confirm"))
        XCTAssertTrue(sheet.exists(label: "My Songs"), "one engraved section per source list")
        sheet.tap(label: "Blue Skies, C")
        XCTAssertEqual(sheet.label(id: "setlist.addSongs.confirm"), "Add 1 song")
        sheet.tap(label: "Shenandoah, C")
        XCTAssertEqual(sheet.label(id: "setlist.addSongs.confirm"), "Add 2 songs")
        XCTAssertEqual(sheet.label(id: "setlist.addSongs.selectAll"), "Clear")
        sheet.tap(id: "setlist.addSongs.selectAll")
        XCTAssertEqual(sheet.label(id: "setlist.addSongs.confirm"), "Add")
        XCTAssertEqual(sheet.label(id: "setlist.addSongs.selectAll"), "Select all")
        sheet.tap(id: "setlist.addSongs.selectAll")
        sheet.tap(id: "setlist.addSongs.confirm")
        settle { app.topPresented === app.host }
        XCTAssertEqual(list.songs.map(\.name), ["Blue Skies", "Shenandoah"])
        XCTAssertTrue(Set(list.songs.map(\.id)).isDisjoint(with: Set(DPSongsModel.sharedInstance.defaultSongList.songs.map(\.id))),
                      "the picker appends copies, not the same songs")
        XCTAssertTrue(app.ui.exists(label: "Blue Skies, C"))
    }

    func testTheManageScreenListsEveryListAndSwitchesOnATap() throws {
        seedSongs(["Blue Skies", "Shenandoah"])
        let first = try customList(named: "Saturday show")
        let second = try customList(named: "Afterglow")
        let app = try songsTab()
        app.songs.manageSetLists()
        settle { app.navigationTitles.contains("Set Lists") }
        let ui = app.ui
        XCTAssertEqual(ui.label(id: "setlist.row.default.current"), "My Songs, 2 songs, current")
        XCTAssertTrue(ui.traits(id: "setlist.row.default.current").contains(.selected))
        XCTAssertEqual(ui.label(id: "setlist.row.\(first.id)"), "Saturday show, no songs")
        XCTAssertEqual(ui.label(id: "setlist.row.menu.\(first.id)"), "Actions for Saturday show")
        XCTAssertEqual(ui.element(id: "setlist.row.default.current")?.accessibilityCustomActions?.map(\.name) ?? [], [],
                       "My Songs never moves")
        XCTAssertEqual(ui.element(id: "setlist.row.\(first.id)")?.accessibilityCustomActions?.map(\.name), ["Move down"])
        XCTAssertEqual(ui.element(id: "setlist.row.\(second.id)")?.accessibilityCustomActions?.map(\.name), ["Move up"])
        ui.perform(action: "Move up", id: "setlist.row.\(second.id)")
        XCTAssertEqual(DPSongsModel.sharedInstance.orderedLists.map(\.id), ["default", second.id, first.id])
        ui.tap(id: "setlist.row.\(first.id)")
        XCTAssertEqual(DPSongsModel.sharedInstance.currentListId, first.id)
        settle { !app.navigationTitles.contains("Set Lists") }
    }
}

/// The Set Lists screen's rules, through its model.
@MainActor
final class SetListsModelTests: PitchPerfectTestCase {
    func testCountsReadNaturally() {
        XCTAssertEqual(SetListsModel.countLabel(0), "No songs")
        XCTAssertEqual(SetListsModel.countLabel(1), "1 song")
        XCTAssertEqual(SetListsModel.countLabel(12), "12 songs")
    }

    func testReorderingNeverMovesMySongs() throws {
        let first = try customList(named: "Saturday show")
        let second = try customList(named: "Afterglow")
        let model = SetListsModel()
        XCTAssertTrue(model.move(second, by: -1))
        XCTAssertEqual(DPSongsModel.sharedInstance.orderedLists.map(\.id), ["default", second.id, first.id])
        XCTAssertFalse(model.move(second, by: -1), "nothing above the first custom row")
        XCTAssertFalse(model.move(DPSongsModel.sharedInstance.defaultSongList, by: 1))
        // A drag onto My Songs' place lands just below it.
        model.move(from: IndexSet(integer: 2), to: 0)
        XCTAssertEqual(DPSongsModel.sharedInstance.orderedLists.map(\.id), ["default", first.id, second.id])
        XCTAssertEqual(first.order, 0)
        XCTAssertEqual(second.order, 1)
    }

    func testMoveActionsOfferOnlyTheDirectionsThatExist() throws {
        let first = try customList(named: "Saturday show")
        let middle = try customList(named: "Afterglow")
        let last = try customList(named: "Encore")
        let model = SetListsModel()
        XCTAssertEqual(model.moveActionNames(DPSongsModel.sharedInstance.defaultSongList), [])
        XCTAssertEqual(model.moveActionNames(first), ["Move down"])
        XCTAssertEqual(model.moveActionNames(middle), ["Move up", "Move down"])
        XCTAssertEqual(model.moveActionNames(last), ["Move up"])
    }

    func testCreatingFromTheManageScreenMakesTheNewListCurrent() {
        let model = SetListsModel()
        model.promptCreate()
        model.prompts.name = "Encore"
        model.prompts.confirmName()
        XCTAssertEqual(DPSongsModel.sharedInstance.displayName(for: DPSongsModel.sharedInstance.currentList), "Encore")
    }
}

/// The Add songs picker's rules, through its model.
@MainActor
final class AddSongsModelTests: PitchPerfectTestCase {
    func testTheConfirmTitleCountsAndSelectAllToggles() throws {
        seedSongs(["Blue Skies", "Shenandoah"])
        let list = try customList(named: "Saturday show")
        let model = AddSongsModel(target: list)
        XCTAssertEqual(model.groups.count, 1)
        XCTAssertEqual(model.confirmTitle, "Add")
        XCTAssertFalse(model.canConfirm)
        model.toggle(model.groups[0].songs[0])
        XCTAssertEqual(model.confirmTitle, "Add 1 song")
        XCTAssertEqual(model.selectAllTitle, "Select all")
        model.toggleSelectAll()
        XCTAssertEqual(model.confirmTitle, "Add 2 songs")
        XCTAssertEqual(model.selectAllTitle, "Clear")
        model.toggleSelectAll()
        XCTAssertEqual(model.confirmTitle, "Add")
        XCTAssertEqual(model.confirm(), 0, "nothing chosen adds nothing")
    }

    func testNothingToOfferDisablesSelectAll() throws {
        let list = try customList(named: "Saturday show")
        let model = AddSongsModel(target: list)
        XCTAssertFalse(model.canSelectAll)
        model.toggleSelectAll()
        XCTAssertFalse(model.canConfirm)
    }
}
