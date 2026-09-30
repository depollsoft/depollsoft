//
//  ClassicPitchPipe.swift
//  pitchperfect
//
//  The classic pitch pipe: the grid of big buttons the app had before the
//  Laboratory Instrument, with its middle put to use. Twelve cells run round
//  the edge of a four-by-four grid, clockwise from C at the top left; the
//  octave's upper note is one wide cell across the top of the middle; below it
//  a well holds the readout and the range control. Settings switches it on in
//  place of the radial face; the same PitchPipeModel drives both.
//

import SwiftUI
import UIKit

// MARK: - Geometry

/// Where the classic grid's parts sit on a face of `size`, for a range control
/// of `selectorSize`.
struct ClassicGeometry: Equatable, PitchFaceLayout {
    /// A cell's place: `columns` wide from (`column`, `row`).
    struct Place: Equatable {
        let column: Int
        let row: Int
        var columns = 1

        func contains(column: Int, row: Int) -> Bool {
            row == self.row && column >= self.column && column < self.column + columns
        }
    }

    /// Along the top, down the right, back along the bottom, up the left, then
    /// the octave's upper note across the top of the middle.
    static let places: [Place] = [
        Place(column: 0, row: 0), Place(column: 1, row: 0), Place(column: 2, row: 0), Place(column: 3, row: 0),
        Place(column: 3, row: 1), Place(column: 3, row: 2),
        Place(column: 3, row: 3), Place(column: 2, row: 3), Place(column: 1, row: 3), Place(column: 0, row: 3),
        Place(column: 0, row: 2), Place(column: 0, row: 1),
        Place(column: 1, row: 1, columns: 2),
    ]

    /// A Material button's background inset, which the Android grid kept.
    static let buttonInset = CGSize(width: 4, height: 6)
    /// The band the range control is given, whatever its own height.
    static let selectorBand: CGFloat = 48
    /// Between the readout and the range control.
    static let gap: CGFloat = 8
    /// The least room the readout gets above the range control; below this
    /// height it reads as one line.
    static let minReadout: CGFloat = 56

    let size: CGSize
    let places: [Place]
    let slots: [CGRect]
    let buttons: [CGRect]
    /// A button in a single slot: every label is sized from it, the wide cell's included.
    let singleButton: CGRect
    /// The bare panel under the wide cell.
    let well: CGRect
    /// The range control sits under the readout (a tall well) or beside it (a short one).
    let stacked: Bool
    let selectorRect: CGRect
    let readoutRect: CGRect

    init(size: CGSize, count: Int, selectorSize: CGSize = CGSize(width: 150, height: 32)) {
        self.size = size
        let columnWidth = size.width / 4
        let rowHeight = size.height / 4
        func rect(_ place: Place) -> CGRect {
            CGRect(x: CGFloat(place.column) * columnWidth, y: CGFloat(place.row) * rowHeight,
                   width: CGFloat(place.columns) * columnWidth, height: rowHeight)
        }
        func inset(_ rect: CGRect) -> CGRect {
            rect.insetBy(dx: Self.buttonInset.width, dy: Self.buttonInset.height)
        }
        places = Array(Self.places.prefix(max(0, min(count, Self.places.count))))
        slots = places.map(rect)
        buttons = slots.map(inset)
        singleButton = inset(rect(Place(column: 0, row: 0)))
        well = inset(rect(Place(column: 1, row: 2, columns: 2)))

        stacked = well.height >= Self.selectorBand + Self.gap + Self.minReadout
        let height = min(selectorSize.height, well.height)
        if stacked {
            let width = min(selectorSize.width, well.width)
            let bandTop = well.maxY - Self.selectorBand
            selectorRect = CGRect(x: well.midX - width / 2, y: bandTop + (Self.selectorBand - height) / 2,
                                  width: width, height: height)
            readoutRect = CGRect(x: well.minX, y: well.minY, width: well.width,
                                 height: max(0, bandTop - Self.gap - well.minY))
        } else {
            let width = min(selectorSize.width, well.width / 2)
            selectorRect = CGRect(x: well.maxX - width, y: well.midY - height / 2, width: width, height: height)
            readoutRect = CGRect(x: well.minX, y: well.minY,
                                 width: max(0, selectorRect.minX - Self.gap - well.minX), height: well.height)
        }
    }

