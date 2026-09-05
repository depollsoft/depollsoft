import AppIntents
import SwiftUI
import WidgetKit

struct PitchWidgetConfiguration: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "Pitch Pipe Range"
    static let description = IntentDescription("Choose the inclusive octave shown by this widget.")

    @Parameter(title: "Range", default: .cToC)
    var range: PitchRange
}

private struct PitchEntry: TimelineEntry {
    let date: Date
    let range: PitchRange
    let activePitches: Set<Int>
}

private struct PitchProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> PitchEntry {
        PitchEntry(date: .now, range: .cToC, activePitches: [])
    }

    func snapshot(for configuration: PitchWidgetConfiguration, in context: Context) async -> PitchEntry {
        entry(for: configuration)
    }

    func timeline(for configuration: PitchWidgetConfiguration, in context: Context) async -> Timeline<PitchEntry> {
        Timeline(entries: [entry(for: configuration)], policy: .never)
    }

    private func entry(for configuration: PitchWidgetConfiguration) -> PitchEntry {
        let selectedRange = WidgetRangeState.rawValue.flatMap(PitchRange.init(rawValue:))
            ?? configuration.range
        let activePitches = WidgetPitchState.activePitches
        return PitchEntry(
            date: .now,
            range: selectedRange,
            activePitches: activePitches
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
    var display: String { "\(name)\(accidental == "natural" ? "" : "♯")\(octave)" }
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
    let lit: Color
    let onLit: Color

    init(dark: Bool) {
        ground = Color(hex: dark ? 0x0E0F10 : 0xDADBDC)
        surface = Color(hex: dark ? 0x16181A : 0xE7E8E9)
        ink = Color(hex: dark ? 0xD9DBDD : 0x1C1E20)
        secondary = Color(hex: dark ? 0x898D92 : 0x55585C)
        hairline = Color(hex: dark ? 0x2C2F33 : 0xB7B9BC)
        lit = Color(hex: dark ? 0xF2EFE6 : 0x141618)
        onLit = Color(hex: dark ? 0x101214 : 0xF2F3F4)
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

private struct RadialLayout: Layout {
    let radius: CGFloat
    let cellDiameter: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let side = (radius + cellDiameter / 2) * 2
        return CGSize(width: side, height: side)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        guard !subviews.isEmpty else { return }
        // Two cells straddle twelve o'clock so the octave's root and its
        // repeat sit side by side at the top, as on the instrument face.
        let step = 2 * CGFloat.pi / CGFloat(subviews.count)
        let start = -CGFloat.pi / 2 + step / 2
        let center = CGPoint(x: bounds.midX, y: bounds.midY)
        let cell = ProposedViewSize(width: cellDiameter, height: cellDiameter)
        for (index, subview) in subviews.enumerated() {
            let angle = start + CGFloat(index) * step
            let point = CGPoint(
                x: center.x + cos(angle) * radius,
                y: center.y + sin(angle) * radius
            )
            subview.place(at: point, anchor: .center, proposal: cell)
        }
    }
}

private struct PitchCellStyle: ToggleStyle {
    let pitch: Pitch
    let diameter: CGFloat
    let palette: PlatePalette

    func makeBody(configuration: Configuration) -> some View {
        let active = configuration.isOn
        let natural = pitch.accidental == "natural"
        ZStack {
            if active {
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [palette.lit.opacity(0.28), palette.lit.opacity(0)],
                            center: .center,
                            startRadius: diameter * 0.46,
                            endRadius: diameter * 0.8
                        )
                    )
                    .frame(width: diameter * 1.6, height: diameter * 1.6)
            }
            Circle().fill(active ? palette.lit : palette.surface)
            Circle().stroke(active ? palette.lit : palette.hairline, lineWidth: active ? 2 : 1.25)
            Circle()
                .stroke(
                    active ? palette.onLit.opacity(0.45) : palette.secondary.opacity(0.28),
                    lineWidth: 0.7
                )
                .padding(diameter * 0.07)
            Text(pitch.engraved)
                .font(.custom("Oswald-Medium", size: diameter * (natural ? 0.44 : 0.29)))
                .foregroundStyle(active ? palette.onLit : (natural ? palette.ink : palette.secondary))
        }
        .frame(width: diameter, height: diameter)
        .contentShape(Circle())
    }
}

/// A Toggle rather than a Button: WidgetKit flips a toggle's appearance the
/// moment it is tapped, before the intent runs, so the cell lights even if
/// the timeline reload that follows is delayed on the device.
private struct PitchCell: View {
    let pitch: Pitch
    let active: Bool
    let diameter: CGFloat
    let palette: PlatePalette

    var body: some View {
        Toggle(
            isOn: active,
            intent: PlayWidgetPitchIntent(pitchIndex: pitch.id, frequency: pitch.frequency)
        ) {
            Text(pitch.spoken)
        }
        .toggleStyle(PitchCellStyle(pitch: pitch, diameter: diameter, palette: palette))
        .accessibilityLabel(pitch.spoken)
    }
}

private struct RangeSegment: View {
    let range: PitchRange
    let selected: Bool
    let palette: PlatePalette

    private var label: String { range == .cToC ? "C TO C" : "F TO F" }

