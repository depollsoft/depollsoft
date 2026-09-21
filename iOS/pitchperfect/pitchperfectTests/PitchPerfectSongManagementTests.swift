import UIKit
import XCTest
@testable import pitchperfect

@MainActor
final class PitchPerfectSongManagementTests: PitchPerfectControllerTestCase {
    private func enterEditing(_ songs: DPSongListViewController) throws {
        XCTAssertEqual(songs.navigationItem.leftBarButtonItem?.accessibilityLabel, "Edit")
        XCTAssertEqual(songs.navigationItem.rightBarButtonItem?.accessibilityLabel, "Settings")
        try press(songs.navigationItem.leftBarButtonItem)
        XCTAssertTrue(try table(in: songs).isEditing)
        // Edit mode: Done alone on the left; Add and the set-list menu on the right.
        let left = try XCTUnwrap(songs.navigationItem.leftBarButtonItems)
        XCTAssertEqual(left.count, 1)
        XCTAssertEqual(left.first?.accessibilityLabel, "Done")
        let right = try XCTUnwrap(songs.navigationItem.rightBarButtonItems)
        XCTAssertEqual(right.count, 2)
        XCTAssertEqual(right.first?.accessibilityLabel, "Add")
        XCTAssertEqual(right.first?.accessibilityIdentifier, "plus")
        XCTAssertEqual(right.last?.accessibilityLabel, "More")
        XCTAssertEqual(right.last?.accessibilityIdentifier, "ellipsis.circle")
        XCTAssertNotNil(right.last?.menu)
    }

    private func openEditor(_ songs: DPSongListViewController) throws -> DPSongEditorViewController {
        try press(songs.navigationItem.rightBarButtonItem)
        settle { songs.presentedViewController != nil }
        let navigation = try XCTUnwrap(songs.presentedViewController as? UINavigationController)
        let editor = try XCTUnwrap(navigation.topViewController as? DPSongEditorViewController)
        editor.loadViewIfNeeded()
        editor.view.layoutIfNeeded()
        XCTAssertEqual(editor.navigationItem.title, "Add Song")
        XCTAssertEqual(editor.navigationItem.leftBarButtonItem?.accessibilityLabel, "Close")
        XCTAssertEqual(editor.navigationItem.rightBarButtonItem?.accessibilityLabel, "Done")
        // Inspect the real text field hosted by SwiftUI, not a replacement form.
        settle { !self.descendants(of: UITextField.self, in: editor.view).isEmpty }
        XCTAssertNotNil(editor.view.window)
        return editor
    }

    func testSongListShowsEmptyStateAndSeededRowContents() throws {
        try withApp { tabs in
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            let table = try table(in: songs)
            XCTAssertEqual(table.numberOfRows(inSection: 0), 0)
            let empty = try XCTUnwrap(songs.value(forKey: "emptyStateLabel") as? UILabel)
            XCTAssertFalse(empty.isHidden)
            XCTAssertEqual(empty.accessibilityLabel, "No songs on file. Tap Add to add your first song and its key.")
            seedSongs(["Blue Skies", "Shenandoah"])
            XCTAssertEqual(table.numberOfRows(inSection: 0), 2)
            XCTAssertTrue(empty.isHidden)
            for (row, title) in ["Blue Skies", "Shenandoah"].enumerated() {
                let songCell = try cell(row, in: table)
                XCTAssertTrue(labels(in: songCell).contains(title))
                XCTAssertTrue(labels(in: songCell).contains("C"))
                XCTAssertEqual(songCell.accessibilityLabel, "\(title), C")
            }
        }
    }

    func testNavigationEditingActionsRestoreAfterDone() throws {
        try withApp { tabs in
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            XCTAssertFalse(songs.navigationController!.navigationBar.isHidden)
            try enterEditing(songs)
            try press(songs.navigationItem.leftBarButtonItems?.first)
            XCTAssertFalse(try table(in: songs).isEditing)
            XCTAssertEqual(songs.navigationItem.leftBarButtonItem?.accessibilityLabel, "Edit")
            XCTAssertEqual(songs.navigationItem.rightBarButtonItem?.accessibilityLabel, "Settings")
        }
    }

