//
//  TMArtworkViews.swift
//  tagmaster
//
//  Tag Master's two pieces of motion artwork, drawn in SwiftUI from the same
//  vector data the UIKit layers used (TMQuartetArtwork, TMLogoArtwork):
//
//  - TMQuartetStaff: the staff with a four-note chord, whose notes "gather" in
//    turn while a tag is on its way.
//  - TMBarberPole: the logo's pole with its stripes turning, the app's
//    activity indicator, full size in lists and compact beside buttons.
//
//  Both hold still under Reduce Motion, while the scene is inactive, and
//  whenever the caller says they should not move.
//

import SwiftUI

/// When motion artwork moves: only when asked, never under Reduce Motion, and
/// never while the scene is not in front.
enum TMMotion {
    static func moves(animating: Bool, reduceMotion: Bool, scenePhase: ScenePhase) -> Bool {
        animating && !reduceMotion && scenePhase == .active
    }
}

// MARK: - Quartet

/// The artwork's per-voice C arrays, which Swift imports as tuples.
enum TMQuartetVoices {
    static func value(_ tuple: (CGFloat, CGFloat, CGFloat, CGFloat), _ index: Int) -> CGFloat {
        withUnsafeBytes(of: tuple) { $0.bindMemory(to: CGFloat.self)[index] }
    }
    static func x(_ index: Int) -> CGFloat { value(TMQuartetX, index) }
    static func y(_ index: Int) -> CGFloat { value(TMQuartetY, index) }
    static func still(_ index: Int) -> CGFloat { value(TMQuartetStill, index) }
}

/// The quartet staff. Its notes animate only while `animating` is true.
struct TMQuartetStaff: View {
    var animating: Bool

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.scenePhase) private var scenePhase
    @State private var start = Date()

    static let size = CGSize(width: TMQuartetWidth, height: TMQuartetHeight)

    private var moving: Bool { TMMotion.moves(animating: animating, reduceMotion: reduceMotion, scenePhase: scenePhase) }

    var body: some View {
        TimelineView(.animation(paused: !moving)) { timeline in
            Canvas { context, size in
                TMQuartetStaff.draw(in: &context, size: size,
                                    dark: colorScheme == .dark,
                                    opacities: opacities(at: timeline.date))
            }
        }
        .frame(width: TMQuartetWidth, height: TMQuartetHeight)
        .onChange(of: moving, initial: true) { _, now in
            // Every run of the animation starts from the beginning of its period.
            if now { start = Date() }
        }
        .accessibilityHidden(true)
    }

    /// Each note's opacity: at rest, or sampled from its keyframes.
    func opacities(at date: Date) -> [Double] {
        let still = (0..<4).map { Double(TMQuartetVoices.still($0)) }
        guard moving else { return still }
        let period = Double(TMQuartetPeriod)
        let phase = date.timeIntervalSince(start).truncatingRemainder(dividingBy: period) / period
        return (0..<4).map { TMQuartetStaff.sample(voice: $0, phase: phase) }
    }

    /// Linear interpolation across a voice's evenly spaced keyframes.
    static func sample(voice: Int, phase: Double) -> Double {
        let values = TMQuartetSamples(UInt(voice)).map(\.doubleValue)
        guard values.count > 1 else { return values.first ?? 1 }
        let position = max(0, min(1, phase)) * Double(values.count - 1)
        let index = min(Int(position), values.count - 2)
        let fraction = position - Double(index)
        return values[index] + (values[index + 1] - values[index]) * fraction
    }

    static func draw(in context: inout GraphicsContext, size: CGSize, dark: Bool, opacities: [Double]) {
        let scale = min(size.width / TMQuartetWidth, size.height / TMQuartetHeight)
        var artwork = context
        artwork.translateBy(x: (size.width - TMQuartetWidth * scale) / 2,
                            y: (size.height - TMQuartetHeight * scale) / 2)
        artwork.scaleBy(x: scale, y: scale)
        let ink = Color(cgColor: TMQuartetColor(dark, false)!.takeUnretainedValue())
        let staffColor = Color(cgColor: TMQuartetColor(dark, true)!.takeUnretainedValue())
        let mask = Path(TMQuartetStaffMaskPath()!.takeUnretainedValue())

        var masked = artwork
        masked.clip(to: mask, style: FillStyle(eoFill: true))
        masked.stroke(Path(TMQuartetStaffPath()!.takeUnretainedValue()), with: .color(staffColor),
                      lineWidth: TMQuartetStaffWidth)
        for symbol in [TMQuartetStemPath(), TMQuartetLedgerPath(), TMQuartetFlatPath(), TMQuartetLabelPath()] {
            masked.fill(Path(symbol!.takeUnretainedValue()), with: .color(ink))
        }
        let note = Path(TMQuartetNotePath()!.takeUnretainedValue())
        for index in 0..<4 {
            var voice = artwork
            voice.translateBy(x: TMQuartetVoices.x(index), y: TMQuartetVoices.y(index))
            voice.opacity = opacities.indices.contains(index) ? opacities[index] : 1
            voice.fill(note, with: .color(ink))
        }
    }
}

