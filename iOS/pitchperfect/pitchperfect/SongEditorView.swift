//
//  SongEditorView.swift
//  pitchperfect
//
//  The song editor: an engraved title field over the key dial. Thirteen key
//  cells sit around the instrument ring in circle-of-fifths order, C at
//  twelve o'clock, sharps clockwise, flats counter-clockwise, the enharmonic
//  pair meeting at six. The chosen key's signature is engraved in the hole
//  beside a Major/Minor selector, the machined part the pitch pipe uses for
//  its octave range.
//

import Combine
import SwiftUI
import UIKit

final class SongEditorModel: ObservableObject {
    @Published var title: String
    @Published private(set) var selectedKey: DPKey
    @Published private(set) var isMinor: Bool
    @Published var titleErrorVisible = false
    /// Incremented when the controller wants the title field focused.
    @Published var titleFocusRequest = 0

    let majorKeys = DPKey.majorKeys() as? [DPKey] ?? []
    let minorKeys = DPKey.minorKeys() as? [DPKey] ?? []

    private var previewNote: DPNote?
    private var previewStop: DispatchWorkItem?
    private let cellFeedback = UIImpactFeedbackGenerator(style: .rigid)
    private let modeFeedback = UISelectionFeedbackGenerator()

    init(title: String, key: DPKey?) {
        self.title = title
        let majors = DPKey.majorKeys() as? [DPKey] ?? []
        let minors = DPKey.minorKeys() as? [DPKey] ?? []
        let initial = key ?? majors[majors.count / 2]
        selectedKey = initial
        isMinor = minors.contains { $0.isEqual(initial) }
    }

    var keys: [DPKey] { isMinor ? minorKeys : majorKeys }

    var trimmedTitle: String {
        title.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// Selecting from code never sounds the tonic; only a tap does.
    func select(_ key: DPKey) {
        selectedKey = key
        isMinor = minorKeys.contains { $0.isEqual(key) }
    }

    func tap(_ key: DPKey) {
        selectedKey = key
        cellFeedback.impactOccurred(intensity: 0.55)
        cellFeedback.prepare()
        preview(key.note)
    }

    func setMode(minor: Bool) {
        guard minor != isMinor else { return }
        modeFeedback.selectionChanged()
        modeFeedback.prepare()
        // Keep the same signature when the mode flips: a relative key shares it.
        let accidentals = selectedKey.numAccidentals
        isMinor = minor
        if let match = keys.first(where: { $0.numAccidentals == accidentals }) {
            selectedKey = match
        }
    }

    func titleChanged() {
        if titleErrorVisible, !trimmedTitle.isEmpty {
            titleErrorVisible = false
        }
    }

    /// Sound the tonic briefly so a singer can confirm the key by ear.
    private func preview(_ note: DPNote) {
        stopPreview()
        note.play()
        previewNote = note
        let stop = DispatchWorkItem { [weak self] in
            note.stop()
            if self?.previewNote === note { self?.previewNote = nil }
        }
        previewStop = stop
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.7, execute: stop)
    }

    func stopPreview() {
        previewStop?.cancel()
        previewStop = nil
        previewNote?.stop()
        previewNote = nil
    }
}

/// The Objective-C face of the editor: owns the model and builds the hosted view.
@objc public final class DPSongEditor: NSObject {
    let model: SongEditorModel

    @objc public init(title: String, key: DPKey?) {
        model = SongEditorModel(title: title, key: key)
    }

    @objc public var title: String {
        get { model.trimmedTitle }
        set { model.title = newValue }
    }
    @objc public var selectedKey: DPKey { model.selectedKey }
    @objc public var isMinor: Bool { model.isMinor }
    @objc public var titleErrorVisible: Bool { model.titleErrorVisible }

    @objc public func select(_ key: DPKey) { model.select(key) }

    /// Shows the inline requirement and focuses the field when the title is blank.
    @objc public func requireTitle() -> Bool {
        guard model.trimmedTitle.isEmpty else { return true }
        model.titleErrorVisible = true
        model.titleFocusRequest += 1
        return false
    }

    @objc public func focusTitle() { model.titleFocusRequest += 1 }
    @objc public func stopPreview() { model.stopPreview() }

    @objc public func makeViewController() -> UIViewController {
        let host = UIHostingController(rootView: SongEditorView(model: model))
        host.view.backgroundColor = .clear
        return host
    }
}

// MARK: - Views

private enum Plate {
    static let ink = Color(DPTheme.plateInk)
    static let inkSecondary = Color(DPTheme.plateInkSecondary)
    static let surface = Color(DPTheme.plateSurface)
    static let hairline = Color(DPTheme.plateHairline)
    static let lit = Color(DPTheme.plateLit)
    static let onLit = Color(DPTheme.plateOnLit)

