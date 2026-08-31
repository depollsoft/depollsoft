import SwiftUI

struct KeysView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var showSettings = false
    @State private var activeKeys: Set<Int> = []
    @State private var toggleNotes = DPSettingsModel.sharedInstance.toggleNotes
    @State private var keyKind = 0

    private var keys: [DPKey] {
        let source = keyKind == 0 ? DPKey.majorKeys() : DPKey.minorKeys()
        return (source ?? []).compactMap { $0 as? DPKey }
    }

    var body: some View {
        NavigationView {
            ZStack {
                PitchPerfectBackground()
                VStack(spacing: 0) {
                    Picker("Key type", selection: $keyKind) {
                        Text("Major").tag(0)
                        Text("Minor").tag(1)
                    }
                    .pickerStyle(.segmented)
                    .padding()
                    .accessibilityIdentifier("KeyType")

                    ScrollViewReader { proxy in
                        List(keys.indices, id: \.self) { index in
                            let key = keys[index]
                            KeyRow(
                                key: key,
                                keyTypeName: keyKind == 0 ? "major" : "minor",
                                isActive: activeKeys.contains(index),
                                toggleNotes: toggleNotes,
                                onToggle: { toggle(index: index, key: key) },
                                onPress: { key.note.play() },
                                onRelease: { key.note.stop() }
                            )
                            .listRowBackground(Color.clear)
                            .id(index)
                        }
                        .listStyle(.plain)
                        .onAppear {
                            UITableView.appearance().backgroundColor = .clear
                            DispatchQueue.main.async {
                                proxy.scrollTo(keys.count / 2, anchor: .center)
                            }
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
            .onChange(of: keyKind) { _ in stopAllNotes() }
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

    private func toggle(index: Int, key: DPKey) {
        if activeKeys.contains(index) {
            key.note.stop()
            activeKeys.remove(index)
        } else {
            key.note.play()
            activeKeys.insert(index)
        }
    }

    private func refreshSettings() {
        toggleNotes = DPSettingsModel.sharedInstance.toggleNotes
    }

    private func stopAllNotes() {
        keys.forEach { $0.note.stop() }
        activeKeys.removeAll()
    }
}

struct KeyRow: View {
    let key: DPKey
    let keyTypeName: String
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
            HStack(spacing: 16) {
                KeySignatureDisplay(numAccidentals: Int(key.numAccidentals))
                Spacer()
                Text(key.friendlyName() ?? "Unknown")
                    .font(.body.bold())
            }
            .padding(.vertical, 8)
            .frame(minHeight: 44)
            .background(highlighted ? Color.accentColor.opacity(0.18) : Color.clear)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(key.friendlyName() ?? "Unknown") \(keyTypeName), \(accidentalDescription)")
        .accessibilityValue(isActive ? "Playing" : "Stopped")
        .accessibilityAddTraits(isActive ? .isSelected : [])
    }

    private var accidentalDescription: String {
        let count = abs(Int(key.numAccidentals))
        guard count > 0 else { return "no sharps or flats" }
        let name = key.numAccidentals > 0 ? "sharp" : "flat"
        return "\(count) \(name)\(count == 1 ? "" : "s")"
    }
}

struct KeySignatureDisplay: View {
    let numAccidentals: Int
    @ScaledMetric(relativeTo: .title2) private var glyphSize = 30.0

    private let flats = ["", "\u{00A8}", "\u{00A9}", "\u{00AA}", "\u{00AB}", "\u{00AC}", "\u{20AC}", "\u{00AE}"]
    private let sharps = ["", "\u{00A1}", "\u{00A2}", "\u{00A3}", "\u{00A4}", "\u{00A5}", "\u{00A6}", "\u{00A7}"]

    var body: some View {
        Text(signatureText)
            .font(.custom("MusiQwik", size: glyphSize))
            .accessibilityHidden(true)
    }

    var signatureText: String {
        var result = "&"
        if numAccidentals > 0, numAccidentals < sharps.count {
            result += sharps[numAccidentals]
        } else if numAccidentals < 0, -numAccidentals < flats.count {
            result += flats[-numAccidentals]
        }
        return result
    }
}
