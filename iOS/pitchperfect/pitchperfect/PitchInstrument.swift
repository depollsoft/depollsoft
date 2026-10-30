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

/// What a face's touch surface asks of its layout. The radial face and the
/// classic grid each answer it, so the one model drives either.
protocol PitchFaceLayout {
    /// The cell under `point`; -1 for none.
    func cellIndex(at point: CGPoint) -> Int
    /// The range position under `point`: false for C to C, true for F to F,
    /// nil for neither.
    func range(at point: CGPoint) -> Bool?
}

/// Where everything sits on a face of `size` holding `count` cells.
///
/// A `fold` (an iPhone Duo's inner display, partly folded: see `Fold`) moves
/// the face off the crease rather than across it, like a bench instrument with
/// its display raised. Folded across (tabletop), the readout leaves the ring's
/// hole for a display window on the upper half and the ring sits on the lower
/// half, under the fingers; folded down the middle (book), the display takes the
/// leading page and the ring the other. Without a fold the face is as it always was.
struct InstrumentGeometry: Equatable, PitchFaceLayout {
    /// How a fold across the face arranges it.
    enum Posture: Equatable {
        /// No fold across the face: the ring with the readout in its hole.
        case flat
        /// Folded across: the display on the upper half, the ring below.
        case tabletop
        /// Folded down: the display on the leading page, the ring on the other.
        case book
    }

    /// The least room a raised display takes: below this the face stays whole
    /// and moves to the larger side of the fold instead.
    static let minimumDisplay = CGSize(width: 200, height: 120)

    let size: CGSize
    let posture: Posture
    /// Where the face is laid out: the whole panel, or the side of a fold it was moved to.
    let faceArea: CGRect
    let faceCenter: CGPoint
    let ringRadius: CGFloat
    let cellRadius: CGFloat
    let cellCenters: [CGPoint]
    let rangeLowRect: CGRect
    let rangeHighRect: CGRect
    /// Where a finger (or VoiceOver) picks each range: the drawn row, grown away from
    /// the other to 44 points tall when a raised display leaves the hole to it.
    let rangeLowTarget: CGRect
    let rangeHighTarget: CGRect
    /// The raised display window holding the readout; nil when the readout is in the ring's hole.
    let display: CGRect?
    /// The nameplate's line under a raised display; nil when it sits at the panel's foot.
    let nameplateArea: CGRect?

