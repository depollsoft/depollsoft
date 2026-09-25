//
//  PitchInstrument.swift
//  pitchperfect
//
//  The Laboratory Instrument: a custom-drawn radial pitch-pipe face.
//  Twelve glass cells on a blackened-steel panel; the sounding cell is the
//  one luminous element on screen. Frequency digits and the octave-range
//  selector live in the ring's hole.
//
//  Geometry and touch rules are plain values the tests drive directly; the
//  face itself is drawn into a Canvas with the same Core Graphics calls the
//  UIKit view made, so it renders identically.
//

import SwiftUI
import UIKit

// MARK: - Geometry

/// Where everything sits on a face of `size` holding `count` cells.
struct InstrumentGeometry: Equatable {
    let size: CGSize
    let faceCenter: CGPoint
    let ringRadius: CGFloat
    let cellRadius: CGFloat
    let cellCenters: [CGPoint]
    let rangeLowRect: CGRect
    let rangeHighRect: CGRect

    init(size: CGSize, count: Int) {
        self.size = size
        let panelWidth = size.width
        let panelHeight = size.height
        faceCenter = CGPoint(x: panelWidth / 2, y: panelHeight * 0.44)
        ringRadius = min(panelWidth, panelHeight * 0.82) * 0.365
        let count = max(count, 1)
        cellRadius = ringRadius * (count > 12 ? 0.225 : 0.245)
        let step = 360.0 / Double(count)
        let start = -90.0 + step / 2.0
        let center = faceCenter
        let radius = ringRadius
        cellCenters = (0..<count).map { index in
            let angle = (start + Double(index) * step) * Double.pi / 180.0
            return CGPoint(
                x: center.x + radius * CGFloat(cos(angle)),
                y: center.y + radius * CGFloat(sin(angle))
            )
        }
        // The range selector is one machined part seated in the ring's hole.
        let rangeWidth = ringRadius * 0.72
        let rowHeight = ringRadius * 0.145
        let rangeTop = faceCenter.y + ringRadius * 0.20
        rangeLowRect = CGRect(x: faceCenter.x - rangeWidth / 2, y: rangeTop, width: rangeWidth, height: rowHeight)
        rangeHighRect = CGRect(x: rangeLowRect.minX, y: rangeLowRect.maxY, width: rangeWidth, height: rowHeight)
    }

    func cellRect(_ index: Int) -> CGRect {
        let center = cellCenters[index]
        return CGRect(x: center.x - cellRadius, y: center.y - cellRadius, width: cellRadius * 2, height: cellRadius * 2)
    }

    /// The cell under `point`, with a touch target slightly larger than the glass; -1 for none.
    func cellIndex(at point: CGPoint) -> Int {
        for (index, center) in cellCenters.enumerated() {
            let deltaX = point.x - center.x
            let deltaY = point.y - center.y
            if sqrt(deltaX * deltaX + deltaY * deltaY) <= cellRadius * 1.15 { return index }
        }
        return -1
    }
}

// MARK: - Model

@Observable
@MainActor
final class PitchPipeModel {
    private let pipe: DPPitchPipeModel
    private let player: NotePlayer
    private let settings: DPSettingsModel

    private(set) var notes: [DPNote] = []
    private(set) var naturals: [Bool] = []
    private(set) var isHighRange = false
    /// Touch id → the cell it holds.
    private(set) var activeTouches: [Int: Int] = [:]
    var geometry = InstrumentGeometry(size: .zero, count: 13)

    private let noteFeedback = UIImpactFeedbackGenerator(style: .rigid)
    private let rangeFeedback = UISelectionFeedbackGenerator()
    @ObservationIgnored private var settingsObserver: NSObjectProtocol?

    init(pipe: DPPitchPipeModel = DPPitchPipeModel(),
         player: NotePlayer = .shared,
         settings: DPSettingsModel = .sharedInstance) {
        self.pipe = pipe
        self.player = player
        self.settings = settings
        refresh()
        settingsObserver = NotificationCenter.default.addObserver(
            forName: .settingsChanged, object: settings, queue: .main
        ) { [weak self] _ in
            MainActor.assumeIsolated { self?.refresh() }
        }
    }