    static func display(_ size: CGFloat) -> Font { Font(DPTheme.condensedFont(size: size) as CTFont) }
    static func text(_ size: CGFloat) -> Font { Font(DPTheme.listTitleFont(size: size) as CTFont) }
    static func mono(_ size: CGFloat) -> Font { Font(DPTheme.monospacedFont(size: size) as CTFont) }
    static func music(_ size: CGFloat) -> Font { Font.custom("MusiQwik", size: size) }
    static func noteHedz(_ size: CGFloat) -> Font { Font.custom("NoteHedz", size: size) }
}

/// Engraved plate label: tracked monospaced capitals in secondary ink.
private struct PlateLabel: View {
    let text: String

    var body: some View {
        Text(text)
            .font(Plate.mono(12))
            .tracking(12 * 0.14)
            .foregroundStyle(Plate.inkSecondary)
            .accessibilityAddTraits(.isHeader)
    }
}

struct SongEditorView: View {
    @ObservedObject var model: SongEditorModel
    @FocusState private var titleFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            PlateLabel(text: "SONG TITLE")
            TextField("", text: $model.title, prompt: Text("Untitled").foregroundStyle(Plate.inkSecondary.opacity(0.6)))
                .font(Plate.text(26))
                .foregroundStyle(Plate.ink)
                .tint(Plate.ink)
                .textInputAutocapitalization(.words)
                .submitLabel(.done)
                .focused($titleFocused)
                .onSubmit { titleFocused = false }
                .onChange(of: model.title) { _, _ in model.titleChanged() }
                .accessibilityLabel("Song title")
                .accessibilityIdentifier("songTitleField")
                .padding(.top, 6)
                .padding(.bottom, 8)
                .frame(minHeight: 44)
            Rectangle()
                .fill(titleFocused || model.titleErrorVisible ? Plate.ink : Plate.hairline)
                .frame(height: titleFocused || model.titleErrorVisible ? 2 : 1)
            if model.titleErrorVisible {
                Text("Song title is required")
                    .font(Plate.mono(12))
                    .foregroundStyle(Plate.ink)
                    .padding(.top, 6)
                    .accessibilityIdentifier("songTitleError")
            }
            PlateLabel(text: "KEY")
                .padding(.top, 22)
            KeyDial(model: model)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .contentShape(Rectangle())
        .onTapGesture { titleFocused = false }
        .onChange(of: model.titleFocusRequest) { _, _ in titleFocused = true }
        .animation(.easeOut(duration: 0.15), value: model.titleErrorVisible)
    }
}

private struct KeyDial: View {
    @ObservedObject var model: SongEditorModel

    var body: some View {
        GeometryReader { geometry in
            let half = min(geometry.size.width, geometry.size.height) / 2
            let ring = half * 0.80
            let cell = ring * 0.45
            let keys = model.keys
            let selected = keys.firstIndex { $0.isEqual(model.selectedKey) }

            ZStack {
                // Bloom beneath the chosen key, as under a sounding pitch.
                RadialLayout(radius: ring, cellDiameter: cell * 2.4, origin: .middleAtTop) {
                    ForEach(Array(keys.indices), id: \.self) { index in
                        Circle()
                            .fill(RadialGradient(colors: [Plate.lit.opacity(0.5), Plate.lit.opacity(0)], center: .center, startRadius: 0, endRadius: cell * 1.2))
                            .opacity(index == selected ? 1 : 0)
                    }
                }
                .allowsHitTesting(false)
                .accessibilityHidden(true)

                RadialLayout(radius: ring, cellDiameter: cell, origin: .middleAtTop) {
                    ForEach(Array(keys.indices), id: \.self) { index in
                        KeyCell(key: keys[index], minor: model.isMinor, active: index == selected, diameter: cell) {
                            model.tap(keys[index])
                        }
                    }
                }

                KeyReadout(model: model, ring: ring)
            }
            .frame(width: geometry.size.width, height: geometry.size.height, alignment: .top)
            .accessibilityElement(children: .contain)
            .accessibilityIdentifier("keyDial")
        }
    }
}

/// "F" plus its accidental, in the engraved display face.
private struct KeyName: View {
    let key: DPKey
    let size: CGFloat
    let color: Color

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            Text(key.friendlyName())
                .font(Plate.display(size))
            if SongEditorSpeech.accidental(of: key) != Int(Natural.rawValue) {
                Text(SongEditorSpeech.accidental(of: key) == Int(Sharp.rawValue) ? "\u{00EC}" : "\u{00ED}")
                    .font(Plate.noteHedz(size * 0.9))
            }
        }
        .foregroundStyle(color)
        .lineLimit(1)
    }
}

private struct KeyCell: View {
    let key: DPKey
    let minor: Bool
    let active: Bool
    let diameter: CGFloat
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle().fill(active ? Plate.lit : Plate.surface)
                Circle().stroke(active ? Plate.lit : Plate.hairline, lineWidth: active ? 2.5 : 1.2)
                Circle()
                    .stroke(active ? Plate.onLit.opacity(0.45) : Plate.inkSecondary.opacity(0.3), lineWidth: 0.8)
                    .padding(diameter * 0.07)
                KeyName(key: key, size: diameter * 0.41, color: active ? Plate.onLit : Plate.ink)
            }
            .frame(width: diameter, height: diameter)
            .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(SongEditorSpeech.name(for: key, minor: minor))
        .accessibilityIdentifier("key-\(key.friendlyName() ?? "")\(key.numAccidentals)")
        .accessibilityAddTraits(active ? .isSelected : [])
    }
}