    /// `fold` is the band the fold covers, in the face's coordinates; `displayOnLeft`
    /// says which page is leading (false in a right-to-left layout).
    init(size: CGSize, count: Int, fold: CGRect? = nil, displayOnLeft: Bool = true) {
        self.size = size
        let count = max(count, 1)
        let cellFactor: CGFloat = count > 12 ? 0.225 : 0.245
        let bounds = CGRect(origin: .zero, size: size)
        let plan = Self.plan(bounds: bounds, fold: fold, displayOnLeft: displayOnLeft, cellFactor: cellFactor)
        posture = plan.posture
        faceArea = plan.face

        if let display = plan.display {
            // The ring alone, filling its side of the fold, with room round the cells.
            faceCenter = CGPoint(x: plan.face.midX, y: plan.face.midY)
            ringRadius = Self.foldedRingRadius(in: plan.face, cellFactor: cellFactor)
            // The display window is sized from the ring, not from its side of the
            // fold: no wider than 1.2 × the ring's glass, edge to edge, and 0.45 ×
            // as tall as wide, so the cells stay the face's largest part. With the
            // nameplate's line beneath it, it is centred across its side and sits
            // level with the ring on a book's page, or just above the crease in
            // tabletop, so the readout stays with the keys it reports on.
            let inset = max(16, min(display.width, display.height) * 0.07)
            let plate = min(max(display.height * 0.06, 14), 20)
            let available = display.insetBy(dx: inset, dy: inset)
            let ringWidth = 2 * ringRadius * (1 + cellFactor)
            let width = min(available.width, ringWidth * 1.2)
            let height = min(available.height - plate - inset * 0.5, width * 0.45)
            let blockHeight = height + inset * 0.5 + plate
            let centreY = posture == .book ? faceCenter.y : available.maxY - blockHeight / 2
            let top = min(max(centreY - blockHeight / 2, available.minY), available.maxY - blockHeight)
            let window = CGRect(x: available.midX - width / 2, y: top, width: width, height: max(height, 0))
            self.display = window
            nameplateArea = CGRect(x: window.minX, y: window.maxY + inset * 0.5, width: window.width, height: plate)
        } else {
            faceCenter = CGPoint(x: plan.face.minX + plan.face.width / 2, y: plan.face.minY + plan.face.height * 0.44)
            ringRadius = min(plan.face.width, plan.face.height * 0.82) * 0.365
            display = nil
            nameplateArea = nil
        }

        cellRadius = ringRadius * cellFactor
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
        // The range selector is one machined part seated in the ring's hole. With
        // the readout raised on the display, the hole is the selector's alone and
        // it fills the hole's middle.
        let rangeWidth = ringRadius * (display == nil ? 0.72 : 0.9)
        let rowHeight = ringRadius * (display == nil ? 0.145 : 0.22)
        let rangeTop = display == nil ? faceCenter.y + ringRadius * 0.20 : faceCenter.y - rowHeight
        rangeLowRect = CGRect(x: faceCenter.x - rangeWidth / 2, y: rangeTop, width: rangeWidth, height: rowHeight)
        rangeHighRect = CGRect(x: rangeLowRect.minX, y: rangeLowRect.maxY, width: rangeWidth, height: rowHeight)
        // The hole holds nothing else then, and two 44-point targets fit inside the
        // cells' own: in tabletop about 66 points from the centre at the corners, against 80.
        let reach = display == nil ? 0 : max(44 - rowHeight, 0)
        rangeLowTarget = CGRect(x: rangeLowRect.minX, y: rangeLowRect.minY - reach,
                                width: rangeWidth, height: rowHeight + reach)
        rangeHighTarget = CGRect(x: rangeHighRect.minX, y: rangeHighRect.minY,
                                 width: rangeWidth, height: rowHeight + reach)
    }

    /// A ring of cells filling `area`: its outermost touch target (1.15 × a cell)
    /// and a little air inside the area's shorter side.
    static func foldedRingRadius(in area: CGRect, cellFactor: CGFloat) -> CGFloat {
        min(area.width, area.height) / 2 / (1 + cellFactor * 1.15 + 0.08)
    }

    /// Where the face, and a raised display, go for `fold`.
    private static func plan(bounds: CGRect, fold: CGRect?, displayOnLeft: Bool, cellFactor: CGFloat)
        -> (posture: Posture, face: CGRect, display: CGRect?) {
        guard let sides = FoldSides(bounds: bounds, fold: fold) else { return (.flat, bounds, nil) }
        let (display, ring): (CGRect, CGRect)
        if sides.across {
            (display, ring) = (sides.first, sides.second)
        } else {
            (display, ring) = displayOnLeft ? (sides.first, sides.second) : (sides.second, sides.first)
        }
        // A fold near the face's edge (beside another app), one leaving the display
        // no room, or one that would shrink the cells' touch targets under 44 points
        // keeps the face whole on the larger side, off the crease.
        let touch = foldedRingRadius(in: ring, cellFactor: cellFactor) * cellFactor * 1.15 * 2
        if sides.parts, display.width >= minimumDisplay.width, display.height >= minimumDisplay.height, touch >= 44 {
            return (sides.across ? .tabletop : .book, ring, display)
        }
        return (.flat, sides.larger, nil)
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

    func range(at point: CGPoint) -> Bool? {
        if rangeLowTarget.contains(point) { return false }
        if rangeHighTarget.contains(point) { return true }
        return nil
    }
}

// MARK: - Fold

/// The two sides of a fold that runs right across a face. Nil when no fold
/// crosses the face.
struct FoldSides: Equatable {
    /// Folded across the face (top and bottom halves), not down it (left and right pages).
    let across: Bool
    /// The top half or the left page.
    let first: CGRect
    /// The bottom half or the right page.
    let second: CGRect

