import XCTest
@testable import pitchperfect

final class DPPitchPipeModelTests: XCTestCase {
    private var originalFToF: Bool = false

    override func setUp() {
        super.setUp()
        originalFToF = DPPitchPipeModel().isFromFToF
    }

    override func tearDown() {
        let model = DPPitchPipeModel()
        model.isFromFToF = originalFToF
        super.tearDown()
    }

    // MARK: - Basic Range Switching Tests
    
    func testNotesSwitchBetweenRanges() {
        let model = DPPitchPipeModel()
        guard let originalNotes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        XCTAssertEqual(originalNotes.count, 12)

        let firstFriendly = originalNotes.first?.friendlyName

        model.isFromFToF.toggle()
        guard let toggledNotes = model.notes as? [DPNote] else {
            XCTFail("Expected toggled notes to bridge to [DPNote]")
            return
        }
        XCTAssertEqual(toggledNotes.count, 12)

        let toggledFirst = toggledNotes.first?.friendlyName
        XCTAssertNotEqual(firstFriendly, toggledFirst)

        // Persisted flag should survive a fresh instance.
        let fresh = DPPitchPipeModel()
        XCTAssertEqual(fresh.isFromFToF, model.isFromFToF)
    }
    
    // MARK: - Note Collection Tests
    
    func testNotesArrayHas12Notes() {
        let model = DPPitchPipeModel()
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        XCTAssertEqual(notes.count, 12, "Pitch pipe should always have exactly 12 notes")
    }
    
    func testCToCRangeStartsWithC() {
        let model = DPPitchPipeModel()
        model.isFromFToF = false
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        XCTAssertEqual(notes.first?.friendlyName, "C", "C-to-C range should start with C")
    }
    
    func testCToCRangeEndsWithB() {
        let model = DPPitchPipeModel()
        model.isFromFToF = false
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        XCTAssertEqual(notes.last?.friendlyName, "B", "C-to-C range should end with B")
    }
    
    func testFToFRangeStartsWithF() {
        let model = DPPitchPipeModel()
        model.isFromFToF = true
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        XCTAssertEqual(notes.first?.friendlyName, "F", "F-to-F range should start with F")
    }
    
    func testFToFRangeEndsWithE() {
        let model = DPPitchPipeModel()
        model.isFromFToF = true
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        XCTAssertEqual(notes.last?.friendlyName, "E", "F-to-F range should end with E")
    }
    
    // MARK: - Key Change Persistence Tests
    
