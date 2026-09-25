//
//  InstrumentScreens.swift
//  pitchperfect
//
//  The Pitch Pipe, Notes and Keys tabs. Each is its tab's navigation root:
//  the instrument chrome, a gear that opens Settings, and the docked banner.
//

import SwiftUI
import UIKit

/// The gear every tab root carries; opens Settings as a sheet.
struct SettingsToolbarItem: ToolbarContent {
    @Binding var isPresented: Bool

    var body: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            BarSymbolButton(systemName: "gearshape") { isPresented = true }
        }
    }
}

// MARK: - Pitch Pipe

struct PitchPipeScreen: View {
    let model: PitchPipeModel
    @State private var showingSettings = false

    var body: some View {
        InstrumentPage {
            PitchInstrumentView(model: model)
        }
        .navigationTitle("Pitch Pipe")
        .navigationBarTitleDisplayMode(.inline)
        .instrumentChrome()
        .toolbar { SettingsToolbarItem(isPresented: $showingSettings) }
        .settingsSheet(isPresented: $showingSettings)
        .onAppear { model.refresh() }
        .onDisappear { model.stopAll() }
    }
}

// MARK: - Pressable rows

/// The touch handling the UIKit rows had: a touch that lands on the row begins
/// a press and the press lasts until that finger lifts, wherever it has slid,
/// or until the list's scrolling cancels it. SwiftUI's own press tracking ends
/// when the finger leaves the row and begins again when it returns, which
/// would stop a note early or toggle it twice.
struct TouchPressSurface: UIViewRepresentable {
    let began: () -> Void
    let ended: () -> Void

    final class Surface: UIView {
        var began: () -> Void = {}
        var ended: () -> Void = {}
        private var touching = Set<ObjectIdentifier>()

        override init(frame: CGRect) {
            super.init(frame: frame)
            backgroundColor = .clear
            isMultipleTouchEnabled = false
            isAccessibilityElement = false
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

        override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
            let wasIdle = touching.isEmpty
            touches.forEach { touching.insert(ObjectIdentifier($0)) }
            if wasIdle { began() }
        }

        override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) { finish(touches) }
        override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) { finish(touches) }

        private func finish(_ touches: Set<UITouch>) {
            guard !touching.isEmpty else { return }
            touches.forEach { touching.remove(ObjectIdentifier($0)) }
            if touching.isEmpty { ended() }
        }
    }

    func makeUIView(context: Context) -> Surface { Surface() }

    func updateUIView(_ surface: Surface, context: Context) {
        surface.began = began
        surface.ended = ended
    }
}

extension View {
    /// Presses the row with the UIKit rows' touch semantics (see TouchPressSurface)
    /// and gives VoiceOver the same note as one activation.
    func notePress(began: @escaping () -> Void, ended: @escaping () -> Void,
                   activate: @escaping () -> Void) -> some View {
        overlay(TouchPressSurface(began: began, ended: ended))
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.isButton)
            .accessibilityAction { activate() }
    }

    /// A row that sounds `note` while pressed (or toggles it, with Toggle Notes).
    func notePress(_ note: DPNote, player: NotePlayer = .shared) -> some View {
        notePress(began: { player.pressBegan(note) },
                  ended: { player.pressEnded(note) },
                  activate: { player.activate(note) })
    }
}

/// The plain hairline list every instrument screen uses: transparent rows over
/// the staff, 1 pt rules inset 20 pt, nothing highlighted but what sounds.
///
/// Inside a screen (Notes, Keys, Songs) the list stops at the bars, as the
/// UIKit tables did, and its staff starts at its own top. `fullScreen` is the
/// UITableViewController form (Set Lists, Add songs): the list runs under the
/// bars and its staff starts at the top of the screen.
struct PlateListStyle: ViewModifier {
    var fullScreen = false

    func body(content: Content) -> some View {
        if fullScreen {
            content
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .background { StaffBackground().ignoresSafeArea() }
                .environment(\.defaultMinListRowHeight, 0)
        } else {
            content
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .background(StaffBackground())
                // UIKit's table stopped at the bars; its rows never ran beneath them,
                // and the bars' edge effect belonged to the screen, not the list.
                .clipped()
                .modifier(TopEdgeEffectHidden())
                .environment(\.defaultMinListRowHeight, 0)
        }
    }
}

private struct TopEdgeEffectHidden: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.scrollEdgeEffectHidden(true, for: .top)
        } else {
            content
        }
    }
}

extension View {
    func plateList(fullScreen: Bool = false) -> some View { modifier(PlateListStyle(fullScreen: fullScreen)) }