// MARK: - Barber pole

/// The turning barber pole. Compact (18.8 × 32) sits beside a control; full size
/// (34 wide, 68 tall) marks a whole list loading.
struct TMBarberPole: View {
    var compact: Bool = true
    /// Light metal regardless of appearance, for the charcoal navigation bar.
    var darkSurface: Bool = false
    var animating: Bool = true

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.scenePhase) private var scenePhase
    @State private var start = Date()

    private var moving: Bool { TMMotion.moves(animating: animating, reduceMotion: reduceMotion, scenePhase: scenePhase) }

    var body: some View {
        TimelineView(.animation(paused: !moving)) { timeline in
            Canvas { context, size in
                TMBarberPole.draw(in: &context, size: size, compact: compact,
                                  metalDark: darkSurface || colorScheme == .dark,
                                  phase: phase(at: timeline.date))
            }
        }
        .frame(width: compact ? TMLoaderCompactWidth : nil, height: compact ? TMLoaderCompactHeight : 68)
        .frame(minWidth: compact ? nil : TMLoaderArtworkWidth)
        .onChange(of: moving, initial: true) { _, now in
            if now { start = Date() }
        }
    }

    /// How far the stripes have travelled along the pole's axis.
    func phase(at date: Date) -> CGFloat {
        TMBarberPole.phase(elapsed: date.timeIntervalSince(start), moving: moving)
    }

    /// The stripes' travel `elapsed` seconds into a turn: one stripe step per
    /// loop, and none at all while the pole holds still.
    static func phase(elapsed: TimeInterval, moving: Bool) -> CGFloat {
        guard moving else { return 0 }
        let duration = Double(TMLoaderDurationSeconds)
        let fraction = elapsed.truncatingRemainder(dividingBy: duration) / duration
        return CGFloat(fraction) * TMLoaderStripeStep * TMLoaderPhaseMultiplier
    }

    static func color(_ name: String) -> Color {
        Color(cgColor: TMLoaderColor(name))
    }

    static func draw(in context: inout GraphicsContext, size: CGSize, compact: Bool, metalDark: Bool, phase: CGFloat) {
        let artworkHeight = compact ? TMLoaderCompactHeight : TMLoaderArtworkHeight
        let artworkWidth = compact ? TMLoaderCompactWidth : TMLoaderArtworkWidth
        let scale = min(min(artworkHeight, size.height) / TMLogoHeight, min(artworkWidth, size.width) / TMLogoWidth)
        var logo = context
        logo.translateBy(x: size.width / 2, y: size.height / 2)
        logo.scaleBy(x: scale, y: scale)
        logo.translateBy(x: -TMLogoWidth / 2, y: -TMLogoHeight / 2)
        logo.fill(Path(TMLoaderMetalPath()),
                  with: .color(color(metalDark ? "metalDark" : "metalLight")),
                  style: FillStyle(eoFill: true))

        var cylinder = logo
        cylinder.clip(to: Path(TMLoaderShaftPath()))
        cylinder.fill(Path(CGRect(x: 0, y: 0, width: TMLogoWidth, height: TMLogoHeight)), with: .color(color("white")))
        cylinder.rotate(by: .degrees(Double(TMLoaderAxisAngle)))
        cylinder.translateBy(x: 0, y: phase)
        let stripe = Path(TMLoaderStripePath())
        for index in TMLoaderRepeatMin...TMLoaderRepeatMax {
            var band = cylinder
            band.translateBy(x: 0, y: CGFloat(index) * TMLoaderStripeStep)
            band.fill(stripe, with: .color(color(index % 2 == 0 ? "red" : "blue")))
        }
    }
}

extension TMBarberPole {
    /// The full-size pole a list shows while its first page loads.
    static func listLoading(label: String = "Loading tags") -> some View {
        TMBarberPole(compact: false)
            .accessibilityElement()
            .accessibilityLabel(label)
            .accessibilityIdentifier("query.loading.barberpole")
    }

    /// A compact pole standing in for a control while its work runs.
    static func operation(_ label: String, darkSurface: Bool = false, active: Bool = true) -> some View {
        TMBarberPole(compact: true, darkSurface: darkSurface, animating: active)
            .opacity(active ? 1 : 0)
            .accessibilityElement()
            .accessibilityLabel(label)
            .accessibilityHidden(!active)
    }
}
