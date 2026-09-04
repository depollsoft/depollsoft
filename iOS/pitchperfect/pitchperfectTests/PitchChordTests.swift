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

    func testWidgetAppGroupTracksBuildVariant() {
        XCTAssertEqual(
            "group.depollsoft.pitchperfect",
            WidgetSharedDefaults.suiteName(for: "depollsoft.pitchperfect.widget")
        )
        XCTAssertEqual(
            "group.depollsoft.pitchperfect.private",
            WidgetSharedDefaults.suiteName(for: "depollsoft.pitchperfect.private.widget")
        )
    }

    func testWidgetRangeIntentPersistsItsAssignedValue() async throws {
        let previousRange = WidgetRangeState.rawValue
        defer { WidgetRangeState.set(previousRange) }

        _ = try await SelectWidgetRangeIntent(range: .fToF).perform()

        XCTAssertEqual(PitchRange.fToF.rawValue, WidgetRangeState.rawValue)
    }

    func testWidgetIntentReturnsWhileToneIsActive() async throws {
        WidgetPitchState.set(nil)
        defer { WidgetPitchState.set(nil) }

        let intent = PlayWidgetPitchIntent(pitchIndex: 9, frequency: 440)
        let startedAt = Date()
        _ = try await intent.perform()

        XCTAssertLessThan(Date().timeIntervalSince(startedAt), PlayWidgetPitchIntent.duration)
        XCTAssertEqual(9, WidgetPitchState.activePitch)

        try await Task.sleep(for: .seconds(PlayWidgetPitchIntent.duration + 0.25))
        XCTAssertNil(WidgetPitchState.activePitch)
    }

    func testWidgetToneIsValidMonoPCM() {
        let tone = PlayWidgetPitchIntent.tone(frequency: 440, duration: 1.5)
        XCTAssertEqual("RIFF", String(data: tone.prefix(4), encoding: .utf8))
        XCTAssertEqual("WAVE", String(data: tone.dropFirst(8).prefix(4), encoding: .utf8))
        XCTAssertEqual(44 + 44_100 * 3, tone.count)
        XCTAssertFalse(PlayWidgetPitchIntent.openAppWhenRun)
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
