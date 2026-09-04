import XCTest
@testable import pitchperfect

final class PitchChordTests: XCTestCase {
    func testRootPositionDominantSeventhOnEveryRootIsBarbershop() {
        for root in 0...2 {
            XCTAssertEqual("BARBERSHOP!", PitchChord.name(cells: [root, root + 4, root + 7, root + 10]))
        }
    }

    func testVoicingAndDoubledRootDoNotMatter() {
        // First inversion of G7 inside one C-to-C octave: B4 D5(=2) F4 G4.
        XCTAssertEqual("BARBERSHOP!", PitchChord.name(cells: [11, 2, 5, 7]))
        // C7 with the root doubled at the octave cell.
        XCTAssertEqual("BARBERSHOP!", PitchChord.name(cells: [0, 4, 7, 10, 12]))
        XCTAssertEqual("BARBERSHOP!", PitchChord.name(cells: [10, 7, 4, 0]))
    }

    func testOtherChordsStayCounted() {
        XCTAssertNil(PitchChord.name(cells: [0, 4, 7, 11]), "major seventh")
        XCTAssertNil(PitchChord.name(cells: [0, 3, 7, 10]), "minor seventh")
        XCTAssertNil(PitchChord.name(cells: [0, 3, 6, 9]), "diminished")
        XCTAssertNil(PitchChord.name(cells: [0, 4, 7]), "triad")
        XCTAssertNil(PitchChord.name(cells: [0, 2, 4, 7, 10]), "five distinct notes")
        XCTAssertNil(PitchChord.name(cells: [0, 12]), "octave only")
    }
}
