import XCTest
@testable import pitchperfect

// MARK: - DPSongList Edge Case Tests

final class DPSongListEdgeCaseTests: XCTestCase {
    
    // MARK: - Add Song Tests
    
    func testAddSongAtIndex() {
        let list = DPSongList(id: "test")
        
        let song1 = DPPitchedSong()
        song1.name = "First"
        list.addSong(song1)
        
        let song2 = DPPitchedSong()
        song2.name = "Second"
        list.addSong(song2)
        
        let inserted = DPPitchedSong()
        inserted.name = "Inserted"
        list.addSong(inserted, atIndex: 1)
        
        XCTAssertEqual(list.songs.count, 3)
        XCTAssertEqual(list.songs[0].name, "First")
        XCTAssertEqual(list.songs[1].name, "Inserted")
        XCTAssertEqual(list.songs[2].name, "Second")
    }
    
    func testAddSongAtBoundaryIndices() {
        let list = DPSongList(id: "test")
        
        let existing = DPPitchedSong()
        existing.name = "Existing"
        list.addSong(existing)
        
        // Insert at index 0 (beginning)
        let newFirst = DPPitchedSong()
        newFirst.name = "New First"
        list.addSong(newFirst, atIndex: 0)
        
        XCTAssertEqual(list.songs[0].name, "New First")
        XCTAssertEqual(list.songs[1].name, "Existing")
    }
    
    // MARK: - Remove Song Tests
    
    func testSortSongsAlphabetically() {
        let list = DPSongList(id: "test")
        
        let songC = DPPitchedSong()
        songC.name = "Charlie"
        list.addSong(songC)
        
        let songA = DPPitchedSong()
        songA.name = "Alpha"
        list.addSong(songA)
        
        let songB = DPPitchedSong()
        songB.name = "Bravo"
        list.addSong(songB)
        
        list.sortSongs()
        
        XCTAssertEqual(list.songs[0].name, "Alpha")
        XCTAssertEqual(list.songs[1].name, "Bravo")
        XCTAssertEqual(list.songs[2].name, "Charlie")
    }
    
    func testSortSongsCaseInsensitive() {
        let list = DPSongList(id: "test")
        
        let songB = DPPitchedSong()
        songB.name = "BRAVO"
        list.addSong(songB)
        
        let songA = DPPitchedSong()
        songA.name = "alpha"
        list.addSong(songA)
        
        list.sortSongs()
        
        XCTAssertEqual(list.songs[0].name, "alpha")
        XCTAssertEqual(list.songs[1].name, "BRAVO")
    }
    
    func testSortEmptyList() {
        let list = DPSongList(id: "test")
        list.sortSongs()
        XCTAssertTrue(list.songs.isEmpty)
    }
    
    func testSortSingleSong() {
        let list = DPSongList(id: "test")
        let song = DPPitchedSong()
        song.name = "Only"
        list.addSong(song)
        
        list.sortSongs()
        
        XCTAssertEqual(list.songs.count, 1)
        XCTAssertEqual(list.songs[0].name, "Only")
    }
    
    // MARK: - Notification Tests
    
    func testAddSongPostsNotification() {
        let list = DPSongList(id: "test")
        
        let expectation = self.expectation(description: "addSong notification")
        expectation.expectedFulfillmentCount = 1
        expectation.assertForOverFulfill = false // Allow multiple notifications
        let token = NotificationCenter.default.addObserver(forName: DPSongsModel.songsChangedNotificationName,
                                                           object: list,
                                                           queue: nil) { _ in
            expectation.fulfill()
        }
        defer { NotificationCenter.default.removeObserver(token) }
        
        let song = DPPitchedSong()
        list.addSong(song)
        
        waitForExpectations(timeout: 1.0)
    }
    
    func testRemoveSongPostsNotification() {
        let list = DPSongList(id: "test")
        let song = DPPitchedSong()
        list.songs = [song] // Direct assignment to avoid notification from addSong
        
        let expectation = self.expectation(description: "removeSong notification")
        expectation.expectedFulfillmentCount = 1
        expectation.assertForOverFulfill = false // Allow multiple notifications
        let token = NotificationCenter.default.addObserver(forName: DPSongsModel.songsChangedNotificationName,
                                                           object: list,
                                                           queue: nil) { _ in
            expectation.fulfill()
        }
        defer { NotificationCenter.default.removeObserver(token) }
        
        list.removeSong(song)
        
        waitForExpectations(timeout: 1.0)
    }
    
    func testSortSongsPostsNotification() {
        let list = DPSongList(id: "test")
        let song1 = DPPitchedSong()
        song1.name = "Z"
        let song2 = DPPitchedSong()
        song2.name = "A"
        list.songs = [song1, song2]
        
        let expectation = self.expectation(description: "sortSongs notification")
        expectation.expectedFulfillmentCount = 1
        expectation.assertForOverFulfill = false // Allow multiple notifications
        let token = NotificationCenter.default.addObserver(forName: DPSongsModel.songsChangedNotificationName,
                                                           object: list,
                                                           queue: nil) { _ in
            expectation.fulfill()
        }
        defer { NotificationCenter.default.removeObserver(token) }
        
        list.sortSongs()
        
        waitForExpectations(timeout: 1.0)
    }
    
    // MARK: - Store Value Tests
    
    func testStoreValuePersistsName() {
        let list = DPSongList(id: "storeTest")
        list.name = "Persisted Name"
        
        list.storeValue()
        
        let defaults = UserDefaults.standard
        let storedLists = defaults.dictionary(forKey: DPSongsModel.songListsKey) as? [String: Any]
        let storedList = storedLists?["storeTest"] as? [String: Any]
        
        XCTAssertEqual(storedList?["name"] as? String, "Persisted Name")
    }
    
    func testStoreValuePersistsSongs() {
        let list = DPSongList(id: "storeSongsTest")
        list.name = "Test"
        
        let song = DPPitchedSong()
        song.name = "Persisted Song"
        if let key = DPKey.majorKeys().first as? DPKey {
            song.key = key
        }
        list.addSong(song)
        
        list.storeValue()
        
        let defaults = UserDefaults.standard
        let storedLists = defaults.dictionary(forKey: DPSongsModel.songListsKey) as? [String: Any]
        let storedList = storedLists?["storeSongsTest"] as? [String: Any]
        let storedSongs = storedList?["songs"] as? [[AnyHashable: Any]]
        
        XCTAssertNotNil(storedSongs)
        XCTAssertEqual(storedSongs?.count, 1)
    }
    
    // MARK: - Songs Property Tests
    
    func testSongsPropertyCanBeSetDirectly() {
        let list = DPSongList(id: "test")
        
        let song1 = DPPitchedSong()
        song1.name = "One"
        let song2 = DPPitchedSong()
        song2.name = "Two"
        
        list.songs = [song1, song2]
        
        XCTAssertEqual(list.songs.count, 2)
    }
    
    func testSongsPropertyCanBeCleared() {
        let list = DPSongList(id: "test")
        
        let song = DPPitchedSong()
        list.addSong(song)
        
        list.songs = []
        
        XCTAssertTrue(list.songs.isEmpty)
    }
}
