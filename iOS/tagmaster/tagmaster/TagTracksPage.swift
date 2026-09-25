//
//  TagTracksPage.swift
//  tagmaster
//
//  The Tracks page: how the tracks were recorded, the inline balance player
//  once a track is chosen, and one row per voice part. Tapping a part plays it.
//

import SwiftUI

struct TagTracksPage: View {
    @Environment(\.tmAccent) private var accent
    let model: TagTracksModel

    var body: some View {
        List {
            Section {
                ForEach(Array(model.tracks.enumerated()), id: \.offset) { _, track in
                    row(track)
                }
            } header: {
                // The recording notes and the player sit above the rows, as the
                // table's header view did, then the section's own "Tracks" title.
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .textCase(nil)
                        .foregroundStyle(Color(.label))
                        .padding(.vertical, 16)
                    Text("Tracks")
                        .padding(.leading, 4)
                        .padding(.top, 28.67)
                        .padding(.bottom, -1.67)
                }
                .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 8, trailing: 16))
            }
        }
        .listStyle(.grouped)
        .scrollContentBackground(.hidden)
        .accessibilityIdentifier("tracks.table")
        .onAppear(perform: model.appeared)
        .onDisappear(perform: model.disappeared)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            if model.tracks.isEmpty {
                TMFactText(text: "Sorry, no tracks could be found for this tag.")
            }
            if let method = model.tag?.recordingMethod {
                Text("Recording Notes")
                    .font(.subheadline)
                    .foregroundStyle(Color(.secondaryLabel))
                TMFactText(text: method)
            }
            if model.playerVisible {
                TMTrackPlayer(model: model.player)
            }
        }
    }

    private func row(_ track: DPTrack) -> some View {
        let loading = model.loadingTrack === track
        return Button { model.select(track) } label: {
            HStack(spacing: 15) {
                Image(systemName: "play.circle")
                    .font(.title2)
                    .foregroundStyle(accent)
                Text(track.title ?? "")
                    .font(.body)
                    .foregroundStyle(Color(.label))
                    .fixedSize(horizontal: false, vertical: true)
                    .alignmentGuide(.listRowSeparatorLeading) { $0[.leading] }
                Spacer(minLength: 0)
                if loading {
                    TMBarberPole(compact: true)
                        .accessibilityHidden(true)
                }
            }
            .frame(minHeight: 52)
            .contentShape(Rectangle())
        }
        .listRowInsets(EdgeInsets(top: 0, leading: 19, bottom: 0, trailing: 20))
        .listRowBackground(Color.clear)
        .accessibilityLabel(loading ? "\(track.title ?? "Track"), loading" : (track.title ?? "Track"))
        .accessibilityHint("Plays the learning track")
    }
}

/// The inline learning-track player: play/pause, stop, a scrub bar with an elapsed
/// counter, and a balance slider that lowers one side to bring a part in or out.
struct TMTrackPlayer: View {
    @Bindable var model: TMTrackPlayerModel
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(model.track?.title ?? "")
                .font(.headline)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityIdentifier("tagmaster.trackPlayer.title")
            let layout = dynamicTypeSize.isAccessibilitySize
                ? AnyLayout(VStackLayout(alignment: .leading, spacing: 8))
                : AnyLayout(HStackLayout(alignment: .center, spacing: 8))
            layout {
                HStack(spacing: 4) {
                    transportButton(model.isPlaying ? "pause.fill" : "play.fill",
                                    label: model.isPlaying ? "Pause" : "Play",
                                    identifier: "tagmaster.trackPlayer.playPause",
                                    action: model.togglePlayPause)
                        .disabled(!model.isLoaded)
                    transportButton("stop.fill", label: "Stop", identifier: "tagmaster.trackPlayer.stop",
                                    action: model.stop)
                        .disabled(!model.canStop)
                }
                VStack(spacing: 4) {
                    HStack(spacing: 8) {
                        Slider(value: Binding(get: { model.position }, set: { model.seek(to: $0) }),
                               in: 0...max(model.duration, 0.001)) { editing in
                            model.scrubbing = editing
                            if !editing { model.refresh() }
                        }
                        .disabled(!model.isLoaded)
                        .accessibilityLabel("Position")
                        .accessibilityValue(model.counterText)
                        .accessibilityIdentifier("tagmaster.trackPlayer.position")
                        Text(model.counterText)
                            .font(.caption.monospacedDigit())
                            .foregroundStyle(Color(.secondaryLabel))
                            .frame(minWidth: 72, alignment: .trailing)
                            .fixedSize()
                            .accessibilityIdentifier("tagmaster.trackPlayer.counter")
                    }
                    HStack(spacing: 8) {
                        Slider(value: Binding(get: { Double(model.balance) }, set: { model.setBalance(Float($0)) }),
                               in: 0...1) {
                            Text("Balance")
                        } minimumValueLabel: {
                            Image(systemName: "l.circle")
                        } maximumValueLabel: {
                            Image(systemName: "r.circle")
                        }
                        .disabled(!model.isLoaded)
                        .accessibilityLabel("Balance")
                        .accessibilityValue(model.balanceDescription)
                        .accessibilityHint("Lowers one side to bring a part in or out.")
                        .accessibilityAction(named: "Center balance", model.centerBalance)
                        .accessibilityIdentifier("tagmaster.trackPlayer.balance")
                        .modifier(TMTwoFingerDoubleTap(action: model.centerBalance))
                        Text("Balance")
                            .font(.caption)
                            .foregroundStyle(Color(.secondaryLabel))
                            .frame(minWidth: 72, alignment: .trailing)
                            .fixedSize()
                            .accessibilityHidden(true)
                    }
                }
            }
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(Color(.secondarySystemBackground)))
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("tagmaster.trackPlayer")
    }

    private func transportButton(_ symbol: String, label: String, identifier: String,
                                 action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.title2)
                .frame(minWidth: 44, minHeight: 44)
        }
        .buttonStyle(.borderless)
        .accessibilityLabel(label)
        .accessibilityIdentifier(identifier)
    }
}

/// A two-finger double tap centres the balance, as it always has.
private struct TMTwoFingerDoubleTap: ViewModifier {
    let action: () -> Void

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *) {
            content.gesture(TMTwoFingerDoubleTapGesture(action: action))
        } else {
            content
        }
    }
}

@available(iOS 18.0, *)
private struct TMTwoFingerDoubleTapGesture: UIGestureRecognizerRepresentable {
    let action: () -> Void

    func makeUIGestureRecognizer(context: Context) -> UITapGestureRecognizer {
        let recognizer = UITapGestureRecognizer()
        recognizer.numberOfTapsRequired = 2
        recognizer.numberOfTouchesRequired = 2
        return recognizer
    }

    func handleUIGestureRecognizerAction(_ recognizer: UITapGestureRecognizer, context: Context) {
        if recognizer.state == .ended { action() }
    }
}
