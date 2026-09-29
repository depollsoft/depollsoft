//
//  PitchPerfectNoteSoundTests.swift
//  pitchperfectTests
//
//  The Sound setting (docs/pitchperfect-note-sounds.md): the instrument
//  tuning table, the MIDI player's sampler pool, the setting and its sync,
//  the Settings row and list, and the widget's instruments.
//

import AVFoundation
import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

/// A sampler that records what it was asked to do.
private final class FakeSampler: NoteSampler {
    let index: Int
    var loads: [Int] = []
    var started: [InstrumentNote] = []
    var stopped: [Int] = []
    var failsToLoad = false
    init(index: Int) { self.index = index }

    func load(program: Int) throws {
        if failsToLoad { throw CocoaError(.fileReadCorruptFile) }
        loads.append(program)
    }
    func start(_ note: InstrumentNote) { started.append(note) }
    func stop(key: Int) { stopped.append(key) }
}

private final class FakeHost: NoteSamplerHost {
    var samplers: [FakeSampler] = []
    var runs = 0
    var onNeedsRestart: (() -> Void)?

    func makeSampler() -> NoteSampler? {
        let sampler = FakeSampler(index: samplers.count)
        samplers.append(sampler)
        return sampler
    }
    func ensureRunning() { runs += 1 }
}

private let tableJSON = """
{
 "firstKey": 24, "lastKey": 27,
 "ios": {
  "0": {"gainDb": -8.6, "correctionCents": [1.0, 2.0, 3.0, 4.0]},
  "19": {"gainDb": 8.3, "correctionCents": [-5.0, null, 0.5, 7.0]},
  "11": {"gainDb": 20.0, "correctionCents": [0, 0, 0, 0]}
 },
 "android": {}
}
"""

final class InstrumentTuningTests: XCTestCase {
    private func table() throws -> InstrumentTuning {
        try InstrumentTuning(data: Data(tableJSON.utf8))
    }

    func testANoteTakesItsKeysCorrectionAndItsInstrumentsGain() throws {
        let note = try XCTUnwrap(table().note(program: 19, key: 26, referencePitch: 440))
        XCTAssertEqual(note, InstrumentNote(program: 19, key: 26, pitchCents: 0.5, gainDb: 8.3))
    }

    func testTheTuningAddsItsOffsetFromA440() throws {
        let note = try XCTUnwrap(table().note(program: 19, key: 27, referencePitch: 430))
        XCTAssertEqual(note.pitchCents, 1200 * log2(430.0 / 440) + 7, accuracy: 1e-9)
    }

    func testKeysOutsideTheTableUseTheNearestEnd() throws {
        let low = try XCTUnwrap(table().note(program: 19, key: 12, referencePitch: 440))
        XCTAssertEqual(low.pitchCents, -5)
        XCTAssertEqual(low.key, 12, "the key itself still plays")
        let high = try XCTUnwrap(table().note(program: 0, key: 100, referencePitch: 440))
        XCTAssertEqual(high.pitchCents, 4)
    }

    func testASilentKeyPlaysThePiano() throws {
        let note = try XCTUnwrap(table().note(program: 19, key: 25, referencePitch: 440))
        XCTAssertEqual(note, InstrumentNote(program: 0, key: 25, pitchCents: 2, gainDb: -8.6))
    }

    func testGainStopsAtTheSamplersLimitAndUnknownProgramsDontPlay() throws {
        XCTAssertEqual(try table().note(program: 11, key: 24, referencePitch: 440)?.gainDb, 12)
        XCTAssertNil(try table().note(program: 73, key: 24, referencePitch: 440))
    }

    func testATableWithTheWrongNumberOfKeysIsRejected() {
        let bad = tableJSON.replacingOccurrences(of: "[1.0, 2.0, 3.0, 4.0]", with: "[1.0]")
        XCTAssertThrowsError(try InstrumentTuning(data: Data(bad.utf8)))
    }

