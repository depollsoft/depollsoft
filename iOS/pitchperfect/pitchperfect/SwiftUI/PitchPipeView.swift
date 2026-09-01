import SwiftUI

struct PitchPipeView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var model: DPPitchPipeModel
    @State private var isFromFToF: Bool
    @State private var toggleNotes = DPSettingsModel.sharedInstance.toggleNotes
    @State private var showSettings = false
    @State private var activeNotes: Set<Int> = []

    private let rowMap = [0, 0, 0, 0, 1, 2, 3, 3, 3, 3, 2, 1]
    private let colMap = [0, 1, 2, 3, 3, 3, 3, 2, 1, 0, 0, 0]

    init() {
        let model = DPPitchPipeModel()
        _model = State(initialValue: model)
        _isFromFToF = State(initialValue: model.isFromFToF)
    }

    private var notes: [DPNote] {
        model.notes.compactMap { $0 as? DPNote }
    }

    var body: some View {
        NavigationView {
            ZStack {
                PitchPerfectBackground()
                GeometryReader { geometry in
                    let side = min(geometry.size.width, geometry.size.height)
                    let cell = side / 4
                    ZStack {
                        ForEach(notes.indices, id: \.self) { index in
                            PitchPipeNoteButton(
                                note: notes[index],
                                isActive: activeNotes.contains(index),
                                toggleNotes: toggleNotes,
                                onToggle: { toggle(index: index, note: notes[index]) },
                                onPress: { notes[index].play() },
                                onRelease: { notes[index].stop() }
                            )
                            .frame(width: cell, height: cell)
                            .position(
                                x: (CGFloat(colMap[index]) + 0.5) * cell,
                                y: (CGFloat(rowMap[index]) + 0.5) * cell
                            )
                        }

                        Picker("Range", selection: $isFromFToF) {
                            Text("C to B").tag(false)
                            Text("F to E").tag(true)
                        }
                        .pickerStyle(.segmented)
                        .padding()
                        .frame(width: cell * 2, height: cell * 2)
                        .position(x: side / 2, y: side / 2)
                        .accessibilityIdentifier("PitchRange")
                    }
                    .frame(width: side, height: side)
                    .position(x: geometry.size.width / 2, y: geometry.size.height / 2)
                }
                .padding()
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
            .onChange(of: isFromFToF) { _, newValue in
                stopAllNotes()
                model.isFromFToF = newValue
            }
            .onReceive(NotificationCenter.default.publisher(for: DPSettingsModel.settingsChangedNotificationName)) { _ in
                stopAllNotes()
                refreshSettings()
            }
            .onChange(of: scenePhase) { _, phase in
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

struct PitchPipeNoteButton: View {
    let note: DPNote
    let isActive: Bool
    let toggleNotes: Bool
    let onToggle: () -> Void
    let onPress: () -> Void
    let onRelease: () -> Void

    @ScaledMetric(relativeTo: .title2) private var glyphSize = 30.0

    var body: some View {
        AudioPressSurface(
            toggleMode: toggleNotes,
            isActive: isActive,
            onToggle: onToggle,
            onPress: onPress,
            onRelease: onRelease
        ) { highlighted in
            Group {
                if note.accidental.get() == Natural.rawValue {
                    Text(note.friendlyName ?? "Unknown")
                        .font(.title2)
                } else {
                    HStack(alignment: .firstTextBaseline, spacing: 3) {
                        Text("ì").font(.custom("NoteHedz", size: glyphSize))
                        Text("/").font(.headline)
                        Text("í").font(.custom("NoteHedz", size: glyphSize))
                    }
                }
            }
            .foregroundColor(highlighted ? .white : .accentColor)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(highlighted ? Color.accentColor : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .padding(2)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityValue(isActive ? "Playing" : "Stopped")
        .accessibilityAddTraits(isActive ? .isSelected : [])
    }

    private var accessibilityLabel: String {
        guard let alternate = note.alternate else {
            return "\(note.friendlyName ?? "Unknown") \(note.octave)"
        }
        return "\(note.friendlyName ?? "Unknown") \(note.octave), \(alternate.friendlyName ?? "Unknown") \(alternate.octave)"
    }
}
