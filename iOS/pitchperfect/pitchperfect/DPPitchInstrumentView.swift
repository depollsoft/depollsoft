//
//  DPPitchInstrumentView.swift
//  pitchperfect
//
//  The Laboratory Instrument: a custom-drawn radial pitch-pipe face.
//  Twelve glass cells on a blackened-steel panel; the sounding cell is the
//  one luminous element on screen. Frequency digits and the octave-range
//  selector live in the ring's hole.
//

import Foundation
import UIKit

private class InstrumentAccessibilityElement: UIAccessibilityElement {
    var onActivate: (() -> Bool)?

    override func accessibilityActivate() -> Bool {
        onActivate?() ?? false
    }
}

@objc public class DPPitchInstrumentView: UIView {
    @objc public var toggleMode: Bool = false
    @objc public var isHighRange: Bool = false {
        didSet { setNeedsDisplay() }
    }
    @objc public var onRangeChange: ((Bool) -> Void)?

    private var notes: [DPNote] = []
    private var naturals: [Bool] = []
    private var cellCenters: [CGPoint] = []
    private var cellRadius: CGFloat = 0
    private var ringRadius: CGFloat = 0
    private var faceCenter: CGPoint = .zero
    private var rangeLowRect: CGRect = .zero
    private var rangeHighRect: CGRect = .zero
    private var activeTouches: [ObjectIdentifier: Int] = [:]
    private var breathePhase: CGFloat = 0
    private var displayLink: CADisplayLink?
    private let noteFeedback = UIImpactFeedbackGenerator(style: .rigid)
    private let rangeFeedback = UISelectionFeedbackGenerator()

    override public init(frame: CGRect) {
        super.init(frame: frame)
        isOpaque = true
        isMultipleTouchEnabled = true
        isAccessibilityElement = false
        noteFeedback.prepare()
        rangeFeedback.prepare()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    @objc public func setNotes(_ notes: [DPNote], naturals: [NSNumber]) {
        self.notes = notes
        self.naturals = naturals.map { $0.boolValue }
        setNeedsLayout()
        rebuildAccessibilityElements()
        setNeedsDisplay()
    }

    @objc public func stopAll() {
        for note in notes {
            note.stop()
        }
        activeTouches.removeAll()
        setNeedsDisplay()
    }

    @objc public func refreshDisplay() {
        setNeedsDisplay()
    }

    // MARK: - Names

    private static let letters: [Character] = ["C", "D", "E", "F", "G", "A", "B"]

    // MARK: - Layout

    override public func layoutSubviews() {
        super.layoutSubviews()
        let panelWidth = bounds.width
        let panelHeight = bounds.height
        guard panelWidth > 0, panelHeight > 0 else { return }
        faceCenter = CGPoint(x: panelWidth / 2, y: panelHeight * 0.44)
        ringRadius = min(panelWidth, panelHeight * 0.82) * 0.365
        let count = max(notes.count, 1)
        cellRadius = ringRadius * (count > 12 ? 0.225 : 0.245)
        let step = 360.0 / Double(count)
        let start = -90.0 + step / 2.0
        cellCenters = (0..<count).map { index in
            let angle = (start + Double(index) * step) * Double.pi / 180.0
            return CGPoint(
                x: faceCenter.x + ringRadius * CGFloat(cos(angle)),
                y: faceCenter.y + ringRadius * CGFloat(sin(angle)),
            )
        }
        // The range selector is one machined part seated in the ring's hole.
        let rangeWidth = ringRadius * 0.72
        let rowHeight = ringRadius * 0.145
        let rangeTop = faceCenter.y + ringRadius * 0.20
        rangeLowRect = CGRect(
            x: faceCenter.x - rangeWidth / 2, y: rangeTop,
            width: rangeWidth, height: rowHeight,
        )
        rangeHighRect = CGRect(
            x: rangeLowRect.minX, y: rangeLowRect.maxY,
            width: rangeWidth, height: rowHeight,
        )
        rebuildAccessibilityElements()
        setNeedsDisplay()
    }

    // MARK: - Breathing

    override public func willMove(toWindow newWindow: UIWindow?) {
        super.willMove(toWindow: newWindow)
        if newWindow == nil {
            displayLink?.invalidate()
            displayLink = nil
        }
    }

    // MARK: - Drawing

    override public func draw(_ rect: CGRect) {
        guard let context = UIGraphicsGetCurrentContext() else { return }
        let traits = traitCollection
        let ground = DPTheme.plateGround.resolvedColor(with: traits)
        let surface = DPTheme.plateSurface.resolvedColor(with: traits)
        let ink = DPTheme.plateInk.resolvedColor(with: traits)
        let inkSecondary = DPTheme.plateInkSecondary.resolvedColor(with: traits)
        let hairline = DPTheme.plateHairline.resolvedColor(with: traits)
        let lit = DPTheme.plateLit.resolvedColor(with: traits)
        let onLit = DPTheme.plateOnLit.resolvedColor(with: traits)

        drawPanel(context: context, ground: ground, hairline: hairline, markColor: inkSecondary)

        let breath = 0.82 + 0.18 * sin(breathePhase)

        // Bloom pass beneath the glass.
        for (index, note) in notes.enumerated() where index < cellCenters.count && note.isPlaying {
            let center = cellCenters[index]
            let bloomRadius = cellRadius * 2.4
            let colors = [
                lit.withAlphaComponent(0.6 * breath).cgColor,
                lit.withAlphaComponent(0).cgColor,
            ] as CFArray
            if let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, 1]) {
                context.drawRadialGradient(
                    gradient,
                    startCenter: center, startRadius: 0,
                    endCenter: center, endRadius: bloomRadius,
                    options: [],
                )
            }
        }

