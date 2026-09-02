import AppIntents
import AVFoundation
import SwiftUI
import WidgetKit

private let widgetKind = "PitchPerfectPitchPipe"

enum PitchRange: String, AppEnum {
    case cToC
    case fToF

    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "Octave Range")
    static let caseDisplayRepresentations: [PitchRange: DisplayRepresentation] = [
        .cToC: "C to C",
        .fToF: "F to F",
    ]
}

struct PitchWidgetConfiguration: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "Pitch Pipe Range"
    static let description = IntentDescription("Choose the inclusive octave shown by this widget.")

    @Parameter(title: "Range", default: .cToC)
    var range: PitchRange
}

private struct PitchEntry: TimelineEntry {
    let date: Date
    let range: PitchRange
    let activePitch: Int?
}

private struct PitchProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> PitchEntry {
        PitchEntry(date: .now, range: .cToC, activePitch: nil)
    }

    func snapshot(for configuration: PitchWidgetConfiguration, in context: Context) async -> PitchEntry {
        PitchEntry(
            date: .now,
            range: configuration.range,
            activePitch: WidgetPitchState.activePitch
        )
    }

    func timeline(for configuration: PitchWidgetConfiguration, in context: Context) async -> Timeline<PitchEntry> {
        Timeline(
            entries: [
                PitchEntry(
                    date: .now,
                    range: configuration.range,
                    activePitch: WidgetPitchState.activePitch
                )
            ],
            policy: .never
        )
    }
}

private struct Pitch: Identifiable {
    let id: Int
    let name: String
    let accidental: String
    let octave: Int
    let frequency: Double

    var engraved: String { accidental == "natural" ? name : "♯/♭" }
    var spoken: String {
        accidental == "natural" ? "\(name), octave \(octave)" : "\(name) sharp, octave \(octave)"
    }


}

private enum WidgetPitchState {
    static let key = "activePitch"

    static var activePitch: Int? {
        guard UserDefaults.standard.object(forKey: key) != nil else { return nil }
        let value = UserDefaults.standard.integer(forKey: key)
        return value >= 0 ? value : nil
    }

    static func set(_ value: Int?) {
        UserDefaults.standard.set(value ?? -1, forKey: key)
    }
}

@MainActor
private final class WidgetTonePlayer {
    static let shared = WidgetTonePlayer()

    private var player: AVAudioPlayer?
    private var stopTask: Task<Void, Never>?

    func play(frequency: Double) throws {
        stopTask?.cancel()
        player?.stop()
        player = try AVAudioPlayer(
            data: PlayWidgetPitchIntent.tone(frequency: frequency, duration: 1.5)
        )
        player?.prepareToPlay()
        player?.play()

        stopTask = Task { @MainActor [weak self] in
            try? await Task.sleep(for: .seconds(1.5))
            guard !Task.isCancelled else { return }
            self?.player?.stop()
            self?.player = nil
            WidgetPitchState.set(nil)
            WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
        }
    }
}

struct PlayWidgetPitchIntent: AudioPlaybackIntent {
    static let title: LocalizedStringResource = "Sound Pitch"
    static let openAppWhenRun = false

    @Parameter(title: "Pitch")
    var pitchIndex: Int

    @Parameter(title: "Frequency")
    var frequency: Double

    init() {}

    init(pitchIndex: Int, frequency: Double) {
        self.pitchIndex = pitchIndex
        self.frequency = frequency
    }

    func perform() async throws -> some IntentResult {
        WidgetPitchState.set(pitchIndex)
        WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
        try await MainActor.run {
            try WidgetTonePlayer.shared.play(frequency: frequency)
        }
        // Returning immediately lets WidgetKit apply the active timeline while
        // AudioPlaybackIntent keeps the short tone alive in the extension.
        return .result()
    }

