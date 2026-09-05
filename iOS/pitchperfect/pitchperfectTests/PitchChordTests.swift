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

    @MainActor
    func testWidgetPitchesKeepPlayingAndToggleIndependently() async throws {
        let player = WidgetTonePlayer.shared
        player.stop()
        defer { player.stop() }

        _ = try await PlayWidgetPitchIntent(pitchIndex: 9, frequency: 440).perform()
        XCTAssertEqual([9], WidgetPitchState.activePitches)
        _ = try await PlayWidgetPitchIntent(pitchIndex: 10, frequency: 466.16).perform()
        XCTAssertEqual([9, 10], WidgetPitchState.activePitches)
        XCTAssertEqual([9, 10], player.activePitches, "Both actual audio players must be sounding")

        try await Task.sleep(for: .seconds(PlayWidgetPitchIntent.loopDuration + 1))
        XCTAssertEqual([9, 10], player.activePitches, "Both tones must outlive the loop buffer")
        XCTAssertEqual(player.activePitches, WidgetPitchState.activePitches)

        _ = try await PlayWidgetPitchIntent(pitchIndex: 10, frequency: 466.16).perform()
        XCTAssertEqual([9], player.activePitches, "Stopping one note must leave the other sounding")
        XCTAssertEqual([9], WidgetPitchState.activePitches)
        _ = try await PlayWidgetPitchIntent(pitchIndex: 9, frequency: 440).perform()
        XCTAssertTrue(player.activePitches.isEmpty)
        XCTAssertTrue(WidgetPitchState.activePitches.isEmpty)
    }

    @MainActor
    func testWidgetFailedNoteDoesNotStopExistingVoices() async throws {
        let player = WidgetTonePlayer.shared
        player.stop()
        defer { player.stop() }
        _ = try await PlayWidgetPitchIntent(pitchIndex: 9, frequency: 440).perform()
        do {
            _ = try await PlayWidgetPitchIntent(pitchIndex: 10, frequency: .nan).perform()
            XCTFail("Invalid frequency should fail without disturbing existing voices")
        } catch {
            XCTAssertEqual([9], player.activePitches)
            XCTAssertEqual([9], WidgetPitchState.activePitches)
        }
    }

    @MainActor
    func testWidgetConcurrentNotesPublishCompleteSnapshotAndStopAll() async throws {
        let player = WidgetTonePlayer.shared
        player.stop()
        defer { player.stop() }
        try await withThrowingTaskGroup(of: Void.self) { group in
            for index in 0..<13 {
                group.addTask {
                    let frequency = 261.63 * pow(2, Double(index) / 12)
                    _ = try await PlayWidgetPitchIntent(pitchIndex: index, frequency: frequency).perform()
                }
            }
            try await group.waitForAll()
        }
        XCTAssertEqual(Set(0..<13), player.activePitches)
        XCTAssertEqual(player.activePitches, WidgetPitchState.activePitches)
        player.stop()
        XCTAssertTrue(player.activePitches.isEmpty)
        XCTAssertTrue(WidgetPitchState.activePitches.isEmpty)
    }

    func testWidgetPitchSetPersistsAndClearsLegacyState() throws {
        let defaults = try XCTUnwrap(WidgetSharedDefaults.defaults)
        let previous = WidgetPitchState.activePitches
        defer { WidgetPitchState.set(previous) }
        defaults.removeObject(forKey: "activePitches")
        defaults.set(9, forKey: "activePitch")
        XCTAssertEqual([9], WidgetPitchState.activePitches)
        WidgetPitchState.set([0, 4, 7, -1, 13])
        XCTAssertEqual([0, 4, 7], WidgetPitchState.activePitches)
        XCTAssertNil(defaults.object(forKey: "activePitch"))
        WidgetPitchState.set([])
        XCTAssertTrue(WidgetPitchState.activePitches.isEmpty)
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