    func testRepeatedAddAndCancelReturnsToUnchangedSongList() throws {
        try withApp { tabs in
            seedSongs(["Blue Skies"])
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            try enterEditing(songs)
            for _ in 0..<3 {
                let editor = try openEditor(songs)
                XCTAssertEqual(descendants(of: UITextField.self, in: editor.view).first?.text, "")
                try press(editor.navigationItem.leftBarButtonItem)
                settle { songs.presentedViewController == nil }
                let table = try table(in: songs)
                XCTAssertEqual(table.numberOfRows(inSection: 0), 1)
                XCTAssertTrue(labels(in: try cell(0, in: table)).contains("Blue Skies"))
                XCTAssertEqual(DPSongsModel.sharedInstance.defaultSongList.songs.map(\.name), ["Blue Skies"])
                XCTAssertEqual(songs.navigationItem.rightBarButtonItem?.accessibilityLabel, "Add")
            }
        }
    }

    func testSavingAnAddedSongUpdatesTheVisibleList() throws {
        try withApp { tabs in
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            try enterEditing(songs)
            let editor = try openEditor(songs)
            let field = try XCTUnwrap(descendants(of: UITextField.self, in: editor.view).first)
            field.text = "Blue Skies"
            field.sendActions(for: .editingChanged)
            let state = try XCTUnwrap(editor.value(forKey: "editor") as? DPSongEditor)
            settle { state.title == "Blue Skies" }
            try press(editor.navigationItem.rightBarButtonItem)
            settle { songs.presentedViewController == nil }
            let table = try table(in: songs)
            XCTAssertEqual(table.numberOfRows(inSection: 0), 1)
            XCTAssertTrue(labels(in: try cell(0, in: table)).contains("Blue Skies"))
            XCTAssertEqual(DPSongsModel.sharedInstance.defaultSongList.songs.map(\.name), ["Blue Skies"])
        }
    }

    func testPressingASongPlaysItsKeyAndHighlightsItsRow() throws {
        try withApp { tabs in
            seedSongs(["Blue Skies"])
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            let songCell = try cell(0, in: table(in: songs))
            let song = try XCTUnwrap(DPSongsModel.sharedInstance.defaultSongList.songs.first)
            assertPressAndRelease(songCell, note: song.key.note)
        }
    }

    func testSongListScrollsToBothEndsWithoutLosingContents() throws {
        try withApp { tabs in
            let titles = (0..<40).map { "Song \($0)" }
            seedSongs(titles)
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            let table = try table(in: songs)
            for row in [39, 0] {
                let path = IndexPath(row: row, section: 0)
                table.scrollToRow(at: path, at: .middle, animated: false)
                table.layoutIfNeeded()
                let visibleCell = try XCTUnwrap(table.cellForRow(at: path))
                XCTAssertTrue(labels(in: visibleCell).contains(titles[row]))
                XCTAssertEqual(table.numberOfRows(inSection: 0), 40)
            }
        }
    }

    // MARK: - Set lists

    private func selector(in songs: DPSongListViewController) throws -> SetListSelectorView {
        let view = try XCTUnwrap(songs.value(forKey: "setListSelector") as? SetListSelectorView)
        view.layoutIfNeeded()
        return view
    }

    private func positions(in songs: DPSongListViewController) throws -> [UIButton] {
        try selector(in: songs).positionButtons
    }

    private func position(_ identifier: String, in songs: DPSongListViewController) throws -> UIButton {
        try XCTUnwrap(try positions(in: songs).first { $0.accessibilityIdentifier == identifier })
    }

    private func alert(from songs: DPSongListViewController) throws -> UIAlertController {
        settle { songs.presentedViewController != nil }
        return try XCTUnwrap(songs.presentedViewController as? UIAlertController)
    }

    private func perform(_ selectorName: String, on songs: DPSongListViewController) {
        songs.perform(NSSelectorFromString(selectorName))
    }

    private func customList(named name: String) throws -> DPSongList {
        try XCTUnwrap(DPSongsModel.sharedInstance.createList(named: name))
    }

    private func menuTitles(_ songs: DPSongListViewController) throws -> [String] {
        let more = try XCTUnwrap(songs.navigationItem.rightBarButtonItems?.last)
        let menu = try XCTUnwrap(more.menu)
        return menu.children.compactMap { ($0 as? UIAction)?.title }
    }