    fileprivate static func tone(frequency: Double, duration: Double) -> Data {
        let sampleRate = 44_100
        let frames = Int(Double(sampleRate) * duration)
        var pcm = Data(capacity: frames * 2)
        let fadeFrames = sampleRate / 40
        for frame in 0..<frames {
            let attack = min(1.0, Double(frame) / Double(fadeFrames))
            let release = min(1.0, Double(frames - frame) / Double(fadeFrames))
            let envelope = min(attack, release)
            let sample = sin(2 * .pi * frequency * Double(frame) / Double(sampleRate))
            var value = Int16(sample * envelope * 9_000).littleEndian
            withUnsafeBytes(of: &value) { pcm.append(contentsOf: $0) }
        }

        var data = Data("RIFF".utf8)
        var riffSize = UInt32(36 + pcm.count).littleEndian
        withUnsafeBytes(of: &riffSize) { data.append(contentsOf: $0) }
        data.append(Data("WAVEfmt ".utf8))
        var formatSize = UInt32(16).littleEndian
        var audioFormat = UInt16(1).littleEndian
        var channels = UInt16(1).littleEndian
        var rate = UInt32(sampleRate).littleEndian
        var byteRate = UInt32(sampleRate * 2).littleEndian
        var blockAlign = UInt16(2).littleEndian
        var bits = UInt16(16).littleEndian
        withUnsafeBytes(of: &formatSize) { data.append(contentsOf: $0) }
        withUnsafeBytes(of: &audioFormat) { data.append(contentsOf: $0) }
        withUnsafeBytes(of: &channels) { data.append(contentsOf: $0) }
        withUnsafeBytes(of: &rate) { data.append(contentsOf: $0) }
        withUnsafeBytes(of: &byteRate) { data.append(contentsOf: $0) }
        withUnsafeBytes(of: &blockAlign) { data.append(contentsOf: $0) }
        withUnsafeBytes(of: &bits) { data.append(contentsOf: $0) }
        data.append(Data("data".utf8))
        var dataSize = UInt32(pcm.count).littleEndian
        withUnsafeBytes(of: &dataSize) { data.append(contentsOf: $0) }
        data.append(pcm)
        return data
    }
}

private struct PitchSeed {
    let name: String
    let accidental: String
    let octave: Int
    let frequency: Double
}

private enum PitchCatalog {
    static func notes(for range: PitchRange) -> [Pitch] {
        let seeds: [PitchSeed]
        switch range {
        case .cToC:
            seeds = [
                PitchSeed(name: "C", accidental: "natural", octave: 4, frequency: 261.63),
                PitchSeed(name: "C", accidental: "sharp", octave: 4, frequency: 277.18),
                PitchSeed(name: "D", accidental: "natural", octave: 4, frequency: 293.66),
                PitchSeed(name: "D", accidental: "sharp", octave: 4, frequency: 311.13),
                PitchSeed(name: "E", accidental: "natural", octave: 4, frequency: 329.63),
                PitchSeed(name: "F", accidental: "natural", octave: 4, frequency: 349.23),
                PitchSeed(name: "F", accidental: "sharp", octave: 4, frequency: 369.99),
                PitchSeed(name: "G", accidental: "natural", octave: 4, frequency: 392.00),
                PitchSeed(name: "G", accidental: "sharp", octave: 4, frequency: 415.30),
                PitchSeed(name: "A", accidental: "natural", octave: 4, frequency: 440.00),
                PitchSeed(name: "A", accidental: "sharp", octave: 4, frequency: 466.16),
                PitchSeed(name: "B", accidental: "natural", octave: 4, frequency: 493.88),
                PitchSeed(name: "C", accidental: "natural", octave: 5, frequency: 523.25),
            ]
        case .fToF:
            seeds = [
                PitchSeed(name: "F", accidental: "natural", octave: 4, frequency: 349.23),
                PitchSeed(name: "F", accidental: "sharp", octave: 4, frequency: 369.99),
                PitchSeed(name: "G", accidental: "natural", octave: 4, frequency: 392.00),
                PitchSeed(name: "G", accidental: "sharp", octave: 4, frequency: 415.30),
                PitchSeed(name: "A", accidental: "natural", octave: 4, frequency: 440.00),
                PitchSeed(name: "A", accidental: "sharp", octave: 4, frequency: 466.16),
                PitchSeed(name: "B", accidental: "natural", octave: 4, frequency: 493.88),
                PitchSeed(name: "C", accidental: "natural", octave: 5, frequency: 523.25),
                PitchSeed(name: "C", accidental: "sharp", octave: 5, frequency: 554.37),
                PitchSeed(name: "D", accidental: "natural", octave: 5, frequency: 587.33),
                PitchSeed(name: "D", accidental: "sharp", octave: 5, frequency: 622.25),
                PitchSeed(name: "E", accidental: "natural", octave: 5, frequency: 659.25),
                PitchSeed(name: "F", accidental: "natural", octave: 5, frequency: 698.46),
            ]
        }
        return seeds.enumerated().map { index, seed in
            Pitch(
                id: index,
                name: seed.name,
                accidental: seed.accidental,
                octave: seed.octave,
                frequency: seed.frequency
            )
        }
    }
}