    var body: some View {
        Button(intent: SelectWidgetRangeIntent(range: range)) {
            HStack(spacing: 5) {
                Circle()
                    .fill(selected ? palette.lit : .clear)
                    .frame(width: 5, height: 5)
                Text(label)
                    .font(.custom("Oswald-Medium", size: 11))
                    .tracking(1.4)
                    .foregroundStyle(selected ? palette.ink : palette.secondary.opacity(0.75))
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(selected ? palette.ink.opacity(0.10) : .clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(range == .cToC ? "Octave range C to C" : "Octave range F to F")
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

private struct PitchFace: View {
    @Environment(\.colorScheme) private var colorScheme
    let entry: PitchEntry

    var body: some View {
        GeometryReader { geometry in
            let dark = colorScheme == .dark
            let palette = PlatePalette(dark: dark)
            let size = geometry.size
            let faceHeight = size.height * 0.86
            let ring = min(size.width, size.height * 0.82) * 0.365
            let cellDiameter = max(44, ring * 0.45)
            let pitches = PitchCatalog.notes(for: entry.range)

            ZStack {
                palette.ground
                ScoreBackground(palette: palette, dark: dark)
                    .clipped()

                VStack(spacing: 0) {
                    ZStack {
                        RadialLayout(radius: ring, cellDiameter: cellDiameter) {
                            ForEach(pitches) { pitch in
                                PitchCell(
                                    pitch: pitch,
                                    active: entry.activePitches.contains(pitch.id),
                                    diameter: cellDiameter,
                                    palette: palette
                                )
                            }
                        }
                        holeContent(pitches: pitches, palette: palette, ring: ring, cellDiameter: cellDiameter)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: faceHeight)

                    Text("DIGITAL PITCH PIPE")
                        .font(.custom("Oswald-Medium", size: ring * 0.075))
                        .tracking(ring * 0.025)
                        .foregroundStyle(palette.secondary.opacity(0.68))
                        .frame(maxWidth: .infinity)
                        .frame(height: size.height - faceHeight)
                }
            }
            .clipShape(ContainerRelativeShape())
        }
        .containerBackground(for: .widget) { Color.clear }
    }

    /// The readout and the range selector share the ring's hole. The selector
    /// keeps a 44pt row, so its width is the chord of the hole at that depth.
    private func holeContent(
        pitches: [Pitch],
        palette: PlatePalette,
        ring: CGFloat,
        cellDiameter: CGFloat
    ) -> some View {
        let holeRadius = ring - cellDiameter / 2 - 4
        let selectorHeight: CGFloat = 44
        let readoutHeight = ring * 0.5
        let halfBlock = (readoutHeight + selectorHeight) / 2
        let chord = 2 * sqrt(max(0, holeRadius * holeRadius - halfBlock * halfBlock))
        let selectorWidth = min(chord - 8, ring * 1.3)
        return VStack(spacing: 0) {
            readout(pitches: pitches, palette: palette, ring: ring)
                .frame(height: readoutHeight)
            HStack(spacing: 0) {
                RangeSegment(range: .cToC, selected: entry.range == .cToC, palette: palette)
                Rectangle()
                    .fill(palette.hairline)
                    .frame(width: 1)
                RangeSegment(range: .fToF, selected: entry.range == .fToF, palette: palette)
            }
            .frame(width: selectorWidth, height: selectorHeight)
            .background(palette.surface.opacity(0.92))
            .overlay(RoundedRectangle(cornerRadius: 4).stroke(palette.hairline, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 4))
        }
    }

    @ViewBuilder
    private func readout(pitches: [Pitch], palette: PlatePalette, ring: CGFloat) -> some View {
        let sounding = pitches.filter { entry.activePitches.contains($0.id) }
        if sounding.count == 1, let pitch = sounding.first {
            VStack(spacing: ring * 0.02) {
                Text(pitch.display)
                    .font(.custom("Oswald-Medium", size: ring * 0.24))
                    .foregroundStyle(palette.ink)
                Text(String(format: "%.1f Hz", pitch.frequency))
                    .font(.system(size: ring * 0.10, design: .monospaced))
                    .foregroundStyle(palette.ink)
            }
        } else if !sounding.isEmpty {
            VStack(spacing: ring * 0.02) {
                Text(sounding.map(\.display).joined(separator: " "))
                    .font(.custom("Oswald-Medium", size: ring * 0.17))
                    .foregroundStyle(palette.ink)
                    .lineLimit(2)
                    .minimumScaleFactor(0.6)
                    .multilineTextAlignment(.center)
                if let chord = PitchChord.name(cells: sounding.map(\.id)) {
                    Text(chord)
                        .font(.custom("Oswald-Medium", size: ring * 0.16))
                        .tracking(ring * 0.01)
                        .foregroundStyle(palette.ink)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                } else {
                    Text("\(sounding.count) NOTES")
                        .font(.system(size: ring * 0.10, design: .monospaced))
                        .foregroundStyle(palette.ink)
                }
            }
            .frame(maxWidth: ring * 1.4)
        } else {
            Text("\u{2014} Hz")
                .font(.system(size: ring * 0.13, design: .monospaced))
                .foregroundStyle(palette.secondary.opacity(0.55))
        }
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
