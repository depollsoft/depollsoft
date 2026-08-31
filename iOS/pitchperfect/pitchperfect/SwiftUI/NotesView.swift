import SwiftUI

struct PitchPerfectBackground: View {
    private let image = UIImage(named: "panobackground.png")

    var body: some View {
        Color(uiColor: .systemBackground)
            .overlay {
                if let image {
                    Rectangle()
                        .fill(ImagePaint(image: Image(uiImage: image), scale: 1))
                        .opacity(0.5)
                }
            }
            .ignoresSafeArea()
    }
}

struct PitchPerfectBannerAd: UIViewRepresentable {
    func makeUIView(context: Context) -> DPBannerAdView { DPBannerAdView() }
    func updateUIView(_ view: DPBannerAdView, context: Context) {}
}

struct AudioPressSurface<Content: View>: View {
    let toggleMode: Bool
    let isActive: Bool
    let onToggle: () -> Void
    let onPress: () -> Void
    let onRelease: () -> Void
    @ViewBuilder let content: (_ highlighted: Bool) -> Content

    @State private var isPressed = false

    var body: some View {
        content(isActive || isPressed)
            .contentShape(Rectangle())
            .simultaneousGesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        guard !isPressed else { return }
                        isPressed = true
                        if !toggleMode { onPress() }
                    }
                    .onEnded { _ in
                        defer { isPressed = false }
                        if toggleMode {
                            onToggle()
                        } else {
                            onRelease()
                        }
                    }
            )
            .accessibilityAddTraits(.isButton)
            .accessibilityAction { onToggle() }
    }
}

struct NotesView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var showSettings = false
    @State private var activeNotes: Set<Int> = []
    @State private var toggleNotes = DPSettingsModel.sharedInstance.toggleNotes

    private let notes = DPNote.prunedNotes().compactMap { $0 as? DPNote }

    var body: some View {
        NavigationView {
            ZStack {
                PitchPerfectBackground()
                ScrollViewReader { proxy in
                    List(notes.indices, id: \.self) { index in
                        let note = notes[index]
                        NoteRow(
                            note: note,
                            isActive: activeNotes.contains(index),
                            toggleNotes: toggleNotes,
                            onToggle: { toggle(index: index, note: note) },
                            onPress: { note.play() },
                            onRelease: { note.stop() }
                        )
                        .listRowBackground(Color.clear)
                        .id(index)
                    }
                    .listStyle(.plain)
                    .onAppear {
                        UITableView.appearance().backgroundColor = .clear
                        DispatchQueue.main.async {
                            proxy.scrollTo(notes.count / 2, anchor: .center)
                        }
                    }
                }
            }
            .safeAreaInset(edge: .top, spacing: 0) {
                PitchPerfectBannerAd()
                    .frame(height: 50)
                    .accessibilityIdentifier("BannerAd")
            }
            .navigationBarTitle("Pitch Perfect", displayMode: .inline)
            .navigationBarItems(trailing: settingsButton)
            .sheet(isPresented: $showSettings, onDismiss: refreshSettings) {
                SettingsView()
            }
            .onReceive(NotificationCenter.default.publisher(for: DPSettingsModel.settingsChangedNotificationName)) { _ in
                stopAllNotes()
                refreshSettings()
            }
            .onChange(of: scenePhase) { phase in
                if phase != .active { stopAllNotes() }
            }
            .onDisappear(perform: stopAllNotes)
        }
        .navigationViewStyle(.stack)
    }

    private var settingsButton: some View {
        Button {
            stopAllNotes()
            showSettings = true
        } label: {
            Image(systemName: "gearshape")
        }
        .accessibilityLabel("Settings")
        .accessibilityIdentifier("Settings")
    }

    private func toggle(index: Int, note: DPNote) {
        if activeNotes.contains(index) {
            note.stop()
            activeNotes.remove(index)
        } else {
            note.play()
            activeNotes.insert(index)
        }
    }

    private func refreshSettings() {
        toggleNotes = DPSettingsModel.sharedInstance.toggleNotes
    }

    private func stopAllNotes() {
        notes.forEach { $0.stop() }
        activeNotes.removeAll()
    }
}

struct NoteRow: View {
    let note: DPNote
    let isActive: Bool
    let toggleNotes: Bool
    let onToggle: () -> Void
    let onPress: () -> Void
    let onRelease: () -> Void

    var body: some View {
        AudioPressSurface(
            toggleMode: toggleNotes,
            isActive: isActive,
            onToggle: onToggle,
            onPress: onPress,
            onRelease: onRelease
        ) { highlighted in
            HStack {
                NoteDisplay(note: note)
                if let alternate = note.alternate {
                    Text("/")
                    NoteDisplay(note: alternate)
                }
                Spacer()
                Text(note.frequency, format: .number.precision(.fractionLength(2)))
                    .foregroundColor(.secondary)
                Text("Hz")
                    .foregroundColor(.secondary)
            }
            .font(.body)
            .padding(.vertical, 8)
            .frame(minHeight: 44)
            .background(highlighted ? Color.accentColor.opacity(0.18) : Color.clear)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityValue(isActive ? "Playing" : "Stopped")
        .accessibilityAddTraits(isActive ? .isSelected : [])
    }

    private var accessibilityLabel: String {
        var names = "\(note.friendlyName ?? "Unknown") \(note.octave)"
        if let alternate = note.alternate {
            names += ", \(alternate.friendlyName ?? "Unknown") \(alternate.octave)"
        }
        return "\(names), \(String(format: "%.2f", note.frequency)) hertz"
    }
}

struct NoteDisplay: View {
    let note: DPNote
    @ScaledMetric(relativeTo: .body) private var accidentalSize = 24.0

    var body: some View {
        HStack(alignment: .lastTextBaseline, spacing: 2) {
            Text(note.friendlyName ?? "Unknown")
                .font(.body.bold())
            if note.accidental.get() == Sharp.rawValue {
                Text("ì").font(.custom("NoteHedz", size: accidentalSize))
            } else if note.accidental.get() == Flat.rawValue {
                Text("í").font(.custom("NoteHedz", size: accidentalSize))
            }
            Text("\(note.octave)")
                .font(.caption)
        }
        .accessibilityHidden(true)
    }
}