    func testTheBundledTableCoversEveryInstrumentAndKey() throws {
        let table = try XCTUnwrap(InstrumentTuning.bundled)
        XCTAssertEqual(table.firstKey, 24)
        XCTAssertEqual(table.lastKey, 107)
        let programs = Set(DPNoteSound.instruments().map { Int(DPNoteSound.program(forSound: $0)) })
        XCTAssertEqual(Set(table.instruments.keys), programs)
        for (program, instrument) in table.instruments {
            XCTAssertEqual(instrument.correctionCents.count, 84, "program \(program)")
            // GeneralUser GS sounds every key of every instrument on iOS.
            XCTAssertFalse(instrument.correctionCents.contains { $0 == nil }, "program \(program)")
            XCTAssertLessThan(instrument.correctionCents.compactMap { $0 }.map(abs).max() ?? 0, 50)
        }
    }

    func testTheSoundBankShipsInTheApp() {
        XCTAssertNotNil(Bundle.main.url(forResource: "PitchPerfectInstruments", withExtension: "sf2"))
    }
}

final class MIDINotePlayerTests: XCTestCase {
    private var host: FakeHost!
    private var clock: TimeInterval = 100
    private var player: MIDINotePlayer!

    override func setUpWithError() throws {
        host = FakeHost()
        clock = 100
        player = MIDINotePlayer(host: host, tuning: try InstrumentTuning(data: Data(tableJSON.utf8)),
                                perform: { $0() }, now: { [unowned self] in clock })
        DPNote.referencePitch = 440
    }

    override func tearDown() {
        DPNote.instrumentPlayer = nil
        DPNote.sound = DPNoteSoundPitchPipe
        DPNote.referencePitch = 440
        super.tearDown()
    }

    /// The A440 frequency of MIDI key `key`.
    private func frequency(_ key: Int) -> Double { 440 * pow(2, Double(key - 69) / 12) }

    func testEachSoundingNoteGetsASamplerOfItsOwn() throws {
        let first = try XCTUnwrap(player.start(sound: "organ", a440Frequency: frequency(24)))
        let second = try XCTUnwrap(player.start(sound: "organ", a440Frequency: frequency(26)))
        XCTAssertEqual(host.samplers.count, 2)
        XCTAssertEqual(host.samplers.map(\.loads), [[19], [19]])
        XCTAssertEqual(host.samplers[0].started, [InstrumentNote(program: 19, key: 24, pitchCents: -5, gainDb: 8.3)])
        XCTAssertEqual(host.samplers[1].started.map(\.key), [26])
        XCTAssertEqual(host.runs, 2, "the output is started (or kept) running for each note")
        player.stopNote(first)
        player.stopNote(second)
        XCTAssertEqual(host.samplers.map(\.stopped), [[24], [26]])
        player.stopNote(first)
        XCTAssertEqual(host.samplers[0].stopped, [24], "a second stop does nothing")
    }

    func testASamplerIsReusedOnlyOnceItsReleaseHasFinished() throws {
        let note = try XCTUnwrap(player.start(sound: "organ", a440Frequency: frequency(24)))
        player.stopNote(note)
        clock += 1
        _ = player.start(sound: "organ", a440Frequency: frequency(26))
        XCTAssertEqual(host.samplers.count, 2, "the first is still releasing")
        clock += MIDINotePlayer.releaseTime
        _ = player.start(sound: "organ", a440Frequency: frequency(27))
        XCTAssertEqual(host.samplers.count, 2)
        XCTAssertEqual(host.samplers[0].started.map(\.key), [24, 27])
        XCTAssertEqual(host.samplers[0].loads, [19], "already loaded with the organ")
    }

    func testAnIdleSamplerWithTheRightInstrumentIsPreferred() throws {
        let piano = try XCTUnwrap(player.start(sound: "piano", a440Frequency: frequency(24)))
        let organ = try XCTUnwrap(player.start(sound: "organ", a440Frequency: frequency(24)))
        player.stopNote(piano)
        player.stopNote(organ)
        clock += 10
        _ = player.start(sound: "organ", a440Frequency: frequency(27))
        XCTAssertEqual(host.samplers[1].started.map(\.key), [24, 27])
        XCTAssertEqual(host.samplers[0].started.count, 1)
    }

    func testAChangeOfInstrumentReloadsTheSampler() throws {
        let organ = try XCTUnwrap(player.start(sound: "organ", a440Frequency: frequency(24)))
        player.stopNote(organ)
        clock += 10
        _ = player.start(sound: "piano", a440Frequency: frequency(24))
        XCTAssertEqual(host.samplers.count, 1)
        XCTAssertEqual(host.samplers[0].loads, [19, 0])
    }

