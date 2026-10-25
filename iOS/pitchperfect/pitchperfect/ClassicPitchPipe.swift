//
//  ClassicPitchPipe.swift
//  pitchperfect
//
//  The classic pitch pipe: the grid of big buttons the app had before the
//  Laboratory Instrument, with its middle put to use. One octave of twelve
//  cells runs round the edge of a four-by-four grid, clockwise from C at the
//  top left, as the old app had it (the radial face's upper note is not on it);
//  the middle is a well holding the readout and the range control. Settings
//  switches it on in place of the radial face; the same PitchPipeModel drives
//  both.
//

import SwiftUI
import UIKit

// MARK: - Geometry

/// Where the classic grid's parts sit on a face of `size`, for a range control
/// of `selectorSize`.
struct ClassicGeometry: Equatable, PitchFaceLayout {
    /// A cell's place in the grid.
    struct Place: Equatable {
        let column: Int
        let row: Int
    }

    /// One octave, along the top, down the right, back along the bottom and up the left.
    static let places: [Place] = [
        Place(column: 0, row: 0), Place(column: 1, row: 0), Place(column: 2, row: 0), Place(column: 3, row: 0),
        Place(column: 3, row: 1), Place(column: 3, row: 2),
        Place(column: 3, row: 3), Place(column: 2, row: 3), Place(column: 1, row: 3), Place(column: 0, row: 3),
        Place(column: 0, row: 2), Place(column: 0, row: 1),
    ]

    /// The cells the grid shows: the pipe's first twelve notes, C to B or F to E.
    static var cellCount: Int { places.count }

    /// A Material button's background inset, which the Android grid kept.
    static let buttonInset = CGSize(width: 4, height: 6)
    /// Between the readout and the range control when they sit side by side.
    static let gap: CGFloat = 8
    /// The least room the readout takes in half the well; below this height
    /// it reads as one line.
    static let minReadout: CGFloat = 56

    /// A column's or row's start and length.
    struct Span: Equatable {
        let start: CGFloat
        let length: CGFloat
        var end: CGFloat { start + length }

        /// Four equal spans from `start`.
        static func quarters(from start: CGFloat, length: CGFloat) -> [Span] {
            (0..<4).map { Span(start: start + CGFloat($0) * (length / 4), length: length / 4) }
        }
    }

    let size: CGSize
    /// How a fold across the grid parts it (see `InstrumentGeometry.Posture`).
    let posture: InstrumentGeometry.Posture
    let columns: [Span]
    let rows: [Span]
    let places: [Place]
    let slots: [CGRect]
    let buttons: [CGRect]
    /// A button in a single slot: every label is sized from it.
    let singleButton: CGRect
    /// The bare middle of the grid, holding the readout and the range control.
    let well: CGRect
    /// The readout fills the well's top half and the range control its bottom
    /// half (a tall well), or the control sits beside the readout (a short one).
    let stacked: Bool
    let selectorRect: CGRect
    let readoutRect: CGRect