    deinit {
        if let settingsObserver { NotificationCenter.default.removeObserver(settingsObserver) }
    }

    var toggleMode: Bool { settings.toggleNotes }

    /// Reloads the range's notes (and which are naturals) from the stored range.
    func refresh() {
        isHighRange = pipe.isFromFToF
        notes = pipe.notes as? [DPNote] ?? []
        naturals = notes.map { Int($0.accidental.get()) == Int(Natural.rawValue) }
    }

    func isPlaying(_ index: Int) -> Bool {
        notes.indices.contains(index) && player.isPlaying(notes[index])
    }

    var playingIndices: [Int] { notes.indices.filter { player.isPlaying(notes[$0]) } }
    var anyPlaying: Bool { !playingIndices.isEmpty }

    // MARK: Touch

    func touchBegan(id: Int, at point: CGPoint) {
        if activeTouches.isEmpty, geometry.rangeLowRect.contains(point) {
            selectRange(high: false)
            return
        }
        if activeTouches.isEmpty, geometry.rangeHighRect.contains(point) {
            selectRange(high: true)
            return
        }
        let index = geometry.cellIndex(at: point)
        guard index >= 0, index < notes.count else { return }
        if toggleMode {
            let note = notes[index]
            if note.isPlaying { player.stop(note) } else { player.play(note) }
            impact()
        } else {
            startNote(at: index)
            activeTouches[id] = index
        }
    }

    func touchMoved(id: Int, to point: CGPoint) {
        guard !toggleMode, let currentCell = activeTouches[id] else { return }
        let newCell = geometry.cellIndex(at: point)
        guard newCell != currentCell else { return }
        activeTouches[id] = nil
        if currentCell < notes.count, !activeTouches.values.contains(currentCell) {
            player.stop(notes[currentCell])
        }
        if newCell >= 0, newCell < notes.count {
            startNote(at: newCell)
            activeTouches[id] = newCell
        }
    }

    func touchEnded(id: Int) {
        guard let cell = activeTouches[id] else { return }
        activeTouches[id] = nil
        if !toggleMode, cell < notes.count, !activeTouches.values.contains(cell) {
            player.stop(notes[cell])
        }
    }

    // MARK: Commands

    func stopAll() {
        player.stop(notes)
        activeTouches.removeAll()
    }

    func selectRange(high: Bool) {
        stopAll()
        if high != isHighRange {
            rangeFeedback.selectionChanged()
            rangeFeedback.prepare()
        }
        pipe.isFromFToF = high
        refresh()
        UIAccessibility.post(notification: .layoutChanged, argument: nil)
    }

    /// VoiceOver's activation: toggled notes flip; momentary ones sound for 1.5 s.
    func activate(cell index: Int) {
        guard notes.indices.contains(index) else { return }
        let note = notes[index]
        if toggleMode {
            if note.isPlaying { player.stop(note) } else { player.play(note) }
        } else {
            player.play(note)
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { [player] in
                player.stop(note)
            }
        }
    }

    private func startNote(at index: Int) {
        player.play(notes[index])
        impact()
    }

    private func impact() {
        noteFeedback.impactOccurred(intensity: 0.55)
        noteFeedback.prepare()
    }

    // MARK: Words

    private static let letters: [Character] = ["C", "D", "E", "F", "G", "A", "B"]

    private func flatPartner(of name: String) -> String {
        guard let first = name.first, let index = Self.letters.firstIndex(of: first) else { return name }
        return String(Self.letters[(index + 1) % Self.letters.count])
    }

    func spokenName(at index: Int) -> String {
        let note = notes[index]
        let name = note.friendlyName ?? ""
        if naturals.indices.contains(index), naturals[index] {
            return "\(name), octave \(note.octave)"
        }
        return "\(name) sharp, \(flatPartner(of: name)) flat, octave \(note.octave)"
    }

