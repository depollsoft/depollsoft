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
        let items = try XCTUnwrap(songs.navigationItem.leftBarButtonItems)
        XCTAssertEqual(items.count, 2)
        XCTAssertEqual(items.first?.accessibilityLabel, "Done")
        XCTAssertEqual(items.last?.title, "Sort Alphabetically")
        XCTAssertEqual(songs.navigationItem.rightBarButtonItem?.accessibilityLabel, "Add")
        XCTAssertEqual(songs.navigationItem.rightBarButtonItem?.accessibilityIdentifier, "plus")
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
}