    func testTheSelectorShowsMySongsAndANewSetListPosition() throws {
        try withApp { tabs in
            seedSongs(["Blue Skies"])
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            let buttons = try positions(in: songs)
            XCTAssertEqual(buttons.map(\.accessibilityIdentifier), ["setlist.default", "setlist.new"])
            XCTAssertEqual(buttons.first?.accessibilityLabel, "My Songs, 1 song")
            XCTAssertTrue(buttons.first?.accessibilityTraits.contains(.selected) == true)
            XCTAssertEqual(buttons.last?.accessibilityLabel, "New set list")
        }
    }

    func testPressingPlusNamesAndCreatesASetListAndSwitchesToIt() throws {
        try withApp { tabs in
            seedSongs(["Blue Skies"])
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            try position("setlist.new", in: songs).sendActions(for: .touchUpInside)

            let prompt = try alert(from: songs)
            XCTAssertEqual(prompt.title, "New set list")
            XCTAssertEqual(prompt.message, "For example “Saturday show”")
            XCTAssertEqual(prompt.actions.map { $0.title ?? "" }, ["Cancel", "Create"])
            let create = try XCTUnwrap(prompt.actions.last)
            XCTAssertFalse(create.isEnabled, "an empty field cannot create a set list")

            prompt.pp_type("My Songs")
            XCTAssertFalse(create.isEnabled)
            XCTAssertEqual(prompt.message, "You already have a set list with that name.")

            prompt.pp_type("Saturday show")
            XCTAssertTrue(create.isEnabled)
            prompt.pp_fire("Create")
            songs.dismiss(animated: false)
            settle { songs.presentedViewController == nil }

            let model = DPSongsModel.sharedInstance
            let created = try XCTUnwrap(model.songLists.values.first { $0.name == "Saturday show" })
            XCTAssertEqual(model.currentListId, created.id)
            XCTAssertEqual(try positions(in: songs).map(\.accessibilityIdentifier),
                           ["setlist.default", "setlist.\(created.id)", "setlist.new"])
            XCTAssertFalse(try table(in: songs).isEditing, "creating leaves edit mode")
            XCTAssertEqual(try table(in: songs).numberOfRows(inSection: 0), 0)
        }
    }

    func testTappingAPositionSwapsTheSongRowsAndTheEmptyState() throws {
        try withApp { tabs in
            seedSongs(["Blue Skies", "Shenandoah"])
            let list = try customList(named: "Saturday show")
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            XCTAssertEqual(try table(in: songs).numberOfRows(inSection: 0), 2)

            try position("setlist.\(list.id)", in: songs).sendActions(for: .touchUpInside)
            XCTAssertEqual(DPSongsModel.sharedInstance.currentListId, list.id)
            XCTAssertEqual(try table(in: songs).numberOfRows(inSection: 0), 0)
            let empty = try XCTUnwrap(songs.value(forKey: "emptyStateLabel") as? UILabel)
            XCTAssertFalse(empty.isHidden)
            XCTAssertEqual(empty.text?.hasPrefix("NOTHING IN THIS SET LIST"), true)

            try position("setlist.default", in: songs).sendActions(for: .touchUpInside)
            XCTAssertEqual(try table(in: songs).numberOfRows(inSection: 0), 2)
            XCTAssertEqual(empty.text?.hasPrefix("NO SONGS ON FILE"), true)
        }
    }