    /// The readout in the ring's hole: the sounding notes' names, then a frequency,
    /// an interval, a chord's name or a count.
    struct Readout: Equatable {
        var names: String
        var detail: String
        var isChord: Bool
    }

    var readout: Readout? {
        let playing = playingIndices
        guard !playing.isEmpty else { return nil }
        let names = playing.map { index -> String in
            let note = notes[index]
            let natural = naturals.indices.contains(index) && naturals[index]
            return "\(note.friendlyName ?? "")\(natural ? "" : "\u{266F}")\(note.octave)"
        }.joined(separator: " ")
        let chord = playing.count > 2 ? PitchChord.name(cells: playing) : nil
        let detail: String
        if let chord {
            detail = chord
        } else if playing.count == 1 {
            detail = String(format: "%.1f Hz", notes[playing[0]].frequency)
        } else if playing.count == 2 {
            detail = Self.intervalName(between: playing[0], and: playing[1])
        } else {
            detail = "\(playing.count) NOTES"
        }
        return Readout(names: names, detail: detail, isChord: chord != nil)
    }

    static func intervalName(between first: Int, and second: Int) -> String {
        let names = [
            "UNISON", "MINOR 2ND", "MAJOR 2ND", "MINOR 3RD",
            "MAJOR 3RD", "PERFECT 4TH", "TRITONE", "PERFECT 5TH",
            "MINOR 6TH", "MAJOR 6TH", "MINOR 7TH", "MAJOR 7TH", "OCTAVE",
        ]
        return names[min(abs(second - first), names.count - 1)]
    }
}

// MARK: - Drawing

/// Draws the face with the Core Graphics calls the UIKit instrument used.
struct InstrumentRenderer {
    let geometry: InstrumentGeometry
    let notes: [DPNote]
    let naturals: [Bool]
    let playing: Set<Int>
    let isHighRange: Bool
    let readout: PitchPipeModel.Readout?
    let breathePhase: CGFloat
    let traits: UITraitCollection

