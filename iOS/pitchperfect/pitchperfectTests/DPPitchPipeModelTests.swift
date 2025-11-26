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
}
