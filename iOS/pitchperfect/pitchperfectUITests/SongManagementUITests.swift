import XCTest

// Keep the native swipe interaction; list content and editor actions run in pitchperfectTests.
final class SongManagementUITests: XCTestCase {
    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launchArguments += ["-telemetry.chosen", "YES", "-telemetry.analytics", "NO", "-telemetry.crashes", "NO"]
        app.launch()
        navigateToSongs()
    }

    override func tearDownWithError() throws {
        app = nil
    }

    private func navigateToSongs() {
        let tabBar = app.tabBars.firstMatch
        if tabBar.waitForExistence(timeout: 5) {
            let songsTab = tabBar.buttons["Songs"]
            if songsTab.exists {
                songsTab.tap()
                Thread.sleep(forTimeInterval: 0.5)
            }
        }
    }

    func testCanSwipeOnSongIfExists() throws {
        // The Songs list is a SwiftUI List: a collection view, not a table.
        let table = app.collectionViews.firstMatch
        // A cold first launch can take a few seconds to lay out the tab.
        guard table.waitForExistence(timeout: 10) else {
            XCTFail("Songs list not found")
            return
        }
        
        guard table.cells.count > 0 else {
            throw XCTSkip("No songs in list to test")
        }
        
        // Swipe left on first song
        let firstCell = table.cells.element(boundBy: 0)
        firstCell.swipeLeft()
        Thread.sleep(forTimeInterval: 0.3)
        
        // Swipe right to dismiss actions
        firstCell.swipeRight()
        Thread.sleep(forTimeInterval: 0.3)
        
        XCTAssertEqual(app.state, .runningForeground, "App should handle swipe gestures")
    }

    /// The real app builds its song store when SwiftUI creates the App, before
    /// didFinishLaunching; the saved songs must still decode on the next launch.
    func testASavedSongSurvivesARelaunch() throws {
        let title = "Relaunch Check \(Int(Date().timeIntervalSince1970) % 100_000)"
        let edit = app.navigationBars.buttons["Edit"]
        XCTAssertTrue(edit.waitForExistence(timeout: 10))
        edit.tap()
        app.navigationBars.buttons["Add"].tap()
        let field = app.textFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText(title)
        let entered = XCTNSPredicateExpectation(predicate: NSPredicate(format: "value == %@", title), object: field)
        XCTAssertEqual(XCTWaiter.wait(for: [entered], timeout: 30), .completed)
        // Return dismisses focus so SwiftUI has the whole title before Done.
        field.typeText("\n")
        let save = app.navigationBars["Add Song"].buttons["Done"]
        XCTAssertTrue(save.waitForExistence(timeout: 5))
        save.tap()
        let row = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "\(title), ")).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        app.navigationBars.buttons["Done"].tap()

        app.terminate()
        app.launch()
        navigateToSongs()
        let restored = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "\(title), ")).firstMatch
        XCTAssertTrue(restored.waitForExistence(timeout: 10), "The saved song came back after a relaunch")

        // Leave the simulator's songs as they were for the store screenshot tour.
        restored.swipeLeft()
        let delete = app.buttons["Delete"].firstMatch
        XCTAssertTrue(delete.waitForExistence(timeout: 5))
        delete.tap()
        XCTAssertTrue(restored.waitForNonExistence(timeout: 5))
    }

    /// A song list saved by the pre-set-list app, exactly as it stored it: the
    /// class names are the serializer's aliases, so nothing decodes unless the
    /// store registers them before it reads (the SwiftUI app builds the store
    /// while creating the App, before didFinishLaunching).
    static func legacySongs(named title: String) -> [String: Any] {
        func primitive(_ type: String, _ value: Any) -> [String: Any] {
            ["*type": "Primitive", "Type": type, "Value": value]
        }
        let note: [String: Any] = [
            "*type": "Note",
            "Accidental": ["*name": "Flat", "*type": "Accidental"],
            "Frequency": primitive("Double", 311.12698372208092),
            "FriendlyName": "E",
            "IsPlaying": primitive("c", false),
            "Octave": primitive("Integer", 4),
        ]
        let song: [String: Any] = [
            "*type": "PitchedSong",
            "Id": "0B8E7C1A-5D2F-4C3B-9E61-7A4D2F8C1B30",
            "Key": [
                "*type": "Key",
                "KeyType": ["*name": "Major", "*type": "KeyType"],
                "Note": note,
                "NumAccidentals": primitive("Integer", -3),
            ] as [String: Any],
            "Name": title,
        ]
        return ["*type": "List", "*items": [song]]
    }

    /// A fresh app process given only the old storage (no earlier test code has
    /// registered anything) shows the song. The simulator's own songs are set
    /// aside first and put back after, whatever happens in between.
    func testSongsSavedByTheOldAppDecodeInAFreshProcess() throws {
        let title = "Legacy \(Int(Date().timeIntervalSince1970) % 100_000)"
        let json = try JSONSerialization.data(withJSONObject: Self.legacySongs(named: title))
        app.terminate()
        addTeardownBlock {
            let restore = XCUIApplication()
            restore.launchArguments = ["--uitesting"]
            restore.launchEnvironment["PP_UNSTASH_SONGS"] = "1"
            restore.launch()
            restore.terminate()
        }
        app.launchEnvironment["PP_STASH_SONGS"] = "1"
        app.launchEnvironment["PP_LEGACY_SONGS_JSON"] = String(decoding: json, as: UTF8.self)
        app.launch()
        navigateToSongs()
        // The row reads "<title>, <key name>": the key came back too (E flat major).
        let row = app.buttons.matching(NSPredicate(format: "label == %@", "\(title), E")).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10), "the legacy song decoded with its key")

        // Without the old storage, the migrated song is in the new storage.
        app.terminate()
        app.launchEnvironment["PP_LEGACY_SONGS_JSON"] = nil
        app.launch()
        navigateToSongs()
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "\(title), ")).firstMatch
            .waitForExistence(timeout: 10), "migrated into the set-list storage")
        app.terminate()
    }
}