    func draw(in context: CGContext) {
        let ground = DPTheme.plateGround.resolvedColor(with: traits)
        let surface = DPTheme.plateSurface.resolvedColor(with: traits)
        let ink = DPTheme.plateInk.resolvedColor(with: traits)
        let inkSecondary = DPTheme.plateInkSecondary.resolvedColor(with: traits)
        let hairline = DPTheme.plateHairline.resolvedColor(with: traits)
        let lit = DPTheme.plateLit.resolvedColor(with: traits)
        let onLit = DPTheme.plateOnLit.resolvedColor(with: traits)
        let bounds = CGRect(origin: .zero, size: geometry.size)
        let cellRadius = geometry.cellRadius

        drawPanel(context: context, bounds: bounds, ground: ground, hairline: hairline, markColor: inkSecondary)

        let breath = 0.82 + 0.18 * sin(breathePhase)

        // Bloom pass beneath the glass.
        for index in notes.indices where index < geometry.cellCenters.count && playing.contains(index) {
            let center = geometry.cellCenters[index]
            let colors = [lit.withAlphaComponent(0.6 * breath).cgColor, lit.withAlphaComponent(0).cgColor] as CFArray
            if let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, 1]) {
                context.drawRadialGradient(gradient, startCenter: center, startRadius: 0,
                                           endCenter: center, endRadius: cellRadius * 2.4, options: [])
            }
        }

        for (index, note) in notes.enumerated() where index < geometry.cellCenters.count {
            let center = geometry.cellCenters[index]
            let isPlaying = playing.contains(index)
            let cellRect = geometry.cellRect(index)
            (isPlaying ? lit.withAlphaComponent(0.9 + 0.1 * sin(breathePhase)) : surface).setFill()
            context.fillEllipse(in: cellRect)
            context.setStrokeColor((isPlaying ? lit : hairline).cgColor)
            context.setLineWidth(isPlaying ? 2.5 : 1.2)
            context.strokeEllipse(in: cellRect)
            // Anode ring: the fine inner rim every cell carries.
            context.setStrokeColor((isPlaying ? onLit.withAlphaComponent(0.45) : inkSecondary.withAlphaComponent(0.3)).cgColor)
            context.setLineWidth(0.8)
            context.strokeEllipse(in: cellRect.insetBy(dx: cellRadius * 0.14, dy: cellRadius * 0.14))

            let natural = naturals.indices.contains(index) && naturals[index]
            let textColor = isPlaying ? onLit : (natural ? ink : inkSecondary)
            let label: NSAttributedString
            if natural {
                label = NSAttributedString(string: note.friendlyName ?? "",
                                           attributes: [.font: condensedFont(size: cellRadius * 0.9), .foregroundColor: textColor])
            } else {
                label = glyphLabel(size: cellRadius * 0.5, color: textColor)
            }
            let size = label.size()
            label.draw(at: CGPoint(x: center.x - size.width / 2, y: center.y - size.height / 2))
        }

        drawCenter(ink: ink, inkSecondary: inkSecondary)

        // One machined frame contains both range positions.
        let lowRect = geometry.rangeLowRect
        let highRect = geometry.rangeHighRect
        let frame = lowRect.union(highRect)
        surface.withAlphaComponent(0.92).setFill()
        let framePath = UIBezierPath(roundedRect: frame, cornerRadius: 4)
        framePath.fill()
        context.setStrokeColor(hairline.cgColor)
        context.setLineWidth(1.2)
        context.addPath(framePath.cgPath)
        context.strokePath()
        context.setLineWidth(0.8)
        context.setStrokeColor(hairline.withAlphaComponent(0.6).cgColor)
        context.move(to: CGPoint(x: lowRect.minX, y: lowRect.maxY))
        context.addLine(to: CGPoint(x: lowRect.maxX, y: lowRect.maxY))
        context.strokePath()
        drawRange(rect: lowRect, label: "C TO C", selected: !isHighRange, context: context, ink: ink, inkSecondary: inkSecondary, lit: lit)
        drawRange(rect: highRect, label: "F TO F", selected: isHighRange, context: context, ink: ink, inkSecondary: inkSecondary, lit: lit)

        // Nameplate.
        let ringRadius = geometry.ringRadius
        let nameplate = NSAttributedString(string: "DIGITAL PITCH PIPE", attributes: [
            .font: condensedFont(size: ringRadius * 0.08),
            .foregroundColor: inkSecondary.withAlphaComponent(0.65),
            .kern: ringRadius * 0.028,
        ])
        let nameplateSize = nameplate.size()
        nameplate.draw(at: CGPoint(x: bounds.midX - nameplateSize.width / 2, y: bounds.height - nameplateSize.height * 2.2))
    }

    private func condensedFont(size: CGFloat) -> UIFont {
        UIFont(name: "Oswald-Medium", size: size) ?? UIFont.systemFont(ofSize: size, weight: .regular, width: .condensed)
    }

    private func monoFont(size: CGFloat) -> UIFont {
        UIFont.monospacedSystemFont(ofSize: size, weight: .regular)
    }

    private func glyphLabel(size: CGFloat, color: UIColor) -> NSAttributedString {
        if let noteHedz = UIFont(name: "NoteHedz", size: size * 1.2) {
            let label = NSMutableAttributedString()
            label.append(NSAttributedString(string: Plate.sharpGlyph, attributes: [.font: noteHedz, .foregroundColor: color]))
            label.append(NSAttributedString(string: "/", attributes: [.font: condensedFont(size: size * 0.8), .foregroundColor: color, .baselineOffset: size * 0.15]))
            label.append(NSAttributedString(string: Plate.flatGlyph, attributes: [.font: noteHedz, .foregroundColor: color]))
            return label
        }
        return NSAttributedString(string: "\u{266F}/\u{266D}", attributes: [.font: condensedFont(size: size * 0.9), .foregroundColor: color])
    }

    private func drawPanel(context: CGContext, bounds: CGRect, ground: UIColor, hairline: UIColor, markColor: UIColor) {
        ground.setFill()
        context.fill(bounds)

        // Brushed-metal grain: fine directional noise across the whole panel.
        context.setLineWidth(1)
        var grainY: CGFloat = 0
        while grainY < bounds.height {
            let alpha = CGFloat(8 + (Int(grainY) * 31) % 14) / 255.0
            context.setStrokeColor(hairline.withAlphaComponent(alpha).cgColor)
            context.move(to: CGPoint(x: 0, y: grainY))
            context.addLine(to: CGPoint(x: bounds.width, y: grainY))
            context.strokePath()
            grainY += 4
        }

        guard let artwork = UIImage(named: "panobackground.png") else { return }
        let engraving = artwork.withTintColor(markColor, renderingMode: .alwaysOriginal)
        let tileHeight = bounds.width * (artwork.size.height / artwork.size.width)
        guard tileHeight > 0 else { return }
        var tileTop: CGFloat = 0
        while tileTop < bounds.height {
            engraving.draw(in: CGRect(x: 0, y: tileTop, width: bounds.width, height: tileHeight), blendMode: .normal, alpha: 0.10)
            tileTop += tileHeight
        }
    }

    private func drawCenter(ink: UIColor, inkSecondary: UIColor) {
        let faceCenter = geometry.faceCenter
        let ringRadius = geometry.ringRadius
        guard let readout else {
            let idle = NSAttributedString(string: "\u{2014} Hz", attributes: [
                .font: monoFont(size: ringRadius * 0.13),
                .foregroundColor: inkSecondary.withAlphaComponent(0.55),
            ])
            let idleSize = idle.size()
            idle.draw(at: CGPoint(x: faceCenter.x - idleSize.width / 2, y: faceCenter.y - ringRadius * 0.18))
            return
        }
        let nameSize2 = playing.count == 1 ? ringRadius * 0.28 : ringRadius * 0.16
        let nameLabel = NSAttributedString(string: readout.names, attributes: [.font: condensedFont(size: nameSize2), .foregroundColor: ink])
        let nameSize = nameLabel.size()
        nameLabel.draw(at: CGPoint(x: faceCenter.x - nameSize.width / 2, y: faceCenter.y - ringRadius * 0.46))
        // A chord has a name, not a measurement: engrave it in the display
        // face so the exclamation reads as one word.
        let attributes: [NSAttributedString.Key: Any] = readout.isChord
            ? [.font: condensedFont(size: ringRadius * 0.19), .foregroundColor: ink, .kern: ringRadius * 0.023]
            : [.font: monoFont(size: ringRadius * 0.13), .foregroundColor: ink]
        let detail = NSAttributedString(string: readout.detail, attributes: attributes)
        let detailSize = detail.size()
        let top = faceCenter.y - ringRadius * (readout.isChord ? 0.14 : 0.1)
        detail.draw(at: CGPoint(x: faceCenter.x - detailSize.width / 2, y: top))
    }

    private func drawRange(rect: CGRect, label: String, selected: Bool, context: CGContext, ink: UIColor, inkSecondary: UIColor, lit: UIColor) {
        if selected {
            ink.withAlphaComponent(0.1).setFill()
            context.fill(rect)
            lit.setFill()
            context.fillEllipse(in: CGRect(x: rect.minX + rect.height * 0.5, y: rect.midY - rect.height * 0.13,
                                           width: rect.height * 0.26, height: rect.height * 0.26))
        }
        let text = NSAttributedString(string: label, attributes: [
            .font: condensedFont(size: rect.height * 0.5),
            .foregroundColor: selected ? ink : inkSecondary.withAlphaComponent(0.75),
            .kern: rect.height * 0.14,
        ])
        let size = text.size()
        text.draw(at: CGPoint(x: rect.midX - size.width / 2, y: rect.midY - size.height / 2))
    }
}

