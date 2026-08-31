import SwiftUI

struct SongEditorView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var songName: String
    @State private var selectedKeyIndex: Int

    let originalSong: DPPitchedSong
    let isNew: Bool
    let onSave: () -> Void

    private let keys: [DPKey]

    init(song: DPPitchedSong, isNew: Bool, onSave: @escaping () -> Void) {
        originalSong = song
        self.isNew = isNew
        self.onSave = onSave
        keys = ((DPKey.majorKeys() ?? []) + (DPKey.minorKeys() ?? [])).compactMap { $0 as? DPKey }
        _songName = State(initialValue: song.name)
        let selected = keys.firstIndex(where: { $0.friendlyName() == song.key?.friendlyName() }) ?? 0
        _selectedKeyIndex = State(initialValue: selected)
    }

    var body: some View {
        NavigationView {
            ZStack {
                PitchPerfectBackground()
                VStack(spacing: 20) {
                    TextField("Song Name", text: $songName)
                        .textFieldStyle(.roundedBorder)
                        .textInputAutocapitalization(.words)
                        .submitLabel(.done)
                        .padding()
                        .accessibilityIdentifier("SongName")

                    if !keys.isEmpty {
                        Picker("Key", selection: $selectedKeyIndex) {
                            ForEach(keys.indices, id: \.self) { index in
                                HStack {
                                    KeySignatureDisplay(numAccidentals: Int(keys[index].numAccidentals))
                                    Text(keys[index].friendlyName() ?? "Unknown")
                                }
                                .tag(index)
                            }
                        }
                        .pickerStyle(.wheel)
                        .accessibilityIdentifier("SongKey")
                    }
                    Spacer()
                }
                .frame(maxWidth: 600)
            }
            .safeAreaInset(edge: .top, spacing: 0) {
                PitchPerfectBannerAd()
                    .frame(height: 50)
                    .accessibilityIdentifier("BannerAd")
            }
            .navigationBarTitle(isNew ? "Add Song" : "Edit Song", displayMode: .inline)
            .navigationBarItems(
                leading: Button("Cancel") { dismiss() },
                trailing: Button("Done", action: save)
                    .disabled(songName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || keys.isEmpty)
            )
        }
        .navigationViewStyle(.stack)
    }

    private func save() {
        originalSong.name = songName.trimmingCharacters(in: .whitespacesAndNewlines)
        originalSong.key = keys[selectedKeyIndex]
        let list = DPSongsModel.sharedInstance.defaultSongList
        if isNew { list.addSong(originalSong) }
        list.storeValue()
        onSave()
        dismiss()
    }
}