        for (index, note) in notes.enumerated() where index < cellCenters.count {
            let center = cellCenters[index]
            let playing = note.isPlaying
            let cellRect = CGRect(
                x: center.x - cellRadius, y: center.y - cellRadius,
                width: cellRadius * 2, height: cellRadius * 2,
            )
            (playing ? lit.withAlphaComponent(0.9 + 0.1 * sin(breathePhase)) : surface).setFill()
            context.fillEllipse(in: cellRect)
            context.setStrokeColor((playing ? lit : hairline).cgColor)
            context.setLineWidth(playing ? 2.5 : 1.2)
            context.strokeEllipse(in: cellRect)
            // Anode ring: the fine inner rim every cell carries.
            context.setStrokeColor((playing ? onLit.withAlphaComponent(0.45) : inkSecondary.withAlphaComponent(0.3)).cgColor)
            context.setLineWidth(0.8)
            context.strokeEllipse(in: cellRect.insetBy(dx: cellRadius * 0.14, dy: cellRadius * 0.14))

            let textColor = playing ? onLit : (naturals.indices.contains(index) && naturals[index] ? ink : inkSecondary)
            let label: NSAttributedString
            if naturals.indices.contains(index), naturals[index] {
                label = NSAttributedString(
                    string: note.friendlyName ?? "",
                    attributes: [.font: condensedFont(size: cellRadius * 0.9), .foregroundColor: textColor],
                )
            } else {
                label = glyphLabel(size: cellRadius * 0.5, color: textColor)
            }
            let size = label.size()
            label.draw(at: CGPoint(x: center.x - size.width / 2, y: center.y - size.height / 2))
        }

        drawCenter(context: context, ink: ink, inkSecondary: inkSecondary)

        // One machined frame contains both range positions.
        let frame = rangeLowRect.union(rangeHighRect)
        surface.withAlphaComponent(0.92).setFill()
        let framePath = UIBezierPath(roundedRect: frame, cornerRadius: 4)
        framePath.fill()
        context.setStrokeColor(hairline.cgColor)
        context.setLineWidth(1.2)
        context.addPath(framePath.cgPath)
        context.strokePath()
        context.setLineWidth(0.8)
        context.setStrokeColor(hairline.withAlphaComponent(0.6).cgColor)
        context.move(to: CGPoint(x: rangeLowRect.minX, y: rangeLowRect.maxY))
        context.addLine(to: CGPoint(x: rangeLowRect.maxX, y: rangeLowRect.maxY))
        context.strokePath()
        drawRange(rect: rangeLowRect, label: "C TO C", selected: !isHighRange, context: context, ink: ink, inkSecondary: inkSecondary, hairline: hairline, lit: lit)
        drawRange(rect: rangeHighRect, label: "F TO F", selected: isHighRange, context: context, ink: ink, inkSecondary: inkSecondary, hairline: hairline, lit: lit)

