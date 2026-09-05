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

    func testProductionBuildResolvesProductionGroup() {
        // Unsigned test hosts hold no group entitlement, so this exercises the fallback.
        XCTAssertEqual(WidgetSharedDefaults.productionGroup, WidgetSharedDefaults.suiteName)
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

    func testWidgetRangeStatePersistsAndClears() {
        let previousRange = WidgetRangeState.rawValue
        defer { WidgetRangeState.set(previousRange) }

        WidgetRangeState.set(PitchRange.fToF.rawValue)
        XCTAssertEqual(PitchRange.fToF.rawValue, WidgetRangeState.rawValue)
        WidgetRangeState.set(nil)
        XCTAssertNil(WidgetRangeState.rawValue)
    }

    func testWidgetPitchTapStartsSwitchesAndStopsTone() async throws {
        WidgetPitchState.set(nil)

        _ = try await PlayWidgetPitchIntent(pitchIndex: 9, frequency: 440, playing: true).perform()
        XCTAssertEqual(9, WidgetPitchState.activePitch)

        _ = try await PlayWidgetPitchIntent(pitchIndex: 10, frequency: 466.16, playing: true).perform()
        XCTAssertEqual(10, WidgetPitchState.activePitch)

        _ = try await PlayWidgetPitchIntent(pitchIndex: 10, frequency: 466.16, playing: false).perform()
        XCTAssertNil(WidgetPitchState.activePitch)
    }

    func testWidgetToneIsValidMonoPCM() {
        let tone = PlayWidgetPitchIntent.tone(frequency: 440, duration: 1.5)
        XCTAssertEqual("RIFF", String(data: tone.prefix(4), encoding: .utf8))
        XCTAssertEqual("WAVE", String(data: tone.dropFirst(8).prefix(4), encoding: .utf8))
        XCTAssertEqual(44 + 44_100 * 3, tone.count)

        let loopingTone = PlayWidgetPitchIntent.loopingTone(frequency: 440)
        XCTAssertEqual(
            44 + Int(44_100 * PlayWidgetPitchIntent.loopDuration) * 2,
            loopingTone.count
        )
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