    /// The roomier side.
    var larger: CGRect {
        across ? (first.height >= second.height ? first : second) : (first.width >= second.width ? first : second)
    }

    /// Whether the fold parts the face fairly: each side at least 30% of it.
    /// A fold nearer an edge (beside another app) leaves the face whole on the larger side.
    var parts: Bool {
        across ? min(first.height, second.height) >= (first.height + second.height) * 0.3
               : min(first.width, second.width) >= (first.width + second.width) * 0.3
    }

    init?(bounds: CGRect, fold: CGRect?) {
        guard let fold, bounds.width > 0, bounds.height > 0 else { return nil }
        let band = fold.intersection(bounds)
        guard !band.isNull else { return nil }
        // A fold divides the whole display, so it runs right across the face or right down it.
        if band.width >= bounds.width - 0.5, band.width > band.height {
            across = true
            first = CGRect(x: bounds.minX, y: bounds.minY, width: bounds.width, height: band.minY - bounds.minY)
            second = CGRect(x: bounds.minX, y: band.maxY, width: bounds.width, height: bounds.maxY - band.maxY)
        } else if band.height >= bounds.height - 0.5, band.height > band.width {
            across = false
            first = CGRect(x: bounds.minX, y: bounds.minY, width: band.minX - bounds.minX, height: bounds.height)
            second = CGRect(x: band.maxX, y: bounds.minY, width: bounds.maxX - band.maxX, height: bounds.height)
        } else {
            return nil
        }
    }
}

/// The fold of an iPhone Duo's inner display while it is partly folded: a band
/// content should not straddle. iOS reports it as an active division region;
/// a flat or closed Duo, and every other device, report none.
@MainActor
enum Fold {
    /// Tests stand a fold in: given the face's frame in its window, the band in
    /// window coordinates (nil for none).
    static var simulated: ((CGRect) -> CGRect?)?

    /// The active fold across the view `proxy` measures, in its coordinates: the
    /// region's frame, which holds the crease and the margins iOS asks interactive
    /// content to keep from it.
    static func band(in proxy: GeometryProxy) -> CGRect? {
        if let simulated {
            let frame = proxy.frame(in: .global)
            return simulated(frame).map { $0.offsetBy(dx: -frame.minX, dy: -frame.minY) }
        }
        guard #available(iOS 27.1, *) else { return nil }
        // Fixed, not mirrored: the face lays itself out in left-to-right coordinates
        // and picks its leading page itself.
        let regions = proxy.reservedRegions(kind: .division, layoutDirectionBehavior: .fixed)
        return regions.first(where: \.isActive)?.frame
    }
}

extension View {
    /// Lays the view out again whenever the hinge opens or folds, so a face
    /// reading `Fold.band` meets the fold as it comes and goes.
    func refreshesOnFold(_ status: Binding<Int>) -> some View {
        hingeStatus(status)
    }

    /// Centres the view, top-aligned, on the space it is given, or on the
    /// leading page when a fold runs down that space (an iPhone Duo held like
    /// a book), so a centred prompt never sits on the crease.
    func centredOffTheFold() -> some View {
        modifier(CentredOffTheFold())
    }
}

private struct CentredOffTheFold: ViewModifier {
    @Environment(\.layoutDirection) private var layoutDirection
    @State private var hingeStatus = 0