        // Nameplate.
        let nameplate = NSAttributedString(
            string: "DIGITAL PITCH PIPE",
            attributes: [
                .font: condensedFont(size: ringRadius * 0.08),
                .foregroundColor: inkSecondary.withAlphaComponent(0.65),
                .kern: ringRadius * 0.028,
            ],
        )
        let nameplateSize = nameplate.size()
        nameplate.draw(at: CGPoint(x: bounds.midX - nameplateSize.width / 2, y: bounds.height - nameplateSize.height * 2.2))

        manageBreathing()
    }

    // MARK: - Touch

    override public func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            let point = touch.location(in: self)
            if activeTouches.isEmpty, rangeLowRect.contains(point) {
                selectRange(high: false)
                continue
            }
            if activeTouches.isEmpty, rangeHighRect.contains(point) {
                selectRange(high: true)
                continue
            }
            let index = cellIndex(at: point)
            guard index >= 0, index < notes.count else { continue }
            if toggleMode {
                let note = notes[index]
                if note.isPlaying { note.stop() } else { note.play() }
                noteFeedback.impactOccurred(intensity: 0.55)
                noteFeedback.prepare()
            } else {
                startNote(at: index)
                activeTouches[ObjectIdentifier(touch)] = index
            }
        }
        setNeedsDisplay()
    }

    override public func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard !toggleMode else { return }
        var changed = false
        for touch in touches {
            let key = ObjectIdentifier(touch)
            guard let currentCell = activeTouches[key] else { continue }
            let newCell = cellIndex(at: touch.location(in: self))
            if newCell != currentCell {
                activeTouches[key] = nil
                if currentCell < notes.count, !activeTouches.values.contains(currentCell) {
                    notes[currentCell].stop()
                }
                if newCell >= 0, newCell < notes.count {
                    startNote(at: newCell)
                    activeTouches[key] = newCell
                }
                changed = true
            }
        }
        if changed { setNeedsDisplay() }
    }

    override public func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        releaseTouches(touches)
    }

    override public func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        releaseTouches(touches)
    }


}

private extension DPPitchInstrumentView {
    private func startNote(at index: Int) {
        notes[index].play()
        noteFeedback.impactOccurred(intensity: 0.55)
        noteFeedback.prepare()
    }

    private func flatPartner(of name: String) -> String {
        guard let first = name.first, let index = Self.letters.firstIndex(of: first) else { return name }
        return String(Self.letters[(index + 1) % Self.letters.count])
    }

    private func spokenName(at index: Int) -> String {
        let note = notes[index]
        let name = note.friendlyName ?? ""
        if naturals.indices.contains(index), naturals[index] {
            return "\(name), octave \(note.octave)"
        }
        return "\(name) sharp, \(flatPartner(of: name)) flat, octave \(note.octave)"
    }

    private func anyPlaying() -> Bool {
        notes.contains { $0.isPlaying }
    }

    private func manageBreathing() {
        if anyPlaying(), !UIAccessibility.isReduceMotionEnabled {
            if displayLink == nil {
                let link = CADisplayLink(target: self, selector: #selector(breathe))
                link.add(to: .main, forMode: .common)
                displayLink = link
            }
        } else {
            displayLink?.invalidate()
            displayLink = nil
            breathePhase = 0
        }
    }

    @objc private func breathe() {
        breathePhase += CGFloat.pi * 2 / (4.0 * 60.0)
        if breathePhase > CGFloat.pi * 2 { breathePhase -= CGFloat.pi * 2 }
        setNeedsDisplay()
    }

    // MARK: - Fonts

    private func condensedFont(size: CGFloat) -> UIFont {
        if let oswald = UIFont(name: "Oswald-Medium", size: size) {
            return oswald
        }
        if #available(iOS 16.0, *) {
            return UIFont.systemFont(ofSize: size, weight: .regular, width: .condensed)
        }
        return UIFont.systemFont(ofSize: size, weight: .regular)
    }

