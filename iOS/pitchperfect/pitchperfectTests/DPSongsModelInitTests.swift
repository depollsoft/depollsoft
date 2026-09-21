//
//  DPSongsModelInitTests.swift
//  pitchperfectTests
//
//  Building the songs model must be silent. The Songs screen answers every
//  `.songsChanged` with a read of `DPSongsModel.sharedInstance`; a notification
//  posted from inside that singleton's initialiser re-enters it and traps the
//  app, which is exactly what a fresh install hit on the first visit to Songs.
//

import XCTest
@testable import pitchperfect

final class DPSongsModelInitTests: XCTestCase {
    private let defaults = UserDefaults.standard
    private var originalLegacy: Any?
    private var originalLists: Any?
    private var originalCurrent: Any?

    override func setUp() {
        super.setUp()
        originalLegacy = defaults.object(forKey: DPSongsModel.legacySongsKey)
        originalLists = defaults.object(forKey: DPSongsModel.songListsKey)
        originalCurrent = defaults.object(forKey: DPSongsModel.currentListKey)
        defaults.removeObject(forKey: DPSongsModel.legacySongsKey)
        defaults.removeObject(forKey: DPSongsModel.songListsKey)
        defaults.removeObject(forKey: DPSongsModel.currentListKey)
    }

    override func tearDown() {
        defaults.set(originalLegacy, forKey: DPSongsModel.legacySongsKey)
        defaults.set(originalLists, forKey: DPSongsModel.songListsKey)
        defaults.set(originalCurrent, forKey: DPSongsModel.currentListKey)
        super.tearDown()
    }

    private func notificationsPosted(during work: () -> Void) -> Int {
        var count = 0
        let token = NotificationCenter.default.addObserver(forName: .songsChanged, object: nil, queue: nil) { _ in
            count += 1
        }
        defer { NotificationCenter.default.removeObserver(token) }
        work()
        return count
    }

    func testAFreshInstallBuildsTheModelWithoutPostingSongsChanged() {
        var model: DPSongsModel?
        let posted = notificationsPosted { model = DPSongsModel() }
        XCTAssertEqual(posted, 0)
        XCTAssertEqual(model?.songLists[DPSongsModel.defaultListId]?.name, "Default")
    }

    func testStoredSetListsAreRestoredWithoutPostingSongsChanged() {
        let seeded = DPSongsModel()
        let list = seeded.createList(named: "Saturday show")
        XCTAssertNotNil(list)
        XCTAssertNotNil(defaults.dictionary(forKey: DPSongsModel.songListsKey))

        var restored: DPSongsModel?
        let posted = notificationsPosted { restored = DPSongsModel() }
        XCTAssertEqual(posted, 0)
        XCTAssertEqual(restored?.songLists.count, 2)
    }

    func testAListStillAnnouncesChangesMadeAfterItIsBuilt() {
        let list = DPSongList(id: "later", name: "Later", songs: [])
        let posted = notificationsPosted { list.name = "Later still" }
        XCTAssertEqual(posted, 1)
    }
}