    func body(content: Content) -> some View {
        GeometryReader { proxy in
            let _ = hingeStatus
            let bounds = CGRect(origin: .zero, size: proxy.size)
            let sides = FoldSides(bounds: bounds, fold: Fold.band(in: proxy))
            let page = sides.flatMap { $0.across || !$0.parts ? nil : $0 }
                .map { layoutDirection == .leftToRight ? $0.first : $0.second } ?? bounds
            content
                .frame(width: page.width)
                .frame(maxHeight: .infinity, alignment: .top)
                .offset(x: page.minX)
        }
        .refreshesOnFold($hingeStatus)
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
    /// The classic grid of big buttons shows in place of the radial face (a
    /// Settings choice).
    private(set) var isClassic = false
    /// Touch id → the cell it holds.
    private(set) var activeTouches: [Int: Int] = [:]
    var geometry = InstrumentGeometry(size: .zero, count: 13)
    var classicGeometry = ClassicGeometry(size: .zero, count: 13)

    /// The layout fingers are hit-tested against: whichever face shows.
    var layout: any PitchFaceLayout { isClassic ? classicGeometry : geometry }

    private let noteFeedback = UIImpactFeedbackGenerator(style: .rigid)
    private let rangeFeedback = UISelectionFeedbackGenerator()
    @ObservationIgnored private var settingsObserver: NSObjectProtocol?

    init(pipe: DPPitchPipeModel = DPPitchPipeModel(),
         player: NotePlayer = .shared,
         settings: DPSettingsModel = .sharedInstance) {
        self.pipe = pipe
        self.player = player
        self.settings = settings
        // Warmed up front, as the UIKit instrument did, so the first press is prompt.
        noteFeedback.prepare()
        rangeFeedback.prepare()
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

    /// Reloads the range's notes (and which are naturals) from the stored range,
    /// and which face shows. Changing face silences the instrument.
    func refresh() {
        isHighRange = pipe.isFromFToF
        notes = pipe.notes as? [DPNote] ?? []
        naturals = notes.map { Int($0.accidental.get()) == Int(Natural.rawValue) }
        let classic = settings.classicPitchPipe
        if classic != isClassic {
            stopAll()
            isClassic = classic
        }
    }

    func isPlaying(_ index: Int) -> Bool {
        notes.indices.contains(index) && player.isPlaying(notes[index])
    }

    var playingIndices: [Int] { notes.indices.filter { player.isPlaying(notes[$0]) } }
    var anyPlaying: Bool { !playingIndices.isEmpty }

    // MARK: Touch

    func touchBegan(id: Int, at point: CGPoint) {
        let layout = layout
        if activeTouches.isEmpty, let high = layout.range(at: point) {
            selectRange(high: high)
            return
        }
        let index = layout.cellIndex(at: point)
        guard index >= 0, index < notes.count else { return }
        if toggleMode {
            let note = notes[index]
            if note.isPlaying {
                player.stop(note)
            } else {
                player.play(note)
                reportPlayed()
            }
            impact()
        } else {
            startNote(at: index)
            reportPlayed()
            activeTouches[id] = index
        }
    }

    func touchMoved(id: Int, to point: CGPoint) {
        guard !toggleMode, let currentCell = activeTouches[id] else { return }
        let newCell = layout.cellIndex(at: point)
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

    /// The classic grid's range control chose `high`. As on the radial face, a
    /// second finger cannot yank the range from under a held note.
    func pickRange(high: Bool) {
        guard activeTouches.isEmpty else { return }
        selectRange(high: high)
    }

    /// VoiceOver's activation: toggled notes flip; momentary ones sound for 1.5 s.
    func activate(cell index: Int) {
        guard notes.indices.contains(index) else { return }
        let note = notes[index]
        if toggleMode {
            if note.isPlaying {
                player.stop(note)
            } else {
                player.play(note)
                reportPlayed()
            }
        } else {
            player.play(note)
            reportPlayed()
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { [player] in
                player.stop(note)
            }
        }
    }

    /// A press or activation started a note; sliding onto a neighbour does not count.
    private func reportPlayed() {
        player.reportPlayed(isClassic ? .classicPitchPipe : .pitchPipe)
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
            detail = String(format: "%.1f Hz", player.frequency(of: notes[playing[0]]))
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

        Self.drawPanel(context: context, bounds: bounds, ground: ground, hairline: hairline, markColor: inkSecondary)

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
                                           attributes: [.font: DPTheme.condensedFont(size: cellRadius * 0.9), .foregroundColor: textColor])
            } else {
                label = Self.glyphLabel(size: cellRadius * 0.5, color: textColor)
            }
            let size = label.size()
            label.draw(at: CGPoint(x: center.x - size.width / 2, y: center.y - size.height / 2))
        }

        if let display = geometry.display {
            drawDisplay(display, context: context, surface: surface, ink: ink, inkSecondary: inkSecondary,
                        hairline: hairline)
        } else {
            drawCenter(ink: ink, inkSecondary: inkSecondary)
        }

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

        // Nameplate: at the panel's foot, or engraved under a raised display.
        if let area = geometry.nameplateArea {
            let size = area.height * 0.72
            let nameplate = NSAttributedString(string: "DIGITAL PITCH PIPE", attributes: [
                .font: DPTheme.condensedFont(size: size),
                .foregroundColor: inkSecondary.withAlphaComponent(0.65),
                .kern: size * 0.34,
            ])
            let nameplateSize = nameplate.size()
            nameplate.draw(at: CGPoint(x: area.midX - nameplateSize.width / 2, y: area.midY - nameplateSize.height / 2))
        } else {
            let ringRadius = geometry.ringRadius
            let face = geometry.faceArea
            let nameplate = NSAttributedString(string: "DIGITAL PITCH PIPE", attributes: [
                .font: DPTheme.condensedFont(size: ringRadius * 0.08),
                .foregroundColor: inkSecondary.withAlphaComponent(0.65),
                .kern: ringRadius * 0.028,
            ])
            let nameplateSize = nameplate.size()
            nameplate.draw(at: CGPoint(x: face.midX - nameplateSize.width / 2, y: face.maxY - nameplateSize.height * 2.2))
        }
    }