    private func monoFont(size: CGFloat) -> UIFont {
        UIFont.monospacedSystemFont(ofSize: size, weight: .regular)
    }

    private func glyphLabel(size: CGFloat, color: UIColor) -> NSAttributedString {
        if let noteHedz = UIFont(name: "NoteHedz", size: size * 1.2) {
            let label = NSMutableAttributedString()
            label.append(NSAttributedString(string: "\u{00EC}", attributes: [.font: noteHedz, .foregroundColor: color]))
            label.append(NSAttributedString(string: "/", attributes: [.font: condensedFont(size: size * 0.8), .foregroundColor: color, .baselineOffset: size * 0.15]))
            label.append(NSAttributedString(string: "\u{00ED}", attributes: [.font: noteHedz, .foregroundColor: color]))
            return label
        }
        return NSAttributedString(
            string: "\u{266F}/\u{266D}",
            attributes: [.font: condensedFont(size: size * 0.9), .foregroundColor: color],
        )
    }

    private func drawPanel(
        context: CGContext,
        ground: UIColor,
        hairline: UIColor,
        markColor: UIColor
    ) {
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

        drawHeritageBackground(markColor: markColor)
    }

    private func drawHeritageBackground(markColor: UIColor) {
        guard let artwork = UIImage(named: "panobackground.png") else { return }
        let engraving = artwork.withTintColor(markColor, renderingMode: .alwaysOriginal)
        let tileHeight = bounds.width * (artwork.size.height / artwork.size.width)
        guard tileHeight > 0 else { return }

        var tileTop: CGFloat = 0
        while tileTop < bounds.height {
            engraving.draw(
                in: CGRect(x: 0, y: tileTop, width: bounds.width, height: tileHeight),
                blendMode: .normal,
                alpha: 0.10
            )
            tileTop += tileHeight
        }
    }

    private func drawCenter(context: CGContext, ink: UIColor, inkSecondary: UIColor) {
        let playingIndices = notes.indices.filter { notes[$0].isPlaying }
        if !playingIndices.isEmpty {
            let name = playingIndices
                .map { index -> String in
                    let note = notes[index]
                    let natural = naturals.indices.contains(index) && naturals[index]
                    return "\(note.friendlyName ?? "")\(natural ? "" : "\u{266F}")\(note.octave)"
                }
                .joined(separator: " ")
            let nameSize2 = playingIndices.count == 1 ? ringRadius * 0.28 : ringRadius * 0.16
            let nameLabel = NSAttributedString(
                string: name,
                attributes: [.font: condensedFont(size: nameSize2), .foregroundColor: ink],
            )
            let nameSize = nameLabel.size()
            nameLabel.draw(at: CGPoint(x: faceCenter.x - nameSize.width / 2, y: faceCenter.y - ringRadius * 0.46))
            let readout: String
            if playingIndices.count == 1 {
                readout = String(format: "%.1f Hz", notes[playingIndices[0]].frequency)
            } else if playingIndices.count == 2 {
                readout = intervalName(between: playingIndices[0], and: playingIndices[1])
            } else {
                readout = "\(playingIndices.count) NOTES"
            }
            let freqLabel = NSAttributedString(
                string: readout,
                attributes: [.font: monoFont(size: ringRadius * 0.13), .foregroundColor: ink],
            )
            let freqSize = freqLabel.size()
            freqLabel.draw(at: CGPoint(x: faceCenter.x - freqSize.width / 2, y: faceCenter.y - ringRadius * 0.1))
        } else {
            let idle = NSAttributedString(
                string: "\u{2014} Hz",
                attributes: [.font: monoFont(size: ringRadius * 0.13), .foregroundColor: inkSecondary.withAlphaComponent(0.55)],
            )
            let idleSize = idle.size()
            idle.draw(at: CGPoint(x: faceCenter.x - idleSize.width / 2, y: faceCenter.y - ringRadius * 0.18))
        }
    }

