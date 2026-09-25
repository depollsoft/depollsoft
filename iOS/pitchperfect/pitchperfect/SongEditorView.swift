//
//  SongEditorView.swift
//  pitchperfect
//
//  The song editor: an engraved title field over a key-signature list. The
//  list is the Keys screen's list, because a singer choosing a key is reading
//  the signature off sheet music: the engraved signature on the left, the
//  key's name on the right, and the chosen row lit like a sounding note.
//  Choosing a key is silent; the pitch pipe and Keys screens are where notes
//  sound.
//

import Combine
import SwiftUI
import UIKit

@Observable
final class SongEditorModel {
    var title: String
    private(set) var selectedKey: DPKey
    var isMinor: Bool {
        didSet { if isMinor != oldValue { modeChanged() } }
    }
    var titleErrorVisible = false
    /// Incremented when the controller wants the title field focused.
    var titleFocusRequest = 0

    let majorKeys = DPKey.majorKeys() as? [DPKey] ?? []
    let minorKeys = DPKey.minorKeys() as? [DPKey] ?? []

    @ObservationIgnored private let rowFeedback = UISelectionFeedbackGenerator()

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

    /// Choosing a key is silent: the editor records the key, it does not play it.
    func tap(_ key: DPKey) {
        guard !key.isEqual(selectedKey) else { return }
        selectedKey = key
        rowFeedback.selectionChanged()
        rowFeedback.prepare()
    }

    /// Keep the same signature when the mode flips: a relative key shares it.
    private func modeChanged() {
        let accidentals = selectedKey.numAccidentals
        if let match = keys.first(where: { $0.numAccidentals == accidentals }) {
            selectedKey = match
        }
    }

    func titleChanged() {
        if titleErrorVisible, !trimmedTitle.isEmpty {
            titleErrorVisible = false
        }
    }

    /// Shows the inline requirement and focuses the field when the title is blank.
    func requireTitle() -> Bool {
        guard trimmedTitle.isEmpty else { return true }
        titleErrorVisible = true
        titleFocusRequest += 1
        return false
    }

}

// MARK: - Views

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
    @Bindable var model: SongEditorModel
    @FocusState private var titleFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
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
                HStack(alignment: .center) {
                    PlateLabel(text: "KEY")
                    Spacer()
                    Picker("Key mode", selection: $model.isMinor) {
                        Text("Major").tag(false)
                        Text("Minor").tag(true)
                    }
                    .pickerStyle(.segmented)
                    .frame(width: 150)
                    .accessibilityIdentifier("keyMode")
                }
                .padding(.top, 18)
                .padding(.bottom, 8)
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .contentShape(Rectangle())
            .onTapGesture { titleFocused = false }

            KeySignatureList(model: model, dismissKeyboard: { titleFocused = false })
        }
        .onChange(of: model.titleFocusRequest) { _, _ in titleFocused = true }
        .animation(.easeOut(duration: 0.15), value: model.titleErrorVisible)
    }
}

/// The Keys screen's list: signature left, name right, hairline rules, and
/// the chosen row lit. Opens scrolled to the chosen key.
private struct KeySignatureList: View {
    @Bindable var model: SongEditorModel
    let dismissKeyboard: () -> Void

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 0) {
                    Rectangle().fill(Plate.hairline).frame(height: 1)
                    ForEach(Array(model.keys.enumerated()), id: \.offset) { index, key in
                        let selected = key.isEqual(model.selectedKey)
                        KeySignatureRow(key: key, minor: model.isMinor, selected: selected) {
                            dismissKeyboard()
                            model.tap(key)
                        }
                        .id(index)
                        Rectangle().fill(Plate.hairline).frame(height: 1)
                    }
                }
            }
            .scrollDismissesKeyboard(.immediately)
            .onAppear { scrollToSelection(proxy, animated: false) }
            // On iPad the form sheet rises for the keyboard (a required title) and
            // the list re-fits; UIKit's kept the chosen key centred.
            .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardDidShowNotification)) { _ in
                guard UIDevice.current.userInterfaceIdiom == .pad else { return }
                scrollToSelection(proxy, animated: false)
            }
            .onChange(of: model.isMinor) { _, _ in scrollToSelection(proxy, animated: true) }
        }
        .accessibilityIdentifier("keyList")
    }

    private func scrollToSelection(_ proxy: ScrollViewProxy, animated: Bool) {
        guard let index = model.keys.firstIndex(where: { $0.isEqual(model.selectedKey) }) else { return }
        if animated {
            withAnimation(.easeOut(duration: 0.2)) { proxy.scrollTo(index, anchor: .center) }
        } else {
            proxy.scrollTo(index, anchor: .center)
        }
    }
}