    /// `fold` is the band an iPhone Duo's fold covers, in the grid's coordinates.
    /// The grid keeps its outer margins and opens at the fold: folded across, two
    /// rows above it and two below, the well's readout above and the range control
    /// below; folded down, two columns either side, the readout left of the fold
    /// and the control right of it. A fold too near an edge to part the grid
    /// evenly leaves it whole on the larger side.
    init(size: CGSize, count: Int, selectorSize: CGSize = CGSize(width: 150, height: 32), fold: CGRect? = nil) {
        self.size = size
        let bounds = CGRect(origin: .zero, size: size)
        var columns = Span.quarters(from: 0, length: size.width)
        var rows = Span.quarters(from: 0, length: size.height)
        var posture = InstrumentGeometry.Posture.flat
        if let sides = FoldSides(bounds: bounds, fold: fold) {
            let (first, second) = sides.across ? (sides.first.height, sides.second.height)
                                               : (sides.first.width, sides.second.width)
            if sides.parts {
                // Two either side, each pair sharing its side.
                let before = sides.across ? sides.first.minY : sides.first.minX
                let after = sides.across ? sides.second.minY : sides.second.minX
                let parted = Array(Span.quarters(from: before, length: first * 2).prefix(2))
                    + Array(Span.quarters(from: after, length: second * 2).prefix(2))
                if sides.across { rows = parted } else { columns = parted }
                posture = sides.across ? .tabletop : .book
            } else {
                let larger = sides.larger
                columns = Span.quarters(from: larger.minX, length: larger.width)
                rows = Span.quarters(from: larger.minY, length: larger.height)
            }
        }
        self.posture = posture
        self.columns = columns
        self.rows = rows
        func rect(columns range: ClosedRange<Int>, rows rowRange: ClosedRange<Int>) -> CGRect {
            CGRect(x: columns[range.lowerBound].start, y: rows[rowRange.lowerBound].start,
                   width: columns[range.upperBound].end - columns[range.lowerBound].start,
                   height: rows[rowRange.upperBound].end - rows[rowRange.lowerBound].start)
        }
        func inset(_ rect: CGRect) -> CGRect {
            rect.insetBy(dx: Self.buttonInset.width, dy: Self.buttonInset.height)
        }
        places = Array(Self.places.prefix(max(0, min(count, Self.places.count))))
        slots = places.map { rect(columns: $0.column...$0.column, rows: $0.row...$0.row) }
        buttons = slots.map(inset)
        // The smallest slot, so every label is the same size either side of a fold.
        singleButton = inset(CGRect(x: 0, y: 0, width: columns.map(\.length).min() ?? 0,
                                    height: rows.map(\.length).min() ?? 0))
        well = inset(rect(columns: 1...2, rows: 1...2))

        let height: CGFloat
        switch posture {
        case .tabletop:
            // The readout on the well's upper row, the control centred on its lower one.
            let upper = inset(rect(columns: 1...2, rows: 1...1))
            let lower = inset(rect(columns: 1...2, rows: 2...2))
            stacked = true
            height = min(selectorSize.height, lower.height)
            let width = min(selectorSize.width, lower.width)
            selectorRect = CGRect(x: lower.midX - width / 2, y: lower.midY - height / 2, width: width, height: height)
            readoutRect = upper
        case .book:
            // The readout in the well's left column, the control centred in its right one.
            let left = inset(rect(columns: 1...1, rows: 1...2))
            let right = inset(rect(columns: 2...2, rows: 1...2))
            stacked = false
            height = min(selectorSize.height, right.height)
            let width = min(selectorSize.width, right.width)
            selectorRect = CGRect(x: right.midX - width / 2, y: right.midY - height / 2, width: width, height: height)
            readoutRect = left
        case .flat:
            let half = well.height / 2
            stacked = half >= max(selectorSize.height, Self.minReadout)
            height = min(selectorSize.height, well.height)
            if stacked {
                let width = min(selectorSize.width, well.width)
                selectorRect = CGRect(x: well.midX - width / 2, y: well.midY + (half - height) / 2,
                                      width: width, height: height)
                readoutRect = CGRect(x: well.minX, y: well.minY, width: well.width, height: half)
            } else {
                let width = min(selectorSize.width, well.width / 2)
                selectorRect = CGRect(x: well.maxX - width, y: well.midY - height / 2, width: width, height: height)
                readoutRect = CGRect(x: well.minX, y: well.minY,
                                     width: max(0, selectorRect.minX - Self.gap - well.minX), height: well.height)
            }
        }
    }

    /// The cell whose slot holds `point`: a finger anywhere in it, the margin
    /// round the drawn button included, plays the cell. -1 for none (the well,
    /// the fold, outside the grid).
    func cellIndex(at point: CGPoint) -> Int {
        guard let column = columns.firstIndex(where: { point.x >= $0.start && point.x < $0.end }),
              let row = rows.firstIndex(where: { point.y >= $0.start && point.y < $0.end }) else { return -1 }
        return places.firstIndex { $0.column == column && $0.row == row } ?? -1
    }

    /// The range control is a real segmented control and takes its own touches.
    func range(at point: CGPoint) -> Bool? { nil }
}

// MARK: - Drawing

/// Paints the grid with the radial face's materials: surface buttons with a
/// hairline rim, the lit treatment for a sounding one, and the readout in the well.
struct ClassicRenderer {
    let geometry: ClassicGeometry
    let notes: [DPNote]
    let naturals: [Bool]
    let playing: Set<Int>
    let readout: PitchPipeModel.Readout?
    let breathePhase: CGFloat
    let traits: UITraitCollection
    /// The labels' least size: 28 pt at the reader's text size.
    let minimumLabelSize: CGFloat