    /// The raised display: one machined window, a plate-surface glass in a
    /// hairline frame with a fine inner rim, the readout large inside it. Like
    /// the range selector it is a part of the plate, not a light: only a
    /// sounding cell glows.
    private func drawDisplay(_ window: CGRect, context: CGContext, surface: UIColor, ink: UIColor,
                             inkSecondary: UIColor, hairline: UIColor) {
        guard window.width > 0, window.height > 0 else { return }
        let frame = UIBezierPath(roundedRect: window, cornerRadius: 5)
        surface.withAlphaComponent(0.92).setFill()
        frame.fill()
        context.setStrokeColor(hairline.cgColor)
        context.setLineWidth(1.5)
        context.addPath(frame.cgPath)
        context.strokePath()
        context.setStrokeColor(inkSecondary.withAlphaComponent(0.3).cgColor)
        context.setLineWidth(0.8)
        context.addPath(UIBezierPath(roundedRect: window.insetBy(dx: 5, dy: 5), cornerRadius: 3).cgPath)
        context.strokePath()
        ReadoutPainter.draw(readout, in: window.insetBy(dx: 14, dy: 14), ink: ink, inkSecondary: inkSecondary,
                            display: true)
    }

    /// The accidental cells' engraving: the NoteHedz sharp and flat either side of a slash.
    static func glyphLabel(size: CGFloat, color: UIColor) -> NSAttributedString {
        if let noteHedz = UIFont(name: "NoteHedz", size: size * 1.2) {
            let label = NSMutableAttributedString()
            label.append(NSAttributedString(string: Plate.sharpGlyph, attributes: [.font: noteHedz, .foregroundColor: color]))
            label.append(NSAttributedString(string: "/", attributes: [.font: DPTheme.condensedFont(size: size * 0.8), .foregroundColor: color, .baselineOffset: size * 0.15]))
            label.append(NSAttributedString(string: Plate.flatGlyph, attributes: [.font: noteHedz, .foregroundColor: color]))
            return label
        }
        return NSAttributedString(string: "\u{266F}/\u{266D}", attributes: [.font: DPTheme.condensedFont(size: size * 0.9), .foregroundColor: color])
    }