    func testIsFromFToFPersistsToUserDefaults() {
        let model = DPPitchPipeModel()
        
        model.isFromFToF = true
        XCTAssertTrue(UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.isFToF"))
        
        model.isFromFToF = false
        XCTAssertFalse(UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.isFToF"))
    }
    
    func testIsFromFToFSurvivesNewInstance() {
        let model1 = DPPitchPipeModel()
        model1.isFromFToF = true
        
        let model2 = DPPitchPipeModel()
        XCTAssertTrue(model2.isFromFToF, "Setting should persist across instances")
        
        model1.isFromFToF = false
        
        let model3 = DPPitchPipeModel()
        XCTAssertFalse(model3.isFromFToF, "Setting should persist across instances")
    }
    
    // MARK: - Edge Cases
    
    func testMultipleToggles() {
        let model = DPPitchPipeModel()
        
        let initialState = model.isFromFToF
        
        model.isFromFToF.toggle()
        model.isFromFToF.toggle()
        
        XCTAssertEqual(model.isFromFToF, initialState, "Double toggle should return to initial state")
    }
    
    func testNotesAreValidDPNoteObjects() {
        let model = DPPitchPipeModel()
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        for note in notes {
            XCTAssertNotNil(note.friendlyName, "Each note should have a friendly name")
            XCTAssertNotNil(note.accidental, "Each note should have an accidental")
        }
    }
    
    func testCToCRangeOctave() {
        let model = DPPitchPipeModel()
        model.isFromFToF = false
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        // All notes in C-to-C range should be octave 4
        for note in notes {
            XCTAssertEqual(note.octave, 4, "C-to-C range notes should all be octave 4")
        }
    }
    
    func testFToFRangeOctaves() {
        let model = DPPitchPipeModel()
        model.isFromFToF = true
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        // F-to-F range crosses octave boundary
        // F through B should be octave 4, C through E should be octave 5
        var foundOctave4 = false
        var foundOctave5 = false
        
        for note in notes {
            if note.octave == 4 {
                foundOctave4 = true
            } else if note.octave == 5 {
                foundOctave5 = true
            }
        }
        
        XCTAssertTrue(foundOctave4, "F-to-F range should have octave 4 notes")
        XCTAssertTrue(foundOctave5, "F-to-F range should have octave 5 notes")
    }
    
    // MARK: - Note Order Tests
    
    func testCToCNotesAreInChromaticOrder() {
        let model = DPPitchPipeModel()
        model.isFromFToF = false
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        let expectedOrder = ["C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B"]
        
        for (index, note) in notes.enumerated() {
            XCTAssertEqual(note.friendlyName, expectedOrder[index], 
                          "Note at index \(index) should be \(expectedOrder[index]) but was \(note.friendlyName ?? "nil")")
        }
    }
    
    func testFToFNotesAreInChromaticOrder() {
        let model = DPPitchPipeModel()
        model.isFromFToF = true
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        let expectedOrder = ["F", "F", "G", "G", "A", "A", "B", "C", "C", "D", "D", "E"]
        
        for (index, note) in notes.enumerated() {
            XCTAssertEqual(note.friendlyName, expectedOrder[index], 
                          "Note at index \(index) should be \(expectedOrder[index]) but was \(note.friendlyName ?? "nil")")
        }
    }
    
    // MARK: - Sharps and Naturals Tests
    
    func testCToCContainsSharps() {
        let model = DPPitchPipeModel()
        model.isFromFToF = false
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        var sharpCount = 0
        let sharpValue = DPAccidental(int: Int32(Sharp.rawValue))?.value?.intValue ?? -1
        for note in notes {
            if note.accidental.value?.intValue == sharpValue {
                sharpCount += 1
            }
        }
        
        XCTAssertEqual(sharpCount, 5, "C-to-C range should have 5 sharps (C#, D#, F#, G#, A#)")
    }
    
    func testCToCContainsNaturals() {
        let model = DPPitchPipeModel()
        model.isFromFToF = false
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        var naturalCount = 0
        let naturalValue = DPAccidental(int: Int32(Natural.rawValue))?.value?.intValue ?? -1
        for note in notes {
            if note.accidental.value?.intValue == naturalValue {
                naturalCount += 1
            }
        }
        
        XCTAssertEqual(naturalCount, 7, "C-to-C range should have 7 naturals")
    }
    
    // MARK: - Fresh Instance Tests
    
    func testFreshInstanceReflectsPersistedState() {
        let model1 = DPPitchPipeModel()
        model1.isFromFToF = true
        
        let fresh = DPPitchPipeModel()
        XCTAssertEqual(fresh.isFromFToF, true)
        
        guard let notes = fresh.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        XCTAssertEqual(notes.first?.friendlyName, "F", "Fresh instance should reflect F-to-F setting")
    }
    
    // MARK: - Notes Property Consistency
    
    func testNotesPropertyReturnsConsistentResults() {
        let model = DPPitchPipeModel()
        
        guard let notes1 = model.notes as? [DPNote],
              let notes2 = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        XCTAssertEqual(notes1.count, notes2.count)
        
        for (index, note1) in notes1.enumerated() {
            let note2 = notes2[index]
            XCTAssertEqual(note1.friendlyName, note2.friendlyName)
            XCTAssertEqual(note1.octave, note2.octave)
        }
    }
    
    func testNotesChangeWhenModeChanges() {
        let model = DPPitchPipeModel()
        model.isFromFToF = false
        
        guard let ctocNotes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        let firstCtoC = ctocNotes.first?.friendlyName
        
        model.isFromFToF = true
        
        guard let ftofNotes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        let firstFtoF = ftofNotes.first?.friendlyName
        
        XCTAssertNotEqual(firstCtoC, firstFtoF, "First note should change when mode changes")
    }
}