    func testTheMoreMenuKeepsOnlyTheSongActionsAndSaysWhenNothingIsAddable() throws {
        try withApp { tabs in
            seedSongs(["Blue Skies"])
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            try enterEditing(songs)
            XCTAssertEqual(try menuTitles(songs),
                           ["Sort Alphabetically", "Add songs from another set list…",
                            "Manage set lists…"],
                           "renaming, duplicating and deleting belong to the list itself")
            let more = try XCTUnwrap(songs.navigationItem.rightBarButtonItems?.last)
            let children = try XCTUnwrap(more.menu?.children.compactMap { $0 as? UIAction })
            XCTAssertEqual(children.filter { $0.image != nil }.count, 3, "every action is illustrated")
            let addFrom = try XCTUnwrap(children.first { $0.title.hasPrefix("Add songs") })
            XCTAssertTrue(addFrom.attributes.contains(.disabled),
                          "nothing is addable while there is only one set list")
            XCTAssertEqual(addFrom.subtitle, "Nothing to add", "disabled alone would read as a fault")

            let list = try customList(named: "Saturday show")
            DPSongsModel.sharedInstance.currentListId = list.id
            XCTAssertEqual(try menuTitles(songs),
                           ["Sort Alphabetically", "Add songs from another set list…",
                            "Manage set lists…"],
                           "the menu does not grow for a deletable list")
            let addNow = try XCTUnwrap(
                songs.navigationItem.rightBarButtonItems?.last?.menu?.children
                    .compactMap { $0 as? UIAction }
                    .first { $0.title.hasPrefix("Add songs") })
            XCTAssertFalse(addNow.attributes.contains(.disabled),
                           "My Songs now has a song this list lacks")
            XCTAssertNil(addNow.subtitle)
        }
    }