    /// The panel under a face: plate ground, brushed grain and the engraved score.
    static func drawPanel(context: CGContext, bounds: CGRect, ground: UIColor, hairline: UIColor, markColor: UIColor) {
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
                .font: DPTheme.monospacedFont(size: ringRadius * 0.13),
                .foregroundColor: inkSecondary.withAlphaComponent(0.55),
            ])
            let idleSize = idle.size()
            idle.draw(at: CGPoint(x: faceCenter.x - idleSize.width / 2, y: faceCenter.y - ringRadius * 0.18))
            return
        }
        let nameSize2 = playing.count == 1 ? ringRadius * 0.28 : ringRadius * 0.16
        let nameLabel = NSAttributedString(string: readout.names, attributes: [.font: DPTheme.condensedFont(size: nameSize2), .foregroundColor: ink])
        let nameSize = nameLabel.size()
        nameLabel.draw(at: CGPoint(x: faceCenter.x - nameSize.width / 2, y: faceCenter.y - ringRadius * 0.46))
        // A chord has a name, not a measurement: engrave it in the display
        // face so the exclamation reads as one word.
        let attributes: [NSAttributedString.Key: Any] = readout.isChord
            ? [.font: DPTheme.condensedFont(size: ringRadius * 0.19), .foregroundColor: ink, .kern: ringRadius * 0.023]
            : [.font: DPTheme.monospacedFont(size: ringRadius * 0.13), .foregroundColor: ink]
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
            .font: DPTheme.condensedFont(size: rect.height * 0.5),
            .foregroundColor: selected ? ink : inkSecondary.withAlphaComponent(0.75),
            .kern: rect.height * 0.14,
        ])
        let size = text.size()
        text.draw(at: CGPoint(x: rect.midX - size.width / 2, y: rect.midY - size.height / 2))
    }
}

/// The readout set in an area of its own (the classic grid's well, a folded
/// face's raised display): the sounding notes, then a frequency, an interval, a
/// chord's name or a count, as one centred block; "— Hz" when silent. Names at
/// min(0.46 × the height, 0.22 × the width), the detail at half that (a chord's
/// name at 0.62), each line shrunk to fit; a readout shorter than
/// `ClassicGeometry.minReadout` reads as one line.
enum ReadoutPainter {
    /// `display` is a folded face's raised display: the names are not held to
    /// 0.22 × the width, so one note fills the window and a chord's names shrink
    /// to fit it, and the whole block shrinks to the window's height.
    static func draw(_ readout: PitchPipeModel.Readout?, in area: CGRect, ink: UIColor, inkSecondary: UIColor,
                     display: Bool = false) {
        guard area.width > 0, area.height > 0 else { return }
        if area.height < ClassicGeometry.minReadout {
            drawLine(readout, in: area, ink: ink, inkSecondary: inkSecondary)
            return
        }
        let fitsHeight = display
        let base = display ? area.height * 0.46 : min(area.height * 0.46, area.width * 0.22)
        let block = layout(readout, area: area, nameSize: base, ink: ink, inkSecondary: inkSecondary)
        let height = block.reduce(0) { $0 + $1.size().height } + (block.count > 1 ? base * 0.15 : 0)
        let limit = area.height * 0.92
        let lines = fitsHeight && height > limit
            ? layout(readout, area: area, nameSize: base * limit / height, ink: ink, inkSecondary: inkSecondary)
            : block
        let nameSize = fitsHeight && height > limit ? base * limit / height : base
        let spacing = lines.count > 1 ? nameSize * 0.15 : 0
        let total = lines.reduce(0) { $0 + $1.size().height } + spacing
        var top = area.midY - total / 2
        for line in lines {
            let size = line.size()
            line.draw(at: CGPoint(x: area.midX - size.width / 2, y: top))
            top += size.height + spacing
        }
    }