// MARK: - View

struct PitchInstrumentView: View {
    @Bindable var model: PitchPipeModel
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// Tests freeze the breathing so a capture is deterministic.
    static var breathingFrozen = false

    var body: some View {
        GeometryReader { proxy in
            let geometry = InstrumentGeometry(size: proxy.size, count: model.notes.count)
            ZStack {
                if model.anyPlaying, !reduceMotion, !Self.breathingFrozen {
                    TimelineView(.animation) { timeline in
                        face(geometry, phase: Self.phase(at: timeline.date))
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
            }
            .onAppear { model.geometry = geometry }
            .onChange(of: geometry) { _, new in model.geometry = new }
        }
    }

    /// One breath every four seconds.
    private static let breathEpoch = Date()
    static func phase(at date: Date) -> CGFloat {
        let elapsed = date.timeIntervalSince(breathEpoch)
        return CGFloat((elapsed / 4).truncatingRemainder(dividingBy: 1)) * 2 * .pi
    }

    private func face(_ geometry: InstrumentGeometry, phase: CGFloat) -> some View {
        let renderer = InstrumentRenderer(
            geometry: geometry,
            notes: model.notes,
            naturals: model.naturals,
            playing: Set(model.playingIndices),
            isHighRange: model.isHighRange,
            readout: model.readout,
            breathePhase: phase,
            traits: UITraitCollection(userInterfaceStyle: colorScheme == .dark ? .dark : .light)
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

    @ViewBuilder
    private func accessibilityElements(_ geometry: InstrumentGeometry) -> some View {
        ZStack(alignment: .topLeading) {
            ForEach(Array(model.notes.indices), id: \.self) { index in
                if index < geometry.cellCenters.count {
                    let rect = geometry.cellRect(index)
                    element(rect, label: model.spokenName(at: index), selected: false) {
                        model.activate(cell: index)
                    }
                }
            }
            element(geometry.rangeLowRect, label: "Octave range C to C", selected: !model.isHighRange) {
                model.selectRange(high: false)
            }
            element(geometry.rangeHighRect, label: "Octave range F to F", selected: model.isHighRange) {
                model.selectRange(high: true)
            }
        }
        .frame(width: geometry.size.width, height: geometry.size.height, alignment: .topLeading)
        .allowsHitTesting(false)
    }

    private func element(_ rect: CGRect, label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Color.clear
            .frame(width: rect.width, height: rect.height)
            .offset(x: rect.minX, y: rect.minY)
            .accessibilityElement()
            .accessibilityLabel(label)
            .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
            .accessibilityAction { action() }
    }
}

/// Reports every finger separately, so chords and slides work as they do on a
/// real pitch pipe. SwiftUI's gestures follow one touch until iOS 18, so this
/// thin input surface is the one UIKit view the instrument keeps.
struct MultiTouchSurface: UIViewRepresentable {
    let began: (Int, CGPoint) -> Void
    let moved: (Int, CGPoint) -> Void
    let ended: (Int) -> Void

    final class Surface: UIView {
        var began: (Int, CGPoint) -> Void = { _, _ in }
        var moved: (Int, CGPoint) -> Void = { _, _ in }
        var ended: (Int) -> Void = { _ in }

        override init(frame: CGRect) {
            super.init(frame: frame)
            isMultipleTouchEnabled = true
            backgroundColor = .clear
            isAccessibilityElement = false
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

        private func id(_ touch: UITouch) -> Int { ObjectIdentifier(touch).hashValue }

        override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
            for touch in touches { began(id(touch), touch.location(in: self)) }
        }

        override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
            for touch in touches { moved(id(touch), touch.location(in: self)) }
        }

        override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
            for touch in touches { ended(id(touch)) }
        }

        override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
            for touch in touches { ended(id(touch)) }
        }
    }

    func makeUIView(context: Context) -> Surface { Surface() }

    func updateUIView(_ surface: Surface, context: Context) {
        surface.began = began
        surface.moved = moved
        surface.ended = ended
    }
}