    func testLongPressingAPositionOffersTheListsOwnActions() throws {
        try withApp { tabs in
            seedSongs(["Blue Skies"])
            let list = try customList(named: "Saturday show")
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)

            let home = try position("setlist.default", in: songs)
            XCTAssertFalse(home.showsMenuAsPrimaryAction, "a tap still switches; the menu is the long press")
            let homeMenu = try XCTUnwrap(home.menu)
            XCTAssertEqual(homeMenu.title, "My Songs", "the menu names the list it acts on")
            XCTAssertEqual(homeMenu.children.compactMap { ($0 as? UIAction)?.title },
                           ["Rename set list…", "Duplicate set list", "Manage set lists…"],
                           "My Songs cannot be deleted")
            XCTAssertEqual(homeMenu.children.compactMap { ($0 as? UIAction)?.image }.count, 3,
                           "every action is illustrated")

            let custom = try position("setlist.\(list.id)", in: songs)
            let customMenu = try XCTUnwrap(custom.menu)
            XCTAssertEqual(customMenu.title, "Saturday show")
            XCTAssertEqual(customMenu.children.compactMap { ($0 as? UIAction)?.title },
                           ["Rename set list…", "Duplicate set list", "Delete set list…", "Manage set lists…"])
            let deleteAction = try XCTUnwrap(customMenu.children[2] as? UIAction)
            XCTAssertTrue(deleteAction.attributes.contains(.destructive))
            XCTAssertNotNil(deleteAction.image)
            XCTAssertNil(try position("setlist.new", in: songs).menu, "the + has no list to act on")

            // Deleting a list that is not on screen leaves the tab where it was.
            XCTAssertEqual(DPSongsModel.sharedInstance.currentListId, "default")
            songs.perform(NSSelectorFromString("confirmDeleteSetList:"), with: list)
            let confirmation = try alert(from: songs)
            XCTAssertEqual(confirmation.title, "Delete “Saturday show”?")
            confirmation.pp_fire("Delete")
            songs.dismiss(animated: false)
            settle { songs.presentedViewController == nil }
            XCTAssertNil(DPSongsModel.sharedInstance.songLists[list.id])
            XCTAssertEqual(DPSongsModel.sharedInstance.currentListId, "default")
            XCTAssertEqual(try positions(in: songs).map(\.accessibilityIdentifier),
                           ["setlist.default", "setlist.new"])
        }
    }

    func testEachListKeepsItsOwnScrollPosition() throws {
        try withApp { tabs in
            seedSongs((0..<40).map { "Song \($0)" })
            let model = DPSongsModel.sharedInstance
            let list = try customList(named: "Saturday show")
            model.copySongs(model.defaultSongList.songs, to: list)
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            let table = try table(in: songs)
            let top = -table.adjustedContentInset.top

            table.scrollToRow(at: IndexPath(row: 39, section: 0), at: .bottom, animated: false)
            table.layoutIfNeeded()
            let scrolled = table.contentOffset.y
            XCTAssertGreaterThan(scrolled, top + 100, "My Songs is scrolled down")

            try position("setlist.\(list.id)", in: songs).sendActions(for: .touchUpInside)
            table.layoutIfNeeded()
            XCTAssertEqual(table.contentOffset.y, top, accuracy: 1,
                           "a list shown for the first time starts at the top")

            try position("setlist.default", in: songs).sendActions(for: .touchUpInside)
            table.layoutIfNeeded()
            XCTAssertEqual(table.contentOffset.y, scrolled, accuracy: 1,
                           "My Songs comes back where it was left")
        }
    }

    func testDeletingTheCurrentSetListConfirmsAndReturnsToMySongs() throws {
        try withApp { tabs in
            seedSongs(["Blue Skies"])
            let list = try customList(named: "Saturday show")
            let model = DPSongsModel.sharedInstance
            model.copySongs(model.defaultSongList.songs, to: list)
            model.currentListId = list.id
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            try enterEditing(songs)

            perform("confirmDeleteSetList", on: songs)
            let confirmation = try alert(from: songs)
            XCTAssertEqual(confirmation.title, "Delete “Saturday show”?")
            XCTAssertEqual(confirmation.message,
                           "This removes the set list and its 1 song. My Songs is not affected.")
            XCTAssertEqual(confirmation.actions.last?.style, .destructive)
            confirmation.pp_fire("Delete")
            songs.dismiss(animated: false)
            settle { songs.presentedViewController == nil }

            XCTAssertNil(model.songLists[list.id])
            XCTAssertEqual(model.currentListId, "default")
            XCTAssertEqual(try positions(in: songs).map(\.accessibilityIdentifier),
                           ["setlist.default", "setlist.new"])
            XCTAssertEqual(try table(in: songs).numberOfRows(inSection: 0), 1)
            XCTAssertFalse(try table(in: songs).isEditing, "deleting leaves edit mode")
        }
    }

    func testAddingSongsFromAnotherSetListAppendsCopies() throws {
        try withApp { tabs in
            seedSongs(["Blue Skies", "Shenandoah"])
            let model = DPSongsModel.sharedInstance
            let list = try customList(named: "Saturday show")
            model.currentListId = list.id
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            try enterEditing(songs)

            perform("addSongsFromAnotherSetList", on: songs)
            settle { songs.presentedViewController != nil }
            let navigation = try XCTUnwrap(songs.presentedViewController as? UINavigationController)
            let picker = try XCTUnwrap(navigation.topViewController as? AddSongsFromListController)
            picker.loadViewIfNeeded()
            let pickerTable = try XCTUnwrap(picker.value(forKey: "tableView") as? UITableView)
            pickerTable.reloadData()

            XCTAssertEqual(picker.navigationItem.title, "Add songs")
            XCTAssertEqual(picker.navigationItem.leftBarButtonItem?.accessibilityLabel, "Close")
            let confirm = try XCTUnwrap(picker.navigationItem.rightBarButtonItem)
            XCTAssertEqual(confirm.title, "Add")
            XCTAssertFalse(confirm.isEnabled)

            let expected = model.addableSongs(for: list)
            XCTAssertEqual(expected.count, 1)
            XCTAssertEqual(pickerTable.numberOfSections, 1)
            XCTAssertEqual(pickerTable.numberOfRows(inSection: 0), 2)
            XCTAssertEqual(picker.tableView(pickerTable, titleForHeaderInSection: 0), "My Songs")
            let firstRow = picker.tableView(pickerTable, cellForRowAt: IndexPath(row: 0, section: 0))
            XCTAssertTrue(labels(in: firstRow).contains("Blue Skies"))

            picker.tableView(pickerTable, didSelectRowAt: IndexPath(row: 0, section: 0))
            XCTAssertEqual(confirm.title, "Add 1 song")
            picker.tableView(pickerTable, didSelectRowAt: IndexPath(row: 1, section: 0))
            XCTAssertEqual(confirm.title, "Add 2 songs")
            XCTAssertTrue(confirm.isEnabled)

            // Select all toggles to Clear once everything is ticked, and back.
            let selectAll = try XCTUnwrap(picker.navigationItem.leftBarButtonItems?.last)
            XCTAssertEqual(selectAll.title, "Clear")
            picker.toggleSelectAll()
            XCTAssertEqual(confirm.title, "Add")
            XCTAssertEqual(selectAll.title, "Select all")
            picker.toggleSelectAll()
            XCTAssertEqual(confirm.title, "Add 2 songs")

            try press(confirm)
            settle { songs.presentedViewController == nil }

            XCTAssertEqual(list.songs.map(\.name), ["Blue Skies", "Shenandoah"])
            XCTAssertTrue(Set(list.songs.map(\.id))
                .isDisjoint(with: Set(model.defaultSongList.songs.map(\.id))),
                          "the picker appends copies, not the same songs")
            XCTAssertEqual(try table(in: songs).numberOfRows(inSection: 0), 2)
        }
    }

    func testTheManageScreenListsEverySetListAndPersistsAReorder() throws {
        try withApp { tabs in
            seedSongs(["Blue Skies", "Shenandoah"])
            let model = DPSongsModel.sharedInstance
            let first = try customList(named: "Saturday show")
            let second = try customList(named: "Afterglow")
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            try enterEditing(songs)

            perform("manageSetLists", on: songs)
            let navigation = try XCTUnwrap(songs.navigationController)
            settle { navigation.topViewController is SetListsController }
            let manage = try XCTUnwrap(navigation.topViewController as? SetListsController)
            manage.loadViewIfNeeded()
            let manageTable = try XCTUnwrap(manage.value(forKey: "tableView") as? UITableView)
            manageTable.reloadData()

            XCTAssertEqual(manage.navigationItem.title, "Set Lists")
            XCTAssertEqual(manageTable.numberOfRows(inSection: 0), 3)
            let home = manage.tableView(manageTable, cellForRowAt: IndexPath(row: 0, section: 0))
            XCTAssertTrue(labels(in: home).contains("My Songs"))
            XCTAssertTrue(labels(in: home).contains("2 songs"))
            let custom = manage.tableView(manageTable, cellForRowAt: IndexPath(row: 1, section: 0))
            XCTAssertTrue(labels(in: custom).contains("Saturday show"))
            XCTAssertTrue(labels(in: custom).contains("No songs"))

            XCTAssertFalse(manage.tableView(manageTable, canMoveRowAt: IndexPath(row: 0, section: 0)),
                           "My Songs never moves")
            XCTAssertTrue(manage.tableView(manageTable, canMoveRowAt: IndexPath(row: 1, section: 0)))
            XCTAssertEqual(manage.tableView(manageTable,
                                            targetIndexPathForMoveFromRowAt: IndexPath(row: 2, section: 0),
                                            toProposedIndexPath: IndexPath(row: 0, section: 0)),
                           IndexPath(row: 1, section: 0))

            manage.tableView(manageTable,
                             moveRowAt: IndexPath(row: 2, section: 0),
                             to: IndexPath(row: 1, section: 0))
            XCTAssertEqual(second.order, 0)
            XCTAssertEqual(first.order, 1)
            XCTAssertEqual(model.orderedLists.map(\.id), ["default", second.id, first.id])
            navigation.popViewController(animated: false)
        }
    }

    func testTheManageScreenSwitchesListsAndCarriesEachRowsActions() throws {
        try withApp { tabs in
            seedSongs(["Blue Skies"])
            let model = DPSongsModel.sharedInstance
            let list = try customList(named: "Saturday show")
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            try enterEditing(songs)

            perform("manageSetLists", on: songs)
            let navigation = try XCTUnwrap(songs.navigationController)
            settle { navigation.topViewController is SetListsController }
            let manage = try XCTUnwrap(navigation.topViewController as? SetListsController)
            manage.loadViewIfNeeded()
            let manageTable = try XCTUnwrap(manage.value(forKey: "tableView") as? UITableView)
            manageTable.reloadData()

            // The current list wears the selector's indicator here too.
            let home = manage.tableView(manageTable, cellForRowAt: IndexPath(row: 0, section: 0))
            XCTAssertEqual(home.accessibilityIdentifier, "setlist.row.default.current")
            XCTAssertTrue(home.accessibilityTraits.contains(.selected))
            XCTAssertEqual(home.accessibilityLabel, "My Songs, 1 song, current")
            let custom = manage.tableView(manageTable, cellForRowAt: IndexPath(row: 1, section: 0))
            XCTAssertEqual(custom.accessibilityIdentifier, "setlist.row.\(list.id)")
            XCTAssertFalse(custom.accessibilityTraits.contains(.selected))

            // Each row carries the list's own actions, headed by its name.
            let rowMenuButton = try XCTUnwrap(descendants(of: UIButton.self, in: custom)
                .first { $0.accessibilityIdentifier == "setlist.row.menu.\(list.id)" })
            XCTAssertTrue(rowMenuButton.showsMenuAsPrimaryAction)
            let rowMenu = try XCTUnwrap(rowMenuButton.menu)
            XCTAssertEqual(rowMenu.title, "Saturday show")
            XCTAssertEqual(rowMenu.children.compactMap { ($0 as? UIAction)?.title },
                           ["Rename set list…", "Duplicate set list", "Delete set list…"])
            XCTAssertTrue(try XCTUnwrap(rowMenu.children.last as? UIAction)
                .attributes.contains(.destructive))
            XCTAssertEqual(manage.menu(forRowAt: IndexPath(row: 0, section: 0))?
                            .children.compactMap { ($0 as? UIAction)?.title },
                           ["Rename set list…", "Duplicate set list"],
                           "My Songs cannot be deleted")

            // Swipe still reaches the same actions, Rename included.
            let swipe = manage.tableView(manageTable,
                                         trailingSwipeActionsConfigurationForRowAt: IndexPath(row: 1, section: 0))
            XCTAssertEqual(swipe?.actions.compactMap(\.title), ["Delete", "Duplicate", "Rename"])
            XCTAssertNil(manage.tableView(manageTable,
                                          trailingSwipeActionsConfigurationForRowAt: IndexPath(row: 0, section: 0)),
                         "My Songs neither moves nor leaves")

            // Tapping a row switches to that list and hands the Songs tab back.
            manage.tableView(manageTable, didSelectRowAt: IndexPath(row: 1, section: 0))
            XCTAssertEqual(model.currentListId, list.id)
            // The pop is animated, so the Songs view is back only once it lands.
            settle { navigation.topViewController === songs && songs.view.window != nil }
            XCTAssertEqual(try table(in: songs).numberOfRows(inSection: 0), 0)
            XCTAssertNil(songs.presentedViewController, "tapping switches; it no longer renames")
        }
    }

    func testTheSelectorSharesItsWholeFrameAndKeepsEveryLabelInPlace() throws {
        try withApp { tabs in
            seedSongs(["Blue Skies"])
            _ = try customList(named: "Saturday show")
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            let machine = try selector(in: songs)
            machine.layoutIfNeeded()
            XCTAssertEqual(machine.bounds.height, 48, accuracy: 0.5)

            let buttons = try positions(in: songs)
            let plus = try XCTUnwrap(buttons.last)
            XCTAssertEqual(plus.bounds.width, 44, accuracy: 0.5, "the + is a control, not a name")
            XCTAssertEqual(plus.convert(plus.bounds, to: machine).maxX, machine.bounds.width,
                           accuracy: 1, "the positions divide the whole frame")
            for position in buttons.dropLast() {
                let label = try XCTUnwrap(descendants(of: UILabel.self, in: position).first)
                XCTAssertEqual(label.frame.minX, 20, accuracy: 0.5,
                               "every label clears the indicator, lit or not")
                XCTAssertGreaterThan(position.bounds.width, label.bounds.width + 20)
            }
        }
    }
}

extension UIAlertController {
    /// Types into the alert's field the way the keyboard would, so the
    /// validation that gates the confirming action actually runs.
    func pp_type(_ text: String, file: StaticString = #filePath, line: UInt = #line) {
        guard let field = textFields?.first else {
            return XCTFail("This alert has no text field", file: file, line: line)
        }
        field.text = text
        field.sendActions(for: .editingChanged)
    }

    /// Runs the handler of the named action, the way tapping it would.
    func pp_fire(_ title: String, file: StaticString = #filePath, line: UInt = #line) {
        guard let action = actions.first(where: { $0.title == title }) else {
            return XCTFail("No “\(title)” action in this alert", file: file, line: line)
        }
        XCTAssertTrue(action.isEnabled, "“\(title)” is disabled", file: file, line: line)
        typealias Handler = @convention(block) (UIAlertAction) -> Void
        guard action.responds(to: Selector(("handler"))), let raw = action.value(forKey: "handler") else {
            return XCTFail("UIAlertAction no longer exposes its handler", file: file, line: line)
        }
        unsafeBitCast(raw as AnyObject, to: Handler.self)(action)
    }
}
