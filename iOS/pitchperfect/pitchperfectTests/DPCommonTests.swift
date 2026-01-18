import XCTest
@testable import pitchperfect

// MARK: - DPCommon Tests

final class DPCommonTests: XCTestCase {
    
    // MARK: - Settings Button Tests
    
    /// Tests that settings button is properly configured with target-action pattern
    func testGetSettingsButtonConfiguresTargetActionCorrectly() {
        let button = DPCommon.getSettingsButton(target: self, selector: #selector(dummySelector))
        
        // Verify target-action binding (the actual functionality being tested)
        XCTAssertTrue(button.target as AnyObject === self, "Target should be correctly bound")
        XCTAssertEqual(button.action, #selector(dummySelector), "Action should be correctly bound")
        XCTAssertNotNil(button.image, "Button should have a gear image for visual identification")
    }
    
    // MARK: - Helper Methods
    
    @objc func dummySelector() {
        // Empty selector for testing
    }
}

// MARK: - Integration Tests

final class PitchPerfectIntegrationTests: XCTestCase {
    
    // MARK: - Model Integration Tests
    
    func testSongsModelAndSettingsModelCoexist() {
        let songsModel = DPSongsModel.sharedInstance
        let settingsModel = DPSettingsModel.sharedInstance
        
        XCTAssertNotNil(songsModel)
        XCTAssertNotNil(settingsModel)
        
        // Both should be usable
        XCTAssertNotNil(songsModel.defaultSongList)
        _ = settingsModel.wakeLock
    }
    
    func testSongWithKeyCanBeAddedToList() {
        let model = DPSongsModel()
        
        // Create a song with a key
        let song = DPPitchedSong()
        song.name = "Integration Test Song"
        
        // Get a key from the available keys
        if let key = DPKey.majorKeys().first as? DPKey {
            song.key = key
        }
        
        // Add to list
        model.defaultSongList.addSong(song)
        
        // Verify
        XCTAssertTrue(model.defaultSongList.songs.contains(song))
        XCTAssertNotNil(song.key)
        
        // Clean up
        model.defaultSongList.removeSong(song)
    }
    
    func testPitchPipeModelNotesCanBeUsed() {
        let model = DPPitchPipeModel()
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes")
            return
        }
        
        for note in notes {
            // Each note should be playable (though we won't actually play in tests)
            XCTAssertNotNil(note)
            XCTAssertNotNil(note.friendlyName)
            XCTAssertGreaterThan(note.frequency, 0)
        }
    }
    
    // MARK: - Key and Note Integration
    
    func testMajorKeysHaveValidNotes() {
        guard let majorKeys = DPKey.majorKeys() as? [DPKey] else {
            XCTFail("Expected major keys")
            return
        }
        
        for key in majorKeys {
            XCTAssertNotNil(key.note)
            XCTAssertNotNil(key.note.friendlyName)
            XCTAssertNotNil(key.note.accidental)
        }
    }
    
    func testMinorKeysHaveValidNotes() {
        guard let minorKeys = DPKey.minorKeys() as? [DPKey] else {
            XCTFail("Expected minor keys")
            return
        }
        
        for key in minorKeys {
            XCTAssertNotNil(key.note)
            XCTAssertNotNil(key.note.friendlyName)
            XCTAssertNotNil(key.note.accidental)
        }
    }
    
    // MARK: - Notification Integration
    
    func testSettingsAndSongsNotificationsAreDistinct() {
        let settingsNotification = DPSettingsModel.settingsChangedNotificationName
        let songsNotification = DPSongsModel.songsChangedNotificationName
        
        XCTAssertNotEqual(settingsNotification, songsNotification)
    }
    
    // MARK: - Persistence Integration
    
    func testSettingsAndSongsPersistIndependently() {
        let settingsModel = DPSettingsModel.sharedInstance
        let songsModel = DPSongsModel()
        
        // Modify settings
        let originalWakeLock = settingsModel.wakeLock
        settingsModel.wakeLock = !originalWakeLock
        
        // Modify songs
        let song = DPPitchedSong()
        song.name = "Persistence Test"
        if let key = DPKey.majorKeys().first as? DPKey {
            song.key = key
        }
        songsModel.defaultSongList.addSong(song)
        
        // Verify they don't affect each other
        XCTAssertEqual(settingsModel.wakeLock, !originalWakeLock)
        XCTAssertTrue(songsModel.defaultSongList.songs.contains(song))
        
        // Clean up
        settingsModel.wakeLock = originalWakeLock
        songsModel.defaultSongList.removeSong(song)
    }
}

// MARK: - DPPitchedSong Swift Tests

final class DPPitchedSongSwiftTests: XCTestCase {
    
    /// Tests that each song gets a unique identifier when created
    func testMultipleSongsHaveUniqueIds() {
        var ids: Set<String> = []
        
        for _ in 0..<100 {
            let song = DPPitchedSong()
            ids.insert(song.id)
        }
        
        XCTAssertEqual(ids.count, 100, "All 100 songs should have unique IDs")
    }
    
    /// Tests that songs can be associated with any major or minor key
    func testSongCanUseAnyKey() {
        guard let majorKeys = DPKey.majorKeys() as? [DPKey],
              let minorKeys = DPKey.minorKeys() as? [DPKey] else {
            XCTFail("Expected keys arrays")
            return
        }
        
        // Verify songs work with major keys
        for key in majorKeys {
            let song = DPPitchedSong()
            song.key = key
            XCTAssertEqual(song.key, key, "Song should retain assigned major key")
        }
        
        // Verify songs work with minor keys  
        for key in minorKeys {
            let song = DPPitchedSong()
            song.key = key
            XCTAssertEqual(song.key, key, "Song should retain assigned minor key")
        }
    }
}