    func testTheSilentKeysFallBackToThePianosSampler() throws {
        _ = try XCTUnwrap(player.start(sound: "organ", a440Frequency: frequency(25)))
        XCTAssertEqual(host.samplers[0].loads, [0])
        XCTAssertEqual(host.samplers[0].started.first?.program, 0)
    }

    func testThePoolHoldsThirteenAndThenTheLongestSoundingNoteGivesWay() throws {
        var tokens: [MIDINotePlayer.Token] = []
        for _ in 0..<MIDINotePlayer.maximumVoices {
            tokens.append(try XCTUnwrap(player.start(sound: "piano", a440Frequency: frequency(24))))
            clock += 0.01
        }
        XCTAssertEqual(host.samplers.count, 13)
        _ = player.start(sound: "piano", a440Frequency: frequency(27))
        XCTAssertEqual(host.samplers.count, 13)
        XCTAssertEqual(host.samplers[0].stopped, [24], "the first note gave up its sampler")
        XCTAssertEqual(host.samplers[0].started.map(\.key), [24, 27])
        player.stopNote(tokens[0])
        XCTAssertEqual(host.samplers[0].stopped, [24], "and its own stop no longer reaches it")
    }

    func testPreparingLoadsTheIdleSamplersButNotASoundingOne() throws {
        player.prepare(sound: "organ")
        XCTAssertEqual(host.samplers.map(\.loads), [[19]], "one sampler is made ready")
        let note = try XCTUnwrap(player.start(sound: "organ", a440Frequency: frequency(24)))
        _ = player.start(sound: "organ", a440Frequency: frequency(26))
        player.stopNote(note)
        player.prepare(sound: "flute")
        XCTAssertEqual(host.samplers.map(\.loads), [[19, 73], [19]])
        XCTAssertEqual(player.voiceStates.map(\.sounding), [false, true])
        player.prepare(sound: "square")
        XCTAssertEqual(host.samplers.map(\.loads), [[19, 73], [19]], "a wave loads nothing")
    }

    func testAnInstrumentThatWontLoadStaysSilent() throws {
        host = FakeHost()
        player = MIDINotePlayer(host: host, tuning: try InstrumentTuning(data: Data(tableJSON.utf8)),
                                perform: { $0() }, now: { [unowned self] in clock })
        player.prepare(sound: "organ")
        host.samplers[0].failsToLoad = true
        player.prepare(sound: "piano")
        _ = player.start(sound: "piano", a440Frequency: frequency(24))
        XCTAssertTrue(host.samplers[0].started.isEmpty)
    }

    func testWithoutAnEngineOrForAWaveNothingStarts() throws {
        let silent = MIDINotePlayer(host: nil, tuning: try InstrumentTuning(data: Data(tableJSON.utf8)), perform: { $0() })
        XCTAssertNil(silent.start(sound: "organ", a440Frequency: 440))
        XCTAssertNil(player.start(sound: "sine", a440Frequency: 440))
        XCTAssertNil(player.start(sound: "pitchPipe", a440Frequency: 440))
    }

    func testTheOutputRestartsInTurnAfterAnInterruption() {
        host.onNeedsRestart?()
        XCTAssertEqual(host.runs, 1)
    }

    func testANotePlaysThroughThePlayerAtItsKeyAndTuning() throws {
        let bundled = try XCTUnwrap(InstrumentTuning.bundled)
        let host = FakeHost()
        let player = MIDINotePlayer(host: host, tuning: bundled, perform: { $0() })
        DPNote.instrumentPlayer = player
        DPNote.sound = "choir"
        DPNote.referencePitch = 442
        let c4 = try XCTUnwrap(DPNote(friendlyName: "C", octave: 4, accidental: DPNote.c4().accidental, frequency: DPNote.c4().frequency))
        c4.play()
        XCTAssertTrue(c4.isPlaying)
        let started = try XCTUnwrap(host.samplers.first?.started.first)
        let correction = try XCTUnwrap(bundled.instruments[52]?.correctionCents[60 - 24] ?? nil)
        XCTAssertEqual(started.program, 52)
        XCTAssertEqual(started.key, 60)
        XCTAssertEqual(started.pitchCents, 1200 * log2(442.0 / 440) + correction, accuracy: 1e-9)
        c4.stop()
        XCTAssertEqual(host.samplers.first?.stopped, [60])
    }
}