    /// A transparent row whose rule runs from 20 pt in from the list's edge to
    /// 20 pt from the other. `contentIndent` is how far the list has moved the
    /// row's content in (edit mode's delete control), so the rule stays put.
    /// `trailingOverhang` carries the rule on under the reorder control, which
    /// sits beyond the row's content.
    func plateRow(_ insets: EdgeInsets = EdgeInsets(), contentIndent: CGFloat = 0,
                  trailingOverhang: CGFloat = 0) -> some View {
        listRowInsets(insets)
            .listRowBackground(Color.clear)
            .alignmentGuide(.listRowSeparatorLeading) { _ in 20 - contentIndent }
            .alignmentGuide(.listRowSeparatorTrailing) { dimensions in
                dimensions.width - 20 + trailingOverhang
            }
    }
}

// MARK: - Notes

/// The Notes tab's notes, and whether it has opened on the middle one yet.
@Observable
@MainActor
final class NotesModel {
    let notes: [DPNote] = DPNote.prunedNotes() as? [DPNote] ?? []
    private let player: NotePlayer
    /// The list opens once on its middle note, as the UIKit table did in
    /// viewDidLoad; after that it keeps wherever it was left.
    @ObservationIgnored var hasOpened = false

    init(player: NotePlayer = .shared) {
        self.player = player
    }

    func stopSounding() { player.stop(notes) }
}

struct NotesScreen: View {
    let model: NotesModel
    @State private var showingSettings = false

    var body: some View {
        let notes = model.notes
        InstrumentPage {
            ScrollViewReader { proxy in
                List(notes.indices, id: \.self) { index in
                    NoteRow(note: notes[index])
                        .notePress(notes[index])
                        .plateRow()
                        .id(index)
                }
                .plateList()
                .onAppear {
                    guard !model.hasOpened else { return }
                    model.hasOpened = true
                    proxy.scrollTo(notes.count / 2, anchor: .center)
                }
            }
        }
        .navigationTitle("Notes")
        .navigationBarTitleDisplayMode(.inline)
        .instrumentChrome()
        .toolbar { SettingsToolbarItem(isPresented: $showingSettings) }
        .settingsSheet(isPresented: $showingSettings)
        .onDisappear { model.stopSounding() }
    }
}

/// "C♯₄/D♭₄": each spelling in the condensed face with its NoteHedz accidental and
/// a subscript octave, and the measurement readout at the trailing edge.
///
/// The spellings are laid out as the UIKit row laid out its labels: each sized
/// to fit (rounded up to whole points), set side by side from 10 pt, and each
/// centred on the first 44 pt of the row with integer arithmetic.
struct NoteRow: View {
    let note: DPNote

    private struct Part: Identifiable {
        let id: Int
        let text: Text
        let size: CGSize
    }

    private var parts: [Part] {
        var parts: [(Text, NSAttributedString)] = [NoteSpelling.parts(note)]
        if let alternate = note.alternate {
            let slash = NSAttributedString(string: "/", attributes: [.font: DPTheme.listTitleFont(size: 24)])
            parts.append((Text("/").font(Plate.text(24)).foregroundColor(Plate.inkSecondary), slash))
            parts.append(NoteSpelling.parts(alternate))
        }
        return parts.enumerated().map { index, part in
            let measured = part.1.boundingRect(with: CGSize(width: CGFloat.greatestFiniteMagnitude, height: .greatestFiniteMagnitude),
                                               options: [.usesLineFragmentOrigin], context: nil).size
            return Part(id: index, text: part.0, size: CGSize(width: pixelCeil(measured.width), height: pixelCeil(measured.height)))
        }
    }

    @Environment(\.displayScale) private var displayScale

    /// UILabel's sizeToFit rounds up to whole device pixels.
    private func pixelCeil(_ value: CGFloat) -> CGFloat { ceil(value * displayScale) / displayScale }