    /// The cell whose slot holds `point`: a finger anywhere in it, the margin
    /// round the drawn button included, plays the cell. -1 for none.
    func cellIndex(at point: CGPoint) -> Int {
        guard size.width > 0, size.height > 0,
              point.x >= 0, point.y >= 0, point.x < size.width, point.y < size.height else { return -1 }
        let column = min(Int(point.x / (size.width / 4)), 3)
        let row = min(Int(point.y / (size.height / 4)), 3)
        return places.firstIndex { $0.contains(column: column, row: row) } ?? -1
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

    /// The label `make` builds at scale 1, or smaller until it fits `bounds`.
    private func fitted(within bounds: CGSize, _ make: (CGFloat) -> NSAttributedString) -> NSAttributedString {
        let label = make(1)
        let size = label.size()
        guard size.width > 0, size.height > 0 else { return label }
        let scale = min(1, bounds.width / size.width, bounds.height / size.height)
        return scale < 1 ? make(scale) : label
    }

    /// The radial face's readout, centred in the well: the sounding notes, then a
    /// frequency, an interval, a chord's name or a count; "— Hz" when silent.
    /// A short readout (a phone turned on its side) reads as one line.
    private func drawReadout(ink: UIColor, inkSecondary: UIColor) {
        let area = geometry.readoutRect
        guard area.width > 0, area.height > 0 else { return }
        if area.height < ClassicGeometry.minReadout {
            drawReadoutLine(in: area, ink: ink, inkSecondary: inkSecondary)
            return
        }
        let nameSize = min(area.height * 0.46, area.width * 0.22)
        let detailSize = nameSize * 0.5
        let chordSize = nameSize * 0.62
        let fit = CGSize(width: area.width * 0.92, height: .greatestFiniteMagnitude)

        guard let readout else {
            let idle = fitted(within: fit) { scale in
                NSAttributedString(string: "\u{2014} Hz", attributes: [
                    .font: DPTheme.monospacedFont(size: detailSize * scale),
                    .foregroundColor: inkSecondary.withAlphaComponent(0.55),
                ])
            }
            let size = idle.size()
            idle.draw(at: CGPoint(x: area.midX - size.width / 2, y: area.midY - size.height / 2))
            return
        }

        let names = fitted(within: fit) { scale in
            NSAttributedString(string: readout.names, attributes: [
                .font: DPTheme.condensedFont(size: nameSize * scale), .foregroundColor: ink,
            ])
        }
        // A chord has a name, not a measurement: engrave it in the display face.
        let detail = fitted(within: fit) { scale in
            NSAttributedString(string: readout.detail, attributes: readout.isChord
                ? [.font: DPTheme.condensedFont(size: chordSize * scale), .foregroundColor: ink,
                   .kern: chordSize * scale * 0.12]
                : [.font: DPTheme.monospacedFont(size: detailSize * scale), .foregroundColor: ink])
        }
        let nameBox = names.size()
        let detailBox = detail.size()
        let spacing = nameSize * 0.15
        let top = area.midY - (nameBox.height + spacing + detailBox.height) / 2
        names.draw(at: CGPoint(x: area.midX - nameBox.width / 2, y: top))
        detail.draw(at: CGPoint(x: area.midX - detailBox.width / 2, y: top + nameBox.height + spacing))
    }

    /// The readout as one line: the names, then the detail after two spaces.
    private func drawReadoutLine(in area: CGRect, ink: UIColor, inkSecondary: UIColor) {
        let lineSize = area.height * 0.6
        let fit = CGSize(width: area.width * 0.92, height: .greatestFiniteMagnitude)
        let line = fitted(within: fit) { scale in
            guard let readout else {
                return NSAttributedString(string: "\u{2014} Hz", attributes: [
                    .font: DPTheme.monospacedFont(size: lineSize * 0.7 * scale),
                    .foregroundColor: inkSecondary.withAlphaComponent(0.55),
                ])
            }
            let text = NSMutableAttributedString(string: readout.names + "  ", attributes: [
                .font: DPTheme.condensedFont(size: lineSize * scale), .foregroundColor: ink,
            ])
            text.append(NSAttributedString(string: readout.detail, attributes: readout.isChord
                ? [.font: DPTheme.condensedFont(size: lineSize * 0.8 * scale), .foregroundColor: ink,
                   .kern: lineSize * 0.8 * scale * 0.12]
                : [.font: DPTheme.monospacedFont(size: lineSize * 0.7 * scale), .foregroundColor: ink]))
            return text
        }
        let size = line.size()
        line.draw(at: CGPoint(x: area.midX - size.width / 2, y: area.midY - size.height / 2))
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

    static let minimumLabelSize: CGFloat = 28

    var body: some View {
        GeometryReader { proxy in
            let geometry = ClassicGeometry(size: proxy.size, count: model.notes.count, selectorSize: selectorSize)
            ZStack(alignment: .topLeading) {
                if model.anyPlaying, !reduceMotion, !PitchInstrumentView.breathingFrozen {
                    TimelineView(.animation) { timeline in
                        face(geometry, phase: PitchInstrumentView.phase(at: timeline.date))
                    }
                } else {
                    face(geometry, phase: 0)
                }
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
            .onAppear { model.classicGeometry = geometry }
            .onChange(of: geometry) { _, new in model.classicGeometry = new }
        }
    }

    /// The old app's segmented control, now for the inclusive octave.
    private var rangePicker: some View {
        Picker("Octave range", selection: Binding(get: { model.isHighRange }, set: { model.pickRange(high: $0) })) {
            Text("C to C").tag(false)
            Text("F to F").tag(true)
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