@MainActor
final class NoteSoundSettingTests: PitchPerfectTestCase {
    func testThePitchPipeIsTheDefault() {
        XCTAssertEqual(DPSettingsModel.sharedInstance.noteSound, "pitchPipe")
        UserDefaults.standard.removeObject(forKey: "depollsoft.pitchperfect.NoteSound")
        XCTAssertEqual(DPSettingsModel.sharedInstance.noteSound, "pitchPipe")
        XCTAssertEqual(SettingsModel(account: AccountService(isSignedIn: { false }, userDescription: { "" }, signOut: {}, deleteAccount: { _ in })).noteSound, "pitchPipe")
    }

    func testChoosingASoundStoresItAndTellsTheNotesAndTheWidget() {
        let widgetSound = WidgetSoundState.sound
        defer { WidgetSoundState.set(widgetSound) }
        DPSettingsModel.sharedInstance.noteSound = "strings"
        XCTAssertEqual(UserDefaults.standard.string(forKey: "depollsoft.pitchperfect.NoteSound"), "strings")
        XCTAssertEqual(DPNote.sound, "strings")
        XCTAssertEqual(WidgetSoundState.sound, "strings")
    }

    func testAnUnknownSoundIsRefusedAndAStoredOneReadsAsThePitchPipe() {
        DPSettingsModel.sharedInstance.noteSound = "square"
        DPSettingsModel.sharedInstance.noteSound = "harmonica"
        XCTAssertEqual(DPSettingsModel.sharedInstance.noteSound, "square")
        UserDefaults.standard.set("kazoo", forKey: "depollsoft.pitchperfect.NoteSound")
        XCTAssertEqual(DPSettingsModel.sharedInstance.noteSound, "pitchPipe")
    }

    func testASyncedSoundIsAppliedAndOneThisVersionDoesntKnowPlaysThePitchPipe() {
        let settings = DPSettingsModel.sharedInstance
        settings.applyRemote(wakeLock: nil, toggleNotes: nil, referencePitch: nil, noteSound: "choir")
        XCTAssertEqual(settings.noteSound, "choir")
        XCTAssertEqual(DPNote.sound, "choir")
        settings.applyRemote(wakeLock: nil, toggleNotes: nil, referencePitch: nil, noteSound: "theremin")
        XCTAssertEqual(settings.noteSound, "pitchPipe")
        XCTAssertEqual(DPNote.sound, "pitchPipe")
    }

    func testThePickersSectionsAndLabels() {
        let sections = DPSettingsModel.noteSoundSections
        XCTAssertEqual(sections.map(\.title), [nil, "Waves", "Instruments"])
        XCTAssertEqual(sections.flatMap(\.sounds), DPNoteSound.allSounds())
        XCTAssertEqual(DPSettingsModel.noteSoundLabel("pitchPipe"), "Pitch Perfect (Loud)")
        XCTAssertEqual(DPSettingsModel.noteSoundLabel("electricPiano"), "Electric Piano")
        for sound in DPNoteSound.allSounds() {
            XCTAssertNotEqual(DPSettingsModel.noteSoundLabel(sound), sound, sound)
        }
    }

    func testChoosingASoundInSettingsPlaysAPreview() {
        var played: [String] = []
        var stops = 0
        let model = SettingsModel(account: AccountService(isSignedIn: { false }, userDescription: { "" }, signOut: {}, deleteAccount: { _ in }),
                                  preview: SoundPreview(play: { played.append($0) }, stop: { stops += 1 }))
        model.chooseNoteSound("vibraphone")
        XCTAssertEqual(model.noteSound, "vibraphone")
        XCTAssertEqual(played, ["vibraphone"])
        model.stopPreview()
        XCTAssertEqual(stops, 1)
    }