private struct PlatePalette {
    let ground: Color
    let surface: Color
    let ink: Color
    let secondary: Color
    let hairline: Color

    init(dark: Bool) {
        ground = Color(hex: dark ? 0x0E0F10 : 0xDADBDC)
        surface = Color(hex: dark ? 0x16181A : 0xE7E8E9)
        ink = Color(hex: dark ? 0xD9DBDD : 0x1C1E20)
        secondary = Color(hex: dark ? 0x898D92 : 0x55585C)
        hairline = Color(hex: dark ? 0x2C2F33 : 0xB7B9BC)
    }
}

private extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: 1
        )
    }
}

private struct PitchFace: View {
    @Environment(\.colorScheme) private var colorScheme
    let entry: PitchEntry

    var body: some View {
        GeometryReader { geometry in
            let palette = PlatePalette(dark: colorScheme == .dark)
            let size = geometry.size
            let center = CGPoint(x: size.width / 2, y: size.height * 0.43)
            let ring = min(size.width, size.height * 0.82) * 0.365
            let cellRadius = ring * 0.225
            let pitches = PitchCatalog.notes(for: entry.range)
            let step = 360.0 / Double(pitches.count)
            let start = -90.0 + step / 2

            ZStack {
                palette.ground
                Image("panobackground", bundle: .main)
                    .resizable()
                    .scaledToFill()
                    .colorMultiply(palette.secondary)
                    .opacity(colorScheme == .dark ? 0.14 : 0.12)
                    .clipped()
                    .allowsHitTesting(false)

                ForEach(pitches) { pitch in
                    let angle = (start + Double(pitch.id) * step) * .pi / 180
                    Button(
                        intent: PlayWidgetPitchIntent(
                            pitchIndex: pitch.id,
                            frequency: pitch.frequency
                        )
                    ) {
                        let active = entry.activePitch == pitch.id
                        ZStack {
                            if active {
                                Circle()
                                    .fill(palette.ink.opacity(0.16))
                                    .blur(radius: cellRadius * 0.35)
                                    .scaleEffect(1.45)
                            }
                            Circle().fill(active ? palette.ink : palette.surface)
                            Circle().stroke(active ? palette.ink : palette.hairline, lineWidth: active ? 2 : 1.25)
                            Circle()
                                .stroke(
                                    active ? palette.ground.opacity(0.45) : palette.secondary.opacity(0.28),
                                    lineWidth: 0.7
                                )
                                .padding(cellRadius * 0.14)
                            Text(pitch.engraved)
                                .font(
                                    .custom(
                                        "Oswald-Medium",
                                        size: cellRadius * (pitch.accidental == "natural" ? 0.88 : 0.58)
                                    )
                                )
                                .foregroundStyle(
                                    active
                                        ? palette.ground
                                        : (pitch.accidental == "natural" ? palette.ink : palette.secondary)
                                )
                        }
                        .frame(width: cellRadius * 2, height: cellRadius * 2)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(pitch.spoken)
                    .position(
                        x: center.x + CGFloat(cos(angle)) * ring,
                        y: center.y + CGFloat(sin(angle)) * ring
                    )
                }

                centerReadout(
                    pitches: pitches,
                    palette: palette,
                    center: center,
                    ring: ring
                )

                rangeSelector(palette: palette, center: center, ring: ring)

                Text("DIGITAL PITCH PIPE")
                    .font(.custom("Oswald-Medium", size: ring * 0.075))
                    .tracking(ring * 0.025)
                    .foregroundStyle(palette.secondary.opacity(0.68))
                    .position(x: center.x, y: size.height * 0.91)
            }
            .clipShape(ContainerRelativeShape())
        }
        .containerBackground(for: .widget) { Color.clear }
    }

    @ViewBuilder
    private func centerReadout(
        pitches: [Pitch],
        palette: PlatePalette,
        center: CGPoint,
        ring: CGFloat
    ) -> some View {
        if let index = entry.activePitch, pitches.indices.contains(index) {
            let pitch = pitches[index]
            VStack(spacing: ring * 0.015) {
                Text("\(pitch.name)\(pitch.accidental == "natural" ? "" : "♯")\(pitch.octave)")
                    .font(.custom("Oswald-Medium", size: ring * 0.25))
                    .foregroundStyle(palette.ink)
                Text(String(format: "%.1f Hz", pitch.frequency))
                    .font(.system(size: ring * 0.11, design: .monospaced))
                    .foregroundStyle(palette.ink)
            }
            .position(x: center.x, y: center.y - ring * 0.07)
        } else {
            Text("— Hz")
                .font(.system(size: ring * 0.14, design: .monospaced))
                .foregroundStyle(palette.secondary.opacity(0.58))
                .position(x: center.x, y: center.y - ring * 0.04)
        }
    }

    private func rangeSelector(palette: PlatePalette, center: CGPoint, ring: CGFloat) -> some View {
        let width = ring * 0.78
        let rowHeight = ring * 0.145
        return VStack(spacing: 0) {
            rangeRow("C TO C", selected: entry.range == .cToC, palette: palette, height: rowHeight)
            Divider().overlay(palette.hairline)
            rangeRow("F TO F", selected: entry.range == .fToF, palette: palette, height: rowHeight)
        }
        .frame(width: width, height: rowHeight * 2)
        .background(palette.surface.opacity(0.94))
        .overlay(RoundedRectangle(cornerRadius: 3).stroke(palette.hairline, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 3))
        .position(x: center.x, y: center.y + ring * 0.20 + rowHeight)
    }

    private func rangeRow(_ label: String, selected: Bool, palette: PlatePalette, height: CGFloat) -> some View {
        HStack(spacing: height * 0.14) {
            Circle()
                .fill(selected ? palette.ink : .clear)
                .frame(width: height * 0.22, height: height * 0.22)
            Text(label)
                .font(.custom("Oswald-Medium", size: height * 0.46))
                .tracking(height * 0.11)
                .foregroundStyle(selected ? palette.ink : palette.secondary.opacity(0.76))
        }
        .frame(maxWidth: .infinity, minHeight: height, maxHeight: height)
        .background(selected ? palette.ink.opacity(0.10) : .clear)
    }
}

@main
struct PitchPerfectWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: widgetKind, intent: PitchWidgetConfiguration.self, provider: PitchProvider()) { entry in
            PitchFace(entry: entry)
        }
        .configurationDisplayName("Pitch Pipe")
        .description("A full inclusive octave on the same circular instrument face as Pitch Perfect.")
        .supportedFamilies([.systemLarge])
        .contentMarginsDisabled()
    }
}
