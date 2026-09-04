import AppIntents
import SwiftUI
import WidgetKit

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

private final class WidgetBundleToken {}

private enum WidgetArtwork {
    static let score: UIImage? = {
        let bundle = Bundle(for: WidgetBundleToken.self)
        guard let url = bundle.url(forResource: "panobackground", withExtension: "png") else {
            return nil
        }
        return UIImage(contentsOfFile: url.path)
    }()
}

private struct ScoreBackground: View {
    let palette: PlatePalette
    let dark: Bool

    var body: some View {
        GeometryReader { geometry in
            if let score = WidgetArtwork.score {
                let tileHeight = geometry.size.width * score.size.height / score.size.width
                let tileCount = max(1, Int(ceil(geometry.size.height / tileHeight)))
                VStack(spacing: 0) {
                    ForEach(0...tileCount, id: \.self) { _ in
                        Image(uiImage: score)
                            .renderingMode(.template)
                            .resizable()
                            .frame(width: geometry.size.width, height: tileHeight)
                            .foregroundStyle(palette.secondary)
                    }
                }
                .opacity(dark ? 0.14 : 0.12)
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
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
                ScoreBackground(palette: palette, dark: colorScheme == .dark)
                    .clipped()

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