    func testTheSoundRowShowsTheChoiceAndOpensTheList() throws {
        DPSettingsModel.sharedInstance.noteSound = "organ"
        let app = try launch()
        app.ui.tap(id: "gearshape")
        settle { app.sheet.exists(id: "settings.sound") }
        XCTAssertEqual(app.sheet.label(id: "settings.sound"), "Sound")
        XCTAssertEqual(app.sheet.value(id: "settings.sound"), "Organ")
        app.sheet.tap(id: "settings.sound")
        settle { app.navigationTitles.contains("Sound") && app.sheet.exists(id: "sound.sine") }
        XCTAssertTrue(app.sheet.isSelected(id: "sound.organ"))
        XCTAssertFalse(app.sheet.isSelected(id: "sound.pitchPipe"))
        for header in ["Waves", "Instruments"] {
            XCTAssertTrue(app.sheet.exists(label: header), header)
        }
        XCTAssertEqual(app.sheet.label(id: "sound.pitchPipe"), "Pitch Perfect (Loud)")
        app.sheet.tap(id: "sound.sine")
        settle { app.sheet.isSelected(id: "sound.sine") }
        XCTAssertEqual(DPSettingsModel.sharedInstance.noteSound, "sine")
        XCTAssertFalse(app.sheet.isSelected(id: "sound.organ"))
        XCTAssertTrue(app.navigationTitles.contains("Sound"), "choosing stays on the list")
    }
}

@MainActor
final class WidgetNoteSoundTests: XCTestCase {
    private var savedSound = WidgetSoundState.pitchPipe

    override func setUp() {
        super.setUp()
        savedSound = WidgetSoundState.sound
        WidgetTonePlayer.shared.stop()
    }

    override func tearDown() {
        WidgetTonePlayer.shared.stop()
        WidgetInstrumentHook.start = nil
        WidgetInstrumentHook.stop = nil
        WidgetSoundState.set(savedSound)
        DPNote.instrumentPlayer = nil
        super.tearDown()
    }

    func testAnInstrumentCellPlaysThroughTheAppsPlayer() throws {
        let host = FakeHost()
        let player = MIDINotePlayer(host: host, tuning: try XCTUnwrap(InstrumentTuning.bundled), perform: { $0() })
        MIDINotePlayer.install(player)
        XCTAssertTrue(DPNote.instrumentPlayer === player)
        WidgetSoundState.set("trumpet")
        let widget = WidgetTonePlayer.shared
        try widget.toggle(pitchIndex: 9, frequency: 440)
        XCTAssertEqual(widget.activePitches, [9])
        XCTAssertEqual(host.samplers.first?.started.map(\.program), [56])
        XCTAssertEqual(host.samplers.first?.started.map(\.key), [69])
        try widget.toggle(pitchIndex: 9, frequency: 440)
        XCTAssertTrue(widget.activePitches.isEmpty)
        XCTAssertEqual(host.samplers.first?.stopped, [69])
    }

    func testTheWidgetsKeyComesFromTheTunedFrequency() throws {
        let host = FakeHost()
        let player = MIDINotePlayer(host: host, tuning: try XCTUnwrap(InstrumentTuning.bundled), perform: { $0() })
        MIDINotePlayer.install(player)
        DPNote.referencePitch = 415
        defer { DPNote.referencePitch = 440 }
        WidgetSoundState.set("flute")
        try WidgetTonePlayer.shared.toggle(pitchIndex: 9, frequency: 415)
        XCTAssertEqual(host.samplers.first?.started.first?.key, 69)
    }

    func testStoppingTheWidgetStopsItsInstrumentNotes() throws {
        var stopped = 0
        WidgetInstrumentHook.start = { _, _ in NSObject() }
        WidgetInstrumentHook.stop = { _ in stopped += 1 }
        WidgetSoundState.set("piano")
        try WidgetTonePlayer.shared.toggle(pitchIndex: 0, frequency: 261.63)
        try WidgetTonePlayer.shared.toggle(pitchIndex: 4, frequency: 329.63)
        WidgetTonePlayer.shared.stop()
        XCTAssertEqual(stopped, 2)
        XCTAssertTrue(WidgetTonePlayer.shared.activePitches.isEmpty)
    }

    /// The 16-bit samples of a widget tone.
    private func pcm(_ data: Data) -> [Int16] {
        data.dropFirst(44).withUnsafeBytes { Array($0.bindMemory(to: Int16.self)) }
    }