    private func drawRange(rect: CGRect, label: String, selected: Bool, context: CGContext, ink: UIColor, inkSecondary: UIColor, hairline: UIColor, lit: UIColor) {
        if selected {
            ink.withAlphaComponent(0.1).setFill()
            context.fill(rect)
            lit.setFill()
            context.fillEllipse(in: CGRect(
                x: rect.minX + rect.height * 0.5, y: rect.midY - rect.height * 0.13,
                width: rect.height * 0.26, height: rect.height * 0.26,
            ))
        }
        let attributes: [NSAttributedString.Key: Any] = [
            .font: condensedFont(size: rect.height * 0.5),
            .foregroundColor: selected ? ink : inkSecondary.withAlphaComponent(0.75),
            .kern: rect.height * 0.14,
        ]
        let text = NSAttributedString(string: label, attributes: attributes)
        let size = text.size()
        text.draw(at: CGPoint(x: rect.midX - size.width / 2, y: rect.midY - size.height / 2))
    }

    private func cellIndex(at point: CGPoint) -> Int {
        for (index, center) in cellCenters.enumerated() {
            let deltaX = point.x - center.x
            let deltaY = point.y - center.y
            if sqrt(deltaX * deltaX + deltaY * deltaY) <= cellRadius * 1.15 { return index }
        }
        return -1
    }

    private func intervalName(between first: Int, and second: Int) -> String {
        let names = [
            "UNISON", "MINOR 2ND", "MAJOR 2ND", "MINOR 3RD",
            "MAJOR 3RD", "PERFECT 4TH", "TRITONE", "PERFECT 5TH",
            "MINOR 6TH", "MAJOR 6TH", "MINOR 7TH", "MAJOR 7TH", "OCTAVE",
        ]
        return names[min(abs(second - first), names.count - 1)]
    }

    private func selectRange(high: Bool) {
        stopAll()
        if high != isHighRange {
            rangeFeedback.selectionChanged()
            rangeFeedback.prepare()
        }
        isHighRange = high
        onRangeChange?(high)
        UIAccessibility.post(notification: .layoutChanged, argument: nil)
    }

    private func releaseTouches(_ touches: Set<UITouch>) {
        for touch in touches {
            let key = ObjectIdentifier(touch)
            guard let cell = activeTouches[key] else { continue }
            activeTouches[key] = nil
            if !toggleMode, cell < notes.count, !activeTouches.values.contains(cell) {
                notes[cell].stop()
            }
        }
        setNeedsDisplay()
    }

    // MARK: - Accessibility

    private func rebuildAccessibilityElements() {
        var elements: [UIAccessibilityElement] = []
        for index in notes.indices where index < cellCenters.count {
            let element = InstrumentAccessibilityElement(accessibilityContainer: self)
            let center = cellCenters[index]
            element.accessibilityFrameInContainerSpace = CGRect(
                x: center.x - cellRadius, y: center.y - cellRadius,
                width: cellRadius * 2, height: cellRadius * 2,
            )
            element.accessibilityLabel = spokenName(at: index)
            element.accessibilityTraits = .button
            element.onActivate = { [weak self] in
                guard let self, index < self.notes.count else { return false }
                let note = self.notes[index]
                if self.toggleMode {
                    if note.isPlaying { note.stop() } else { note.play() }
                } else {
                    note.play()
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                        note.stop()
                        self.setNeedsDisplay()
                    }
                }
                self.setNeedsDisplay()
                return true
            }
            elements.append(element)
        }
        let lowElement = InstrumentAccessibilityElement(accessibilityContainer: self)
        lowElement.accessibilityFrameInContainerSpace = rangeLowRect
        lowElement.accessibilityLabel = "Octave range C to C"
        lowElement.accessibilityTraits = isHighRange ? .button : [.button, .selected]
        lowElement.onActivate = { [weak self] in
            self?.selectRange(high: false)
            return true
        }
        elements.append(lowElement)
        let highElement = InstrumentAccessibilityElement(accessibilityContainer: self)
        highElement.accessibilityFrameInContainerSpace = rangeHighRect
        highElement.accessibilityLabel = "Octave range F to F"
        highElement.accessibilityTraits = isHighRange ? [.button, .selected] : .button
        highElement.onActivate = { [weak self] in
            self?.selectRange(high: true)
            return true
        }
        elements.append(highElement)
        accessibilityElements = elements
    }
}
