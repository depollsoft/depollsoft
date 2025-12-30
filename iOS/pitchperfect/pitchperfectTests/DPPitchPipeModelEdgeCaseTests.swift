import XCTest
@testable import pitchperfect

// MARK: - DPPitchPipeModel Additional Tests

final class DPPitchPipeModelEdgeCaseTests: XCTestCase {
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
    
    // MARK: - Note Frequency Tests
    
    func testAllNotesHavePositiveFrequency() {
        let model = DPPitchPipeModel()
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        for note in notes {
            XCTAssertGreaterThan(note.frequency, 0, "Note \(note.friendlyName ?? "?") should have positive frequency")
        }
    }
    
    func testNotesInAscendingFrequencyOrder() {
        let model = DPPitchPipeModel()
        model.isFromFToF = false
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        for i in 1..<notes.count {
            XCTAssertGreaterThanOrEqual(notes[i].frequency, notes[i-1].frequency,
                "Notes should be in ascending frequency order")
        }
    }
    
    func testFToFNotesInAscendingFrequencyOrder() {
        let model = DPPitchPipeModel()
        model.isFromFToF = true
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        for i in 1..<notes.count {
            XCTAssertGreaterThanOrEqual(notes[i].frequency, notes[i-1].frequency,
                "Notes should be in ascending frequency order")
        }
    }
    
    // MARK: - Note Count Consistency
    
    func testAlways12Notes() {
        let model = DPPitchPipeModel()
        
        model.isFromFToF = false
        guard let ctocNotes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        XCTAssertEqual(ctocNotes.count, 12)
        
        model.isFromFToF = true
        guard let ftofNotes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        XCTAssertEqual(ftofNotes.count, 12)
    }
    
    // MARK: - First and Last Note Tests
    
    func testCToCFirstNoteIsC() {
        let model = DPPitchPipeModel()
        model.isFromFToF = false
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        XCTAssertEqual(notes.first?.friendlyName, "C")
    }
    
    func testCToCLastNoteIsB() {
        let model = DPPitchPipeModel()
        model.isFromFToF = false
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        XCTAssertEqual(notes.last?.friendlyName, "B")
    }
    
    func testFToFFirstNoteIsF() {
        let model = DPPitchPipeModel()
        model.isFromFToF = true
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        XCTAssertEqual(notes.first?.friendlyName, "F")
    }
    
    func testFToFLastNoteIsE() {
        let model = DPPitchPipeModel()
        model.isFromFToF = true
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        XCTAssertEqual(notes.last?.friendlyName, "E")
    }
    
    // MARK: - Mode Switching Edge Cases
    
    func testRapidModeToggling() {
        let model = DPPitchPipeModel()
        let initial = model.isFromFToF
        
        for _ in 0..<20 {
            model.isFromFToF.toggle()
        }
        
        // After even number of toggles, should be back to initial
        XCTAssertEqual(model.isFromFToF, initial)
    }
    
    func testModePersistedAfterToggle() {
        let model = DPPitchPipeModel()
        model.isFromFToF = true
        
        // Create new instance to verify persistence
        let newModel = DPPitchPipeModel()
        XCTAssertTrue(newModel.isFromFToF)
        
        model.isFromFToF = false
        
        let newerModel = DPPitchPipeModel()
        XCTAssertFalse(newerModel.isFromFToF)
    }
    
    // MARK: - Accidental Distribution Tests
    
    func testEachModeHas5Sharps() {
        let model = DPPitchPipeModel()
        
        for mode in [false, true] {
            model.isFromFToF = mode
            
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
            
            XCTAssertEqual(sharpCount, 5, "Mode \(mode ? "F-to-F" : "C-to-C") should have 5 sharps")
        }
    }
    
    func testEachModeHas7Naturals() {
        let model = DPPitchPipeModel()
        
        for mode in [false, true] {
            model.isFromFToF = mode
            
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
            
            XCTAssertEqual(naturalCount, 7, "Mode \(mode ? "F-to-F" : "C-to-C") should have 7 naturals")
        }
    }
    
    // MARK: - Note Object Validity Tests
    
    func testAllNotesHaveFriendlyName() {
        let model = DPPitchPipeModel()
        
        for mode in [false, true] {
            model.isFromFToF = mode
            
            guard let notes = model.notes as? [DPNote] else {
                XCTFail("Expected notes to bridge to [DPNote]")
                return
            }
            
            for note in notes {
                XCTAssertNotNil(note.friendlyName)
                XCTAssertFalse(note.friendlyName?.isEmpty ?? true)
            }
        }
    }
    
    func testAllNotesHaveAccidental() {
        let model = DPPitchPipeModel()
        
        for mode in [false, true] {
            model.isFromFToF = mode
            
            guard let notes = model.notes as? [DPNote] else {
                XCTFail("Expected notes to bridge to [DPNote]")
                return
            }
            
            for note in notes {
                XCTAssertNotNil(note.accidental)
            }
        }
    }
    
    // MARK: - Frequency Range Tests
    
    func testCToCFrequencyRange() {
        let model = DPPitchPipeModel()
        model.isFromFToF = false
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        // C4 ≈ 261 Hz, B4 ≈ 494 Hz
        let minFreq = notes.min(by: { $0.frequency < $1.frequency })?.frequency ?? 0
        let maxFreq = notes.max(by: { $0.frequency < $1.frequency })?.frequency ?? 0
        
        XCTAssertGreaterThan(minFreq, 200, "Lowest note should be above 200 Hz")
        XCTAssertLessThan(maxFreq, 550, "Highest note should be below 550 Hz")
    }
    
    func testFToFFrequencyRange() {
        let model = DPPitchPipeModel()
        model.isFromFToF = true
        
        guard let notes = model.notes as? [DPNote] else {
            XCTFail("Expected notes to bridge to [DPNote]")
            return
        }
        
        // F4 ≈ 349 Hz, E5 ≈ 659 Hz
        let minFreq = notes.min(by: { $0.frequency < $1.frequency })?.frequency ?? 0
        let maxFreq = notes.max(by: { $0.frequency < $1.frequency })?.frequency ?? 0
        
        XCTAssertGreaterThan(minFreq, 300, "Lowest note should be above 300 Hz")
        XCTAssertLessThan(maxFreq, 700, "Highest note should be below 700 Hz")
    }
    
    // MARK: - UserDefaults Key Tests
    
    func testUserDefaultsKeyIsCorrect() {
        let model = DPPitchPipeModel()
        model.isFromFToF = true
        
        let value = UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.isFToF")
        XCTAssertTrue(value)
        
        model.isFromFToF = false
        let value2 = UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.isFToF")
        XCTAssertFalse(value2)
    }
}