    /// The block's lines at `nameSize`, each fitted to the area's width.
    private static func layout(_ readout: PitchPipeModel.Readout?, area: CGRect, nameSize: CGFloat, ink: UIColor,
                               inkSecondary: UIColor) -> [NSAttributedString] {
        let detailSize = nameSize * 0.5
        let chordSize = nameSize * 0.62
        let fit = CGSize(width: area.width * 0.92, height: .greatestFiniteMagnitude)
        guard let readout else {
            return [fitted(within: fit) { scale in
                NSAttributedString(string: "\u{2014} Hz", attributes: [
                    .font: DPTheme.monospacedFont(size: detailSize * scale),
                    .foregroundColor: inkSecondary.withAlphaComponent(0.55),
                ])
            }]
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
        return [names, detail]
    }

    /// The readout as one line: the names, then the detail after two spaces.
    private static func drawLine(_ readout: PitchPipeModel.Readout?, in area: CGRect, ink: UIColor,
                                 inkSecondary: UIColor) {
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

    /// The label `make` builds at scale 1, or smaller until it fits `bounds`.
    static func fitted(within bounds: CGSize, _ make: (CGFloat) -> NSAttributedString) -> NSAttributedString {
        let label = make(1)
        let size = label.size()
        guard size.width > 0, size.height > 0 else { return label }
        let scale = min(1, bounds.width / size.width, bounds.height / size.height)
        return scale < 1 ? make(scale) : label
    }
}

// MARK: - View

struct PitchInstrumentView: View {
    @Bindable var model: PitchPipeModel
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.layoutDirection) private var layoutDirection
    /// The hinge's state, so the face is laid out again as a fold comes and goes.
    @State private var hingeStatus = 0

    /// Tests freeze the breathing so a capture is deterministic.
    static var breathingFrozen = false

    var body: some View {
        GeometryReader { proxy in
            let _ = hingeStatus
            let geometry = InstrumentGeometry(size: proxy.size, count: model.notes.count,
                                              fold: Fold.band(in: proxy),
                                              displayOnLeft: layoutDirection == .leftToRight)
            ZStack {
                Group {
                    if model.anyPlaying, !reduceMotion, !Self.breathingFrozen {
                        TimelineView(.animation) { timeline in
                            face(geometry, phase: Self.phase(at: timeline.date))
                        }
                    } else {
                        face(geometry, phase: 0)
                    }
                }
                // Folding or opening crossfades the face to its new arrangement.
                .id(geometry.posture)
                .transition(.opacity)
                MultiTouchSurface(
                    began: { id, point in model.touchBegan(id: id, at: point) },
                    moved: { id, point in model.touchMoved(id: id, to: point) },
                    ended: { id in model.touchEnded(id: id) }
                )
                accessibilityElements(geometry)
            }
            .animation(reduceMotion ? nil : .easeInOut(duration: 0.3), value: geometry.posture)
            .onAppear { model.geometry = geometry }
            .onChange(of: geometry) { _, new in model.geometry = new }
            // The cells VoiceOver knew have moved: let it find them again.
            .onChange(of: geometry.posture) { UIAccessibility.post(notification: .layoutChanged, argument: nil) }
        }
        .refreshesOnFold($hingeStatus)
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
            // A raised display is half the face (a book's whole leading page): it reads
            // out what the window shows, first, as the eye meets it.
            if let display = geometry.display {
                Color.clear
                    .frame(width: display.width, height: display.height)
                    .offset(x: display.minX, y: display.minY)
                    .accessibilityElement()
                    .accessibilityLabel("Display")
                    .accessibilityValue(model.readout.map { "\($0.names), \($0.detail)" } ?? "Silent")
                    .accessibilityAddTraits(.updatesFrequently)
            }
            ForEach(Array(model.notes.indices), id: \.self) { index in
                if index < geometry.cellCenters.count {
                    let rect = geometry.cellRect(index)
                    element(rect, label: model.spokenName(at: index), selected: false) {
                        model.activate(cell: index)
                    }
                }
            }
            element(geometry.rangeLowTarget, label: "Octave range C to C", selected: !model.isHighRange) {
                model.selectRange(high: false)
            }
            element(geometry.rangeHighTarget, label: "Octave range F to F", selected: model.isHighRange) {
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