    static let cornerRadius: CGFloat = 2
    static let glowReach: CGFloat = 14
    static let glowSteps = 10
    static let glowAlpha: CGFloat = 22.0 / 255.0
    /// The accidentals' glyph size for a natural's letter size: NoteHedz's sharp
    /// then stands about 0.72 of Oswald's capital, as "♯/♭" does beside the
    /// letters on Android's grid. (The radial face's small cells use about 0.46.)
    static let glyphScale: CGFloat = 0.86

    /// A natural's label size: the least size, or larger in proportion on bigger buttons.
    var naturalSize: CGFloat {
        max(minimumLabelSize, min(geometry.singleButton.width, geometry.singleButton.height) * 0.3)
    }

    func draw(in context: CGContext) {
        let ground = DPTheme.plateGround.resolvedColor(with: traits)
        let surface = DPTheme.plateSurface.resolvedColor(with: traits)
        let ink = DPTheme.plateInk.resolvedColor(with: traits)
        let inkSecondary = DPTheme.plateInkSecondary.resolvedColor(with: traits)
        let hairline = DPTheme.plateHairline.resolvedColor(with: traits)
        let lit = DPTheme.plateLit.resolvedColor(with: traits)
        let onLit = DPTheme.plateOnLit.resolvedColor(with: traits)

        InstrumentRenderer.drawPanel(context: context, bounds: CGRect(origin: .zero, size: geometry.size),
                                     ground: ground, hairline: hairline, markColor: inkSecondary)

        let breath = 0.82 + 0.18 * sin(breathePhase)
        let cells = min(notes.count, geometry.buttons.count)
        // Every glow first, so no neighbour's glow lies over a button.
        for index in 0..<cells where playing.contains(index) {
            for step in stride(from: Self.glowSteps, through: 1, by: -1) {
                let spread = Self.glowReach * CGFloat(step) / CGFloat(Self.glowSteps)
                lit.withAlphaComponent(Self.glowAlpha * breath).setFill()
                UIBezierPath(roundedRect: geometry.buttons[index].insetBy(dx: -spread, dy: -spread),
                             cornerRadius: Self.cornerRadius + spread).fill()
            }
        }

        for index in 0..<cells {
            let button = geometry.buttons[index]
            let isPlaying = playing.contains(index)
            let path = UIBezierPath(roundedRect: button, cornerRadius: Self.cornerRadius)
            (isPlaying ? lit.withAlphaComponent(0.9 + 0.1 * sin(breathePhase)) : surface).setFill()
            path.fill()
            context.setStrokeColor((isPlaying ? lit : hairline).cgColor)
            context.setLineWidth(isPlaying ? 2.5 : 1.2)
            context.addPath(path.cgPath)
            context.strokePath()

            let natural = naturals.indices.contains(index) && naturals[index]
            let color = isPlaying ? onLit : (natural ? ink : inkSecondary)
            let label = fitted(within: CGSize(width: button.width * 0.8, height: button.height * 0.6)) { scale in
                natural
                    ? NSAttributedString(string: notes[index].friendlyName ?? "",
                                         attributes: [.font: DPTheme.condensedFont(size: naturalSize * scale), .foregroundColor: color])
                    : InstrumentRenderer.glyphLabel(size: naturalSize * Self.glyphScale * scale, color: color)
            }
            let size = label.size()
            label.draw(at: CGPoint(x: button.midX - size.width / 2, y: button.midY - size.height / 2))
        }

        drawReadout(ink: ink, inkSecondary: inkSecondary)
    }

    private func fitted(within bounds: CGSize, _ make: (CGFloat) -> NSAttributedString) -> NSAttributedString {
        ReadoutPainter.fitted(within: bounds, make)
    }

    /// The radial face's readout, centred in the well; a short readout (a phone
    /// turned on its side) reads as one line.
    private func drawReadout(ink: UIColor, inkSecondary: UIColor) {
        ReadoutPainter.draw(readout, in: geometry.readoutRect, ink: ink, inkSecondary: inkSecondary)
    }
}

// MARK: - View