private struct KeySignatureRow: View {
    let key: DPKey
    let minor: Bool
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(alignment: .center) {
                Text(SongEditorSpeech.signatureGlyphs(numAccidentals: Int(key.numAccidentals)))
                    .font(Plate.music(44))
                    .accessibilityHidden(true)
                Spacer(minLength: 16)
                KeyName(key: key, size: 22, color: selected ? Plate.onLit : Plate.ink)
            }
            .foregroundStyle(selected ? Plate.onLit : Plate.ink)
            .padding(.horizontal, 20)
            .frame(minHeight: 64)
            .background(selected ? Plate.lit : Color.clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(SongEditorSpeech.name(for: key, minor: minor))
        .accessibilityIdentifier("key-\(key.friendlyName() ?? "")\(key.numAccidentals)")
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

/// "F" plus its accidental, in the condensed text face with the NoteHedz glyph.
private struct KeyName: View {
    let key: DPKey
    let size: CGFloat
    let color: Color

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            Text(key.friendlyName())
                .font(Plate.text(size))
            if SongEditorSpeech.accidental(of: key) != Int(Natural.rawValue) {
                Text(SongEditorSpeech.accidental(of: key) == Int(Sharp.rawValue) ? "\u{00EC}" : "\u{00ED}")
                    .font(Plate.noteHedz(size * 1.2))
            }
        }
        .foregroundStyle(color)
        .lineLimit(1)
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
        case 0: return "no sharps or flats"
        case 1: return "1 sharp"
        case -1: return "1 flat"
        case let count where count > 0: return "\(count) sharps"
        case let count: return "\(-count) flats"
        }
    }

    static func accidental(of key: DPKey) -> Int {
        Int(key.note.accidental.get())
    }

    static func name(for key: DPKey, minor: Bool) -> String {
        let accidental = accidental(of: key)
        let letter = (key.note.friendlyName ?? "").uppercased()
        let spelled = accidental == Int(Sharp.rawValue) ? "\(letter) sharp" : (accidental == Int(Flat.rawValue) ? "\(letter) flat" : letter)
        return "\(spelled) \(minor ? "minor" : "major"), \(accidentalCount(numAccidentals: Int(key.numAccidentals)))"
    }
}

// MARK: - Screen

/// One editing of one song: the editor's state and what Close and Done do.
@MainActor
final class SongEditorSession {
    let request: SongEditorRequest
    let model: SongEditorModel

    init(request: SongEditorRequest) {
        self.request = request
        model = SongEditorModel(title: request.song.name ?? "", key: request.song.key)
    }

    var title: String { (request.song.name ?? "").isEmpty ? "Add Song" : "Edit Song" }

    /// Done: a blank title is refused (with the error haptic and announcement);
    /// otherwise the song takes the title and key and the Songs screen closes the editor.
    func complete() {
        guard model.requireTitle() else {
            UINotificationFeedbackGenerator().notificationOccurred(.error)
            UIAccessibility.post(notification: .announcement, argument: "Song title is required")
            return
        }
        request.song.name = model.trimmedTitle
        request.song.key = model.selectedKey
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        request.completion(true)
    }

    func cancel() {
        request.completion(false)
    }
}

/// The editor's content: the form over the staff, the banner docked below on iPhone.
struct SongEditorScreen: View {
    let session: SongEditorSession

    private var isPhone: Bool { UIDevice.current.userInterfaceIdiom == .phone }

    var body: some View {
        VStack(spacing: 0) {
            SongEditorView(model: session.model)
                .padding(.bottom, 8)
            if isPhone {
                BannerAdSlot()
                    .accessibilityHidden(true)
            }
        }
        .staffScreenBackground()
    }
}

/// The editor as the Songs tab presents it, built as the UIKit editor was: its
/// own navigation controller, titled Add Song or Edit Song, with Close and Done
/// bar items. (A SwiftUI NavigationStack in a UIKit-presented controller hands
/// its title and toolbar to the presenting screen's bar instead.)
final class SongEditorController: UIHostingController<SongEditorScreen> {
    let session: SongEditorSession

    init(request: SongEditorRequest) {
        session = SongEditorSession(request: request)
        super.init(rootView: SongEditorScreen(session: session))
        navigationItem.title = session.title
        navigationItem.leftBarButtonItem = BarSymbol.item(systemName: "xmark", target: self, action: #selector(close))
        navigationItem.rightBarButtonItem = BarSymbol.item(systemName: "checkmark", target: self, action: #selector(done))
    }

    @available(*, unavailable)
    @MainActor required dynamic init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    @objc private func close() { session.cancel() }
    @objc private func done() { session.complete() }
}