    var body: some View {
        let parts = self.parts
        ZStack(alignment: .topLeading) {
            ForEach(parts) { part in
                let left = 10 + parts.prefix(part.id).reduce(0) { $0 + $1.size.width }
                let top = CGFloat(Int(22 - part.size.height / 2))
                part.text
                    .fixedSize()
                    .frame(width: part.size.width, height: part.size.height)
                    .offset(x: left, y: top)
            }
            HStack {
                Spacer(minLength: 0)
                Text(String(format: "%1.2f Hz", note.frequency))
                    .font(Plate.mono(14))
                    .kerning(14 * 0.04)
                    .foregroundStyle(Plate.inkSecondary)
                    .padding(.trailing, 20)
            }
            .frame(height: 52)
            .offset(y: 4.0 / 3.0)
        }
        .frame(maxWidth: .infinity, minHeight: 52, maxHeight: 52, alignment: .topLeading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(NoteSpelling.spoken(note))
        .accessibilityValue(String(format: "%1.2f Hz", note.frequency))
    }
}

enum NoteSpelling {
    /// The spelling as SwiftUI text and as the attributed string UIKit measured.
    static func parts(_ note: DPNote) -> (Text, NSAttributedString) {
        let measured = NSMutableAttributedString(string: note.friendlyName ?? "",
                                                 attributes: [.font: DPTheme.listTitleFont(size: 24)])
        var text = Text(note.friendlyName ?? "").font(Plate.text(24)).foregroundColor(Plate.ink)
        if let glyph = Plate.glyph(for: Int(note.accidental.get())) {
            let font = UIFont(name: "NoteHedz", size: 26) ?? DPTheme.listTitleFont(size: 24)
            measured.append(NSAttributedString(string: glyph, attributes: [.font: font]))
            text = text + Text(glyph).font(Font(font as CTFont)).foregroundColor(Plate.ink)
        }
        measured.append(NSAttributedString(string: "\(note.octave)", attributes: [
            .font: DPTheme.listTitleFont(size: 14), .baselineOffset: -5,
        ]))
        text = text + Text("\(note.octave)").font(Plate.text(14)).foregroundColor(Plate.inkSecondary).baselineOffset(-5)
        return (text, measured)
    }

    static func spoken(_ note: DPNote) -> String {
        func name(_ note: DPNote) -> String {
            let accidental = Int(note.accidental.get())
            let suffix = accidental == Int(Sharp.rawValue) ? " sharp" : (accidental == Int(Flat.rawValue) ? " flat" : "")
            return "\(note.friendlyName ?? "")\(suffix) \(note.octave)"
        }
        guard let alternate = note.alternate else { return name(note) }
        return "\(name(note)), \(name(alternate))"
    }
}

// MARK: - Keys

enum KeyMode: Int, CaseIterable {
    case major, minor

    var keys: [DPKey] {
        (self == .major ? DPKey.majorKeys() : DPKey.minorKeys()) as? [DPKey] ?? []
    }
}

/// Which signatures the Keys tab lists.
@Observable
@MainActor
final class KeysModel {
    private let player: NotePlayer
    /// Switching stops whatever the other mode's rows were sounding.
    var mode = KeyMode.major {
        didSet { if mode != oldValue { player.stop(oldValue.keys.map(\.note)) } }
    }

    /// The list opens once on its middle key; after that it keeps its place.
    @ObservationIgnored var hasOpened = false

    init(player: NotePlayer = .shared) {
        self.player = player
    }

    var keys: [DPKey] { mode.keys }

    func stopSounding() { player.stop(keys.map(\.note)) }
}

struct KeysScreen: View {
    @Bindable var model: KeysModel
    @State private var showingSettings = false

    var body: some View {
        let keys = model.keys
        InstrumentPage {
            ScrollViewReader { proxy in
                List(keys.indices, id: \.self) { index in
                    KeyRow(key: keys[index])
                        .notePress(keys[index].note)
                        .plateRow()
                        .id(index)
                }
                .plateList()
                .onAppear {
                    guard !model.hasOpened else { return }
                    model.hasOpened = true
                    proxy.scrollTo(keys.count / 2, anchor: .center)
                }
            }
        }
        .navigationTitle("Keys")
        .navigationBarTitleDisplayMode(.inline)
        .instrumentChrome()
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Picker("Key mode", selection: $model.mode) {
                    Text("Major").tag(KeyMode.major)
                    Text("Minor").tag(KeyMode.minor)
                }
                .pickerStyle(.segmented)
                .fixedSize()
                .accessibilityIdentifier("keys.mode")
            }
            SettingsToolbarItem(isPresented: $showingSettings)
        }
        .settingsSheet(isPresented: $showingSettings)
        .onDisappear { model.stopSounding() }
    }
}

/// The engraved signature on the left, the key's name on the right.
struct KeyRow: View {
    let key: DPKey

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            Text(SongEditorSpeech.signatureGlyphs(numAccidentals: Int(key.numAccidentals)))
                .font(Plate.music(44))
                .foregroundStyle(Plate.ink)
                .fixedSize()
            Spacer(minLength: 0)
            HStack(alignment: .center, spacing: 0) {
                Text(key.friendlyName() ?? "")
                    .font(Plate.text(22))
                if let glyph = Plate.glyph(for: SongEditorSpeech.accidental(of: key)) {
                    Text(glyph).font(Plate.noteHedz(26))
                }
            }
            .foregroundStyle(Plate.ink)
            .fixedSize()
        }
        .padding(.horizontal, 8)
        // The 1 pt a self-sizing UIKit cell adds for its separator.
        .padding(.bottom, 1)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(SongEditorSpeech.name(for: key, minor: Int(key.keyType.get()) == Int(Minor.rawValue)))
    }
}