struct ClassicPitchPipeView: View {
    @Bindable var model: PitchPipeModel
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    /// The range control's own size, measured.
    @State private var selectorSize = CGSize(width: 150, height: 32)
    /// The hinge's state, so the grid is laid out again as a fold comes and goes.
    @State private var hingeStatus = 0

    static let minimumLabelSize: CGFloat = 28

    var body: some View {
        GeometryReader { proxy in
            let _ = hingeStatus
            let geometry = ClassicGeometry(size: proxy.size, count: model.notes.count, selectorSize: selectorSize,
                                           fold: Fold.band(in: proxy))
            ZStack(alignment: .topLeading) {
                Group {
                    if model.anyPlaying, !reduceMotion, !PitchInstrumentView.breathingFrozen {
                        TimelineView(.animation) { timeline in
                            face(geometry, phase: PitchInstrumentView.phase(at: timeline.date))
                        }
                    } else {
                        face(geometry, phase: 0)
                    }
                }
                // Folding or opening crossfades the grid to its new arrangement.
                .id(geometry.posture)
                .transition(.opacity)
                MultiTouchSurface(
                    began: { id, point in model.touchBegan(id: id, at: point) },
                    moved: { id, point in model.touchMoved(id: id, to: point) },
                    ended: { id in model.touchEnded(id: id) }
                )
                accessibilityElements(geometry)
                rangePicker
                    .frame(width: geometry.selectorRect.width)
                    .position(x: geometry.selectorRect.midX, y: geometry.selectorRect.midY)
                    .background(alignment: .topLeading) {
                        rangePicker
                            .fixedSize()
                            .hidden()
                            .accessibilityHidden(true)
                            .onGeometryChange(for: CGSize.self) { $0.size } action: { selectorSize = $0 }
                    }
            }
            .animation(reduceMotion ? nil : .easeInOut(duration: 0.3), value: geometry.posture)
            .onAppear { model.classicGeometry = geometry }
            .onChange(of: geometry) { _, new in model.classicGeometry = new }
        }
        .refreshesOnFold($hingeStatus)
    }

    /// The old app's segmented control.
    private var rangePicker: some View {
        Picker("Octave range", selection: Binding(get: { model.isHighRange }, set: { model.pickRange(high: $0) })) {
            Text("C to B").tag(false)
            Text("F to E").tag(true)
        }
        .pickerStyle(.segmented)
        // A regular width (an iPad) has room for the larger control; the measured copy takes it too.
        .controlSize(horizontalSizeClass == .regular ? .large : .regular)
        .accessibilityIdentifier("pitchpipe.range")
    }

    private func face(_ geometry: ClassicGeometry, phase: CGFloat) -> some View {
        let category = UIContentSizeCategory(dynamicTypeSize)
        let renderer = ClassicRenderer(
            geometry: geometry,
            notes: model.notes,
            naturals: model.naturals,
            playing: Set(model.playingIndices),
            readout: model.readout,
            breathePhase: phase,
            traits: UITraitCollection(userInterfaceStyle: colorScheme == .dark ? .dark : .light),
            minimumLabelSize: UIFontMetrics.default.scaledValue(
                for: Self.minimumLabelSize, compatibleWith: UITraitCollection(preferredContentSizeCategory: category))
        )
        return Canvas(opaque: true) { context, _ in
            context.withCGContext { cgContext in
                UIGraphicsPushContext(cgContext)
                renderer.draw(in: cgContext)
                UIGraphicsPopContext()
            }
        }
        .accessibilityHidden(true)
    }

    /// One VoiceOver element per cell, over its whole slot.
    @ViewBuilder
    private func accessibilityElements(_ geometry: ClassicGeometry) -> some View {
        ZStack(alignment: .topLeading) {
            ForEach(Array(model.notes.indices), id: \.self) { index in
                if index < geometry.slots.count {
                    let rect = geometry.slots[index]
                    Color.clear
                        .frame(width: rect.width, height: rect.height)
                        .offset(x: rect.minX, y: rect.minY)
                        .accessibilityElement()
                        .accessibilityLabel(model.spokenName(at: index))
                        .accessibilityAddTraits(.isButton)
                        .accessibilityAction { model.activate(cell: index) }
                }
            }
        }
        .frame(width: geometry.size.width, height: geometry.size.height, alignment: .topLeading)
        .allowsHitTesting(false)
    }
}
