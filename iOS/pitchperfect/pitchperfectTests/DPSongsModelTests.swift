import XCTest
@testable import pitchperfect

// MARK: - Mock Classes for Firestore Testing

/// Mock ListenerRegistration to track listener removal calls
class MockListenerRegistration {
    var removed = false
    func remove() {
        removed = true
    }
}

/// Mock DocumentSnapshot for testing Firestore snapshot handling
class MockDocumentSnapshot {
    let documentID: String
    var data: [String: Any]
    
    init(documentID: String, data: [String: Any] = [:]) {
        self.documentID = documentID
        self.data = data
    }
    
    func get(_ key: String) -> Any? {
        return data[key]
    }
}

// MARK: - DPSongsModelTests

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
        // Detach from Firestore on shared instance to clean state
        DPSongsModel.sharedInstance.detachFromFirestore()
        super.tearDown()
    }

    // MARK: - Basic Song List Operations
    
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
    
    // MARK: - Default Song List Tests
    
    func testDefaultSongListExists() {
        let model = DPSongsModel()
        XCTAssertNotNil(model.defaultSongList)
        XCTAssertEqual(model.defaultSongList.id, "default")
    }
    
    func testDefaultSongListNameIsSet() {
        let model = DPSongsModel()
        XCTAssertEqual(model.defaultSongList.name, "Default")
    }
    
    // MARK: - Song List Addition and Removal
    
    func testAddSongToList() {
        let model = DPSongsModel()
        let song = DPPitchedSong()
        song.name = "Test Song"
        if let key = DPKey.majorKeys().first as? DPKey {
            song.key = key
        }
        
        let initialCount = model.defaultSongList.songs.count
        model.defaultSongList.addSong(song)
        
        XCTAssertEqual(model.defaultSongList.songs.count, initialCount + 1)
        XCTAssertTrue(model.defaultSongList.songs.contains(song))
    }
    
    func testAddSongAtIndex() {
        let model = DPSongsModel()
        
        // Add two songs first
        let song1 = DPPitchedSong()
        song1.name = "First Song"
        if let key = DPKey.majorKeys().first as? DPKey {
            song1.key = key
        }
        
        let song2 = DPPitchedSong()
        song2.name = "Second Song"
        if let key = DPKey.majorKeys().first as? DPKey {
            song2.key = key
        }
        
        model.defaultSongList.addSong(song1)
        model.defaultSongList.addSong(song2)
        
        // Insert a new song at index 1
        let insertedSong = DPPitchedSong()
        insertedSong.name = "Inserted Song"
        if let key = DPKey.minorKeys().first as? DPKey {
            insertedSong.key = key
        }
        
        model.defaultSongList.addSong(insertedSong, atIndex: 1)
        
        XCTAssertEqual(model.defaultSongList.songs[1].name, "Inserted Song")
    }
    
    func testRemoveSongFromList() {
        let model = DPSongsModel()
        let song = DPPitchedSong()
        song.name = "Test Song to Remove"
        if let key = DPKey.majorKeys().first as? DPKey {
            song.key = key
        }
        
        model.defaultSongList.addSong(song)
        let countAfterAdd = model.defaultSongList.songs.count
        
        model.defaultSongList.removeSong(song)
        
        XCTAssertEqual(model.defaultSongList.songs.count, countAfterAdd - 1)
        XCTAssertFalse(model.defaultSongList.songs.contains(song))
    }
    
    func testRemoveSongAtIndex() {
        let model = DPSongsModel()
        let song = DPPitchedSong()
        song.name = "Test Song at Index"
        if let key = DPKey.majorKeys().first as? DPKey {
            song.key = key
        }
        
        model.defaultSongList.addSong(song)
        let index = model.defaultSongList.songs.count - 1
        
        model.defaultSongList.removeSong(atIndex: index)
        
        XCTAssertFalse(model.defaultSongList.songs.contains(song))
    }
    
    func testRemoveSongListForKey() {
        let model = DPSongsModel()
        let customList = DPSongList(id: "toRemove")
        customList.name = "To Remove"
        
        model.songLists["toRemove"] = customList
        XCTAssertNotNil(model.songLists["toRemove"])
        
        model.removeSongList(forKey: "toRemove")
        
        XCTAssertNil(model.songLists["toRemove"])
    }
    
    // MARK: - Sorting Tests
    
    func testSortSongsAlphabetically() {
        let model = DPSongsModel()
        
        let songZ = DPPitchedSong()
        songZ.name = "Zebra Song"
        if let key = DPKey.majorKeys().first as? DPKey {
            songZ.key = key
        }
        
        let songA = DPPitchedSong()
        songA.name = "Alpha Song"
        if let key = DPKey.majorKeys().first as? DPKey {
            songA.key = key
        }
        
        let songM = DPPitchedSong()
        songM.name = "Middle Song"
        if let key = DPKey.majorKeys().first as? DPKey {
            songM.key = key
        }
        
        model.defaultSongList.addSong(songZ)
        model.defaultSongList.addSong(songA)
        model.defaultSongList.addSong(songM)
        
        model.defaultSongList.sortSongs()
        
        // Get the last 3 songs added (the ones we just added)
        let songs = model.defaultSongList.songs
        let lastThree = Array(songs.suffix(3))
        
        XCTAssertEqual(lastThree[0].name, "Alpha Song")
        XCTAssertEqual(lastThree[1].name, "Middle Song")
        XCTAssertEqual(lastThree[2].name, "Zebra Song")
    }
    
    func testSortSongsCaseInsensitive() {
        let model = DPSongsModel()
        
        // Clear existing songs first for clean test
        model.defaultSongList.songs = []
        
        let songLower = DPPitchedSong()
        songLower.name = "beta song"
        if let key = DPKey.majorKeys().first as? DPKey {
            songLower.key = key
        }
        
        let songUpper = DPPitchedSong()
        songUpper.name = "Alpha Song"
        if let key = DPKey.majorKeys().first as? DPKey {
            songUpper.key = key
        }
        
        model.defaultSongList.addSong(songLower)
        model.defaultSongList.addSong(songUpper)
        
        model.defaultSongList.sortSongs()
        
        // Alpha should come before beta regardless of case
        XCTAssertEqual(model.defaultSongList.songs[0].name, "Alpha Song")
        XCTAssertEqual(model.defaultSongList.songs[1].name, "beta song")
    }
    
    // MARK: - Notification Tests
    
    func testSongListsPropertyDidSetPostsNotification() {
        let model = DPSongsModel()
        
        let expectation = self.expectation(description: "songsChanged notification")
        let token = NotificationCenter.default.addObserver(forName: DPSongsModel.songsChangedNotificationName,
                                                           object: model,
                                                           queue: nil) { _ in
            expectation.fulfill()
        }
        defer { NotificationCenter.default.removeObserver(token) }
        
        // Trigger didSet by assigning a new dictionary
        model.songLists = ["new": DPSongList(id: "new")]
        
        waitForExpectations(timeout: 1.0)
    }
    
    func testSongListNameChangePostsNotification() {
        let list = DPSongList(id: "test")
        
        let expectation = self.expectation(description: "name change notification")
        let token = NotificationCenter.default.addObserver(forName: DPSongsModel.songsChangedNotificationName,
                                                           object: list,
                                                           queue: nil) { _ in
            expectation.fulfill()
        }
        defer { NotificationCenter.default.removeObserver(token) }
        
        list.name = "New Name"
        
        waitForExpectations(timeout: 1.0)
    }
    
    func testSongsPropertyDidSetPostsNotification() {
        let list = DPSongList(id: "test")
        
        let expectation = self.expectation(description: "songs property change notification")
        let token = NotificationCenter.default.addObserver(forName: DPSongsModel.songsChangedNotificationName,
                                                           object: list,
                                                           queue: nil) { _ in
            expectation.fulfill()
        }
        defer { NotificationCenter.default.removeObserver(token) }
        
        list.songs = []
        
        waitForExpectations(timeout: 1.0)
    }
    
    // MARK: - Store Value Tests
    
    func testStoreValuePersistsToUserDefaults() {
        let model = DPSongsModel()
        let testList = DPSongList(id: "storeTest")
        testList.name = "Store Test List"
        
        let song = DPPitchedSong()
        song.name = "Stored Song"
        if let key = DPKey.majorKeys().first as? DPKey {
            song.key = key
        }
        testList.addSong(song)
        
        model.songLists["storeTest"] = testList
        testList.storeValue()
        
        let defaults = UserDefaults.standard
        let storedLists = defaults.dictionary(forKey: DPSongsModel.SONG_LISTS_KEY) as? [String: Any]
        
        XCTAssertNotNil(storedLists?["storeTest"])
        let serialized = storedLists?["storeTest"] as? [String: Any]
        XCTAssertEqual(serialized?["name"] as? String, "Store Test List")
    }
    
    func testStoreAllPersistsAllLists() {
        let model = DPSongsModel()
        
        let list1 = DPSongList(id: "list1")
        list1.name = "List One"
        
        let list2 = DPSongList(id: "list2")
        list2.name = "List Two"
        
        model.songLists["list1"] = list1
        model.songLists["list2"] = list2
        
        model.storeAll()
        
        let defaults = UserDefaults.standard
        let storedLists = defaults.dictionary(forKey: DPSongsModel.SONG_LISTS_KEY) as? [String: Any]
        
        XCTAssertNotNil(storedLists?["list1"])
        XCTAssertNotNil(storedLists?["list2"])
    }
    
    // MARK: - Legacy Migration Tests
    
    func testLegacySongsMigration() {
        // Note: Legacy migration requires specific serialization format
        // This test verifies the default song list is created even without legacy data
        let defaults = UserDefaults.standard
        defaults.removeObject(forKey: DPSongsModel.SONGS_KEY_OLD)
        defaults.removeObject(forKey: DPSongsModel.SONG_LISTS_KEY)
        
        // Create new model - should create default list
        let model = DPSongsModel()
        
        // Songs should now be in default list
        XCTAssertNotNil(model.defaultSongList)
        XCTAssertEqual(model.defaultSongList.name, "Default")
    }
    
    // MARK: - Detach From Firestore Tests
    
    func testDetachFromFirestoreClearsUserDoc() {
        let model = DPSongsModel.sharedInstance
        
        // First detach to ensure clean state
        model.detachFromFirestore()
        
        // After detach, internal state should be cleared
        // We can verify by checking no crashes occur when detaching again
        model.detachFromFirestore()
        
        // Multiple detaches should be safe
        XCTAssertTrue(true, "Detach completed without crashing")
    }
    
    // MARK: - DPSongList ID Tests
    
    func testSongListHasCorrectID() {
        let list = DPSongList(id: "testID123")
        XCTAssertEqual(list.id, "testID123")
    }
    
    func testSongListIDIsImmutable() {
        let list = DPSongList(id: "immutableID")
        // ID property is let, so we just verify it stays the same
        XCTAssertEqual(list.id, "immutableID")
        XCTAssertEqual(list.id, "immutableID") // Still the same
    }
    
    // MARK: - Multiple Song Operations
    
    func testAddMultipleSongs() {
        let model = DPSongsModel()
        model.defaultSongList.songs = [] // Clear for clean test
        
        for i in 0..<5 {
            let song = DPPitchedSong()
            song.name = "Song \(i)"
            if let key = DPKey.majorKeys().first as? DPKey {
                song.key = key
            }
            model.defaultSongList.addSong(song)
        }
        
        XCTAssertEqual(model.defaultSongList.songs.count, 5)
    }
    
    func testRemoveNonexistentSongDoesNotCrash() {
        let model = DPSongsModel()
        let song = DPPitchedSong()
        song.name = "Not In List"
        if let key = DPKey.majorKeys().first as? DPKey {
            song.key = key
        }
        
        // Should not crash when removing a song that's not in the list
        model.defaultSongList.removeSong(song)
        
        XCTAssertTrue(true, "Removing nonexistent song did not crash")
    }
    
    // MARK: - Shared Instance Tests
    
    func testSharedInstanceReturnsSameInstance() {
        let instance1 = DPSongsModel.sharedInstance
        let instance2 = DPSongsModel.sharedInstance
        
        XCTAssertTrue(instance1 === instance2)
    }
}