private struct KeyReadout: View {
    @ObservedObject var model: SongEditorModel
    let ring: CGFloat

    var body: some View {
        let key = model.selectedKey
        // Everything here must fit the ring's hole, so sizes follow the ring.
        VStack(spacing: 0) {
            Text(SongEditorSpeech.signatureGlyphs(numAccidentals: Int(key.numAccidentals)))
                .font(Plate.music(ring * 0.34))
                .foregroundStyle(Plate.ink)
                .accessibilityHidden(true)
            HStack(alignment: .firstTextBaseline, spacing: ring * 0.04) {
                KeyName(key: key, size: ring * 0.14, color: Plate.ink)
                Text(model.isMinor ? "MINOR" : "MAJOR")
                    .font(Plate.display(ring * 0.11))
                    .tracking(ring * 0.015)
                    .foregroundStyle(Plate.ink)
            }
            Text(SongEditorSpeech.accidentalCount(numAccidentals: Int(key.numAccidentals)))
                .font(Plate.mono(ring * 0.075))
                .tracking(ring * 0.011)
                .foregroundStyle(Plate.inkSecondary)
                .padding(.top, ring * 0.03)
            ModeSelector(model: model)
                .frame(width: ring * 0.72, height: 72)
                .padding(.top, ring * 0.06)
        }
        .frame(maxHeight: (ring * 0.775) * 2)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(SongEditorSpeech.name(for: key, minor: model.isMinor))
    }
}

/// One machined frame containing both mode positions, as the pitch pipe's
/// octave-range selector does.
private struct ModeSelector: View {
    @ObservedObject var model: SongEditorModel

    var body: some View {
        VStack(spacing: 0) {
            ModeSegment(label: "MAJOR", selected: !model.isMinor, identifier: "keyMode-major") { model.setMode(minor: false) }
            Rectangle().fill(Plate.hairline.opacity(0.6)).frame(height: 1)
            ModeSegment(label: "MINOR", selected: model.isMinor, identifier: "keyMode-minor") { model.setMode(minor: true) }
        }
        .background(Plate.surface.opacity(0.92))
        .overlay(RoundedRectangle(cornerRadius: 4).stroke(Plate.hairline, lineWidth: 1.2))
        .clipShape(RoundedRectangle(cornerRadius: 4))
    }
}

private struct ModeSegment: View {
    let label: String
    let selected: Bool
    let identifier: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Circle()
                    .fill(selected ? Plate.lit : .clear)
                    .frame(width: 5, height: 5)
                Text(label)
                    .font(Plate.display(12))
                    .tracking(1.6)
                    .foregroundStyle(selected ? Plate.ink : Plate.inkSecondary.opacity(0.75))
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(selected ? Plate.ink.opacity(0.10) : .clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label == "MAJOR" ? "Major keys" : "Minor keys")
        .accessibilityIdentifier(identifier)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

enum SongEditorSpeech {
    /// MusiQwik encodes a treble clef as "&" followed by one glyph per signature.
    static func signatureGlyphs(numAccidentals: Int) -> String {
        let sharps = ["", "\u{00A1}", "\u{00A2}", "\u{00A3}", "\u{00A4}", "\u{00A5}", "\u{00A6}", "\u{00A7}"]
        let flats = ["", "\u{00A8}", "\u{00A9}", "\u{00AA}", "\u{00AB}", "\u{00AC}", "\u{20AC}", "\u{00AE}"]
        let count = min(abs(numAccidentals), 7)
        return "&" + (numAccidentals >= 0 ? sharps[count] : flats[count])
    }

    static func accidentalCount(numAccidentals: Int) -> String {
        switch numAccidentals {
        case 0: return "NO SHARPS OR FLATS"
        case 1: return "1 SHARP"
        case -1: return "1 FLAT"
        case let n where n > 0: return "\(n) SHARPS"
        case let n: return "\(-n) FLATS"
        }
    }

    static func accidental(of key: DPKey) -> Int {
        Int(key.note.accidental.get())
    }

    static func name(for key: DPKey, minor: Bool) -> String {
        let accidental = accidental(of: key)
        let letter = (key.note.friendlyName ?? "").uppercased()
        let spelled = accidental == Int(Sharp.rawValue) ? "\(letter) sharp" : (accidental == Int(Flat.rawValue) ? "\(letter) flat" : letter)
        return "\(spelled) \(minor ? "minor" : "major"), \(accidentalCount(numAccidentals: Int(key.numAccidentals)).lowercased())"
    }
}