    func testAWaveLoopsSeamlesslyAtTheAppsLevel() {
        for sound in DPNoteSound.waves() {
            let samples = pcm(PlayWidgetPitchIntent.loopingTone(frequency: 440.1, sound: sound))
            XCTAssertEqual(samples.count, 44_100 * 4)
            let peak = samples.map { abs(Int($0)) }.max() ?? 0
            XCTAssertLessThanOrEqual(peak, Int(0.89 * 32767) + 1, sound)
            XCTAssertGreaterThan(peak, Int(0.85 * 32767), sound)
            // The loop point continues the wave: the jump from the last sample
            // back to the first is no bigger than the wave's own steps.
            let steps = zip(samples, samples.dropFirst()).map { abs(Int($0) - Int($1)) }
            let wrap = abs(Int(samples.last!) - Int(samples.first!))
            XCTAssertLessThanOrEqual(wrap, steps.max()!, sound)
        }
    }

    func testThePitchPipeToneIsUnchanged() {
        let samples = pcm(PlayWidgetPitchIntent.loopingTone(frequency: 440, sound: "pitchPipe"))
        for frame in [0, 7, 100, 4410] {
            XCTAssertEqual(samples[frame], PlayWidgetPitchIntent.pitchPipeSample(frequency: 440, time: Double(frame) / 44_100))
        }
        XCTAssertEqual(pcm(PlayWidgetPitchIntent.loopingTone(frequency: 440, sound: "piano")), samples,
                       "an instrument the app can't play falls back to the pitch pipe")
    }

    func testTheWidgetsWavesMatchTheApps() {
        // The widget can't link pitchperfectlib, so it has its own copy of the formulas.
        for (index, sound) in DPNoteSound.waves().enumerated() {
            var state = DPWaveStateMake(DPWaveShape(rawValue: Int32(index))!, 440.1, 44_100)
            var app = [Float](repeating: 0, count: 2000)
            DPWaveRender(&state, &app, 2000)
            let widget = PlayWidgetPitchIntent.samples(frequency: 440.1, frames: 2000, sampleRate: 44_100, sound: sound)
            for frame in 220..<2000 {
                XCTAssertEqual(widget[frame] / Double(Int16.max), Double(app[frame]), accuracy: 1e-5, "\(sound) \(frame)")
            }
        }
    }
}

/// The bundled sound bank through a real AVAudioUnitSampler, rendered offline:
/// no audio hardware is involved.
final class SoundBankRenderingTests: XCTestCase {
    func testEveryInstrumentLoadsFromTheBundledBankAndSounds() throws {
        let bank = try XCTUnwrap(Bundle.main.url(forResource: "PitchPerfectInstruments", withExtension: "sf2"))
        for sound in DPNoteSound.instruments() {
            let program = UInt8(DPNoteSound.program(forSound: sound))
            let engine = AVAudioEngine()
            let sampler = AVAudioUnitSampler()
            engine.attach(sampler)
            let format = try XCTUnwrap(AVAudioFormat(standardFormatWithSampleRate: 44_100, channels: 2))
            try engine.enableManualRenderingMode(.offline, format: format, maximumFrameCount: 4096)
            engine.connect(sampler, to: engine.mainMixerNode, format: nil)
            try sampler.loadSoundBankInstrument(at: bank, program: program,
                                                bankMSB: UInt8(kAUSampler_DefaultMelodicBankMSB),
                                                bankLSB: UInt8(kAUSampler_DefaultBankLSB))
            try engine.start()
            sampler.startNote(60, withVelocity: 100, onChannel: 0)
            let buffer = try XCTUnwrap(AVAudioPCMBuffer(pcmFormat: engine.manualRenderingFormat, frameCapacity: 4096))
            var peak: Float = 0
            for _ in 0..<8 {
                XCTAssertEqual(try engine.renderOffline(4096, to: buffer), .success)
                let left = try XCTUnwrap(buffer.floatChannelData?[0])
                for frame in 0..<Int(buffer.frameLength) { peak = max(peak, abs(left[frame])) }
            }
            engine.stop()
            XCTAssertGreaterThan(peak, 0.01, sound)
        }
    }
}
