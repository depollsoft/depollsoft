import XCTest
@testable import pitchperfect

final class DPSongsModelTests: XCTestCase {
    private var originalLegacySongs: Any?
    private var originalLists: Any?

    override func setUp() {
        super.setUp()
        let defaults = UserDefaults.standard
        originalLegacySongs = defaults.object(forKey: DPSongsModel.SONGS_KEY_OLD)
        originalLists = defaults.object(forKey: DPSongsModel.SONG_LISTS_KEY)
        defaults.removeObject(forKey: DPSongsModel.SONGS_KEY_OLD)
        defaults.removeObject(forKey: DPSongsModel.SONG_LISTS_KEY)
    }

    override func tearDown() {
        let defaults = UserDefaults.standard
        if let legacy = originalLegacySongs {
            defaults.setValue(legacy, forKey: DPSongsModel.SONGS_KEY_OLD)
        } else {
            defaults.removeObject(forKey: DPSongsModel.SONGS_KEY_OLD)
        }
        if let lists = originalLists {
            defaults.setValue(lists, forKey: DPSongsModel.SONG_LISTS_KEY)
        } else {
            defaults.removeObject(forKey: DPSongsModel.SONG_LISTS_KEY)
        }
        super.tearDown()
    }

    func testSongListOperationsPersistAndNotify() {
        var notificationCount = 0
        let token = NotificationCenter.default.addObserver(forName: DPSongsModel.songsChangedNotificationName,
                                                           object: nil,
                                                           queue: nil) { _ in
            notificationCount += 1
        }
        defer { NotificationCenter.default.removeObserver(token) }

        let model = DPSongsModel()
        let customList = DPSongList(id: "custom")
        customList.name = "Custom"

        model.songLists["custom"] = customList

        let song = DPPitchedSong()
        song.name = "A Song"
        if let key = DPKey.majorKeys().first as? DPKey {
            song.key = key
        }

        customList.addSong(song)
        customList.sortSongs()

        model.storeAll()

        let defaults = UserDefaults.standard
        let storedLists = defaults.dictionary(forKey: DPSongsModel.SONG_LISTS_KEY) as? [String: Any]
        let serialized = storedLists?["custom"] as? [String: Any]
        XCTAssertEqual(serialized?["name"] as? String, "Custom")
        let storedSongs = serialized?["songs"] as? [[AnyHashable: Any]]
        XCTAssertEqual(storedSongs?.count, 1)

        customList.removeSong(song)
        model.removeSongList(forKey: "custom")

        let cleared = defaults.dictionary(forKey: DPSongsModel.SONG_LISTS_KEY) as? [String: Any]
        XCTAssertNil(cleared?["custom"])

        XCTAssertGreaterThanOrEqual(notificationCount, 5)
    }
}
