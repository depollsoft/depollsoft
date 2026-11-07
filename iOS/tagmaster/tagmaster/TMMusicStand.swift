//
//  TMMusicStand.swift
//  tagmaster
//
//  An iPhone Duo standing half open on a table (tabletop) is a music stand: the
//  words read on the upper half, the one that stands and faces the singers, and
//  what they play lies on the lower half under the fingers. Summary stands its
//  lyrics over the key, Sheet Music and the tag's facts; Tracks stands its notes
//  and lyrics over the parts and the player. Details and Videos, pages to browse,
//  stay whole. Every other window, and a Duo flat, closed or folded like a book,
//  shows each page as it always has.
//

import SwiftUI

extension EnvironmentValues {
    /// The page stands on the upper half of a tabletop Duo: its controls are on the deck below.
    @Entry var tmOnStand = false
}

extension View {
    /// Stands a tag page on a fold across it: the page above the fold and `deck`
    /// below. With nothing for the deck (`enabled` false) the page stays whole.
    func tmMusicStand<Deck: View>(enabled: Bool, @ViewBuilder deck: @escaping () -> Deck) -> some View {
        modifier(TMMusicStand(enabled: enabled, deck: deck))
    }

    /// A tag page that stays whole on the fold, as a list to browse does. It goes
    /// plain there all the same, as the pages standing beside it do.
    func tmMusicStand() -> some View {
        modifier(TMMusicStand(enabled: false) { EmptyView() })
    }
}

/// The fold a page stands on.
@MainActor
enum TMStandFold {
    /// Tests stand a fold in: given the page's frame in its window, the band in
    /// window coordinates (nil for none).
    static var simulated: ((CGRect) -> CGRect?)?

    /// The page needs this much above the fold to be worth reading, and the deck this much below.
    static let minimumPage: CGFloat = 160
    static let minimumDeck: CGFloat = 200

    /// The active fold across the page `proxy` measures, in its coordinates (the
    /// region's frame holds the crease and the margins iOS asks content to keep
    /// from it), when it leaves room for both parts.
    static func band(in proxy: GeometryProxy) -> CGRect? {
        let band: CGRect?
        if let simulated {
            let frame = proxy.frame(in: .global)
            band = simulated(frame).map { $0.offsetBy(dx: -frame.minX, dy: -frame.minY) }
        } else if #available(iOS 27.1, *) {
            band = proxy.reservedRegions(kind: .division).first(where: \.isActive)?.frame
        } else {
            band = nil
        }
        return band.flatMap { stands($0, in: proxy.size) ? $0 : nil }
    }

    /// Whether `band` runs right across a page of `size`, leaving both parts their room.
    nonisolated static func stands(_ band: CGRect, in size: CGSize) -> Bool {
        band.width >= size.width - 0.5 && band.width > band.height
            && band.minY >= minimumPage && size.height - band.maxY >= minimumDeck
    }
}

private struct TMMusicStand<Deck: View>: ViewModifier {
    let enabled: Bool
    @ViewBuilder let deck: () -> Deck
    @State private var band: CGRect?
    @State private var hingeStatus = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        let fold = enabled ? band : nil
        // The page keeps its place in the hierarchy either way, so standing it up
        // or laying it flat keeps its scroll position and a part that is playing.
        VStack(spacing: 0) {
            content
                .environment(\.tmOnStand, fold != nil)
                .frame(height: fold?.minY)
            if let fold {
                Color.clear
                    .frame(height: fold.height)
                    .accessibilityHidden(true)
                deck()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .transition(.opacity)
            }
        }
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.3), value: fold)
        // Over the fold the barber pole would run from the words, across the crease
        // and under the controls: a tag's pages go plain there, standing or not.
        .background {
            if band != nil { Color(.systemBackground).ignoresSafeArea() }
        }
        .background {
            GeometryReader { proxy in
                let _ = hingeStatus
                Color.clear.preference(key: TMStandBandKey.self, value: TMStandFold.band(in: proxy))
            }
        }
        .onPreferenceChange(TMStandBandKey.self) { band = $0 }
        .hingeStatus($hingeStatus)
    }
}

private struct TMStandBandKey: PreferenceKey {
    static let defaultValue: CGRect? = nil
    static func reduce(value: inout CGRect?, nextValue: () -> CGRect?) { value = value ?? nextValue() }
}

// MARK: - Decks

/// The deck's measures: the page's own controls, a little taller for a hand
/// reaching across a table, starting just below the fold.
enum TMStandDeck {
    /// The key, Sheet Music and each part.
    static let controlHeight: CGFloat = 52
    /// Between the key and Sheet Music, side by side or stacked.
    static let gap: CGFloat = 12
    /// Above the deck's first control, below the fold's own margin.
    static let top: CGFloat = 12
}

/// The deck under Tracks: the parts, the one in the player marked, and once a part
/// is chosen the player itself, held under them where the hand is while the parts
/// scroll.
struct TMStandPartsDeck: View {
    let model: TagTracksModel

    var body: some View {
        VStack(spacing: 0) {
            TMReadableColumn {
                Text("Tracks")
                    .font(.subheadline)
                    .foregroundStyle(Color(.secondaryLabel))
                    .padding(.leading, 4)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.top, TMStandDeck.top)
            .padding(.bottom, 4)
            ScrollViewReader { reader in
                ScrollView {
                    TMReadableColumn {
                        VStack(alignment: .leading, spacing: 0) {
                            ForEach(Array(model.tracks.enumerated()), id: \.offset) { index, track in
                                TMTrackRow(model: model, track: track)
                                    .padding(.leading, 3)
                                    .id(index)
                                // The rules either side of the playing part would cut its wash;
                                // they keep their hairline of space so no row moves.
                                if index < model.tracks.count - 1 {
                                    Divider()
                                        .padding(.leading, 48)
                                        .opacity(current == index || current == index + 1 ? 0 : 1)
                                }
                            }
                        }
                    }
                    .padding(.bottom, 8)
                }
                .scrollBounceBehavior(.basedOnSize)
                // The player coming in shortens the parts: the one it holds stays in
                // view, once the shorter list has been laid out.
                .onChange(of: current) { _, index in
                    guard let index else { return }
                    DispatchQueue.main.async { withAnimation { reader.scrollTo(index) } }
                }
            }
            if model.playerVisible {
                TMReadableColumn { TMTrackPlayer(model: model.player) }
                    .padding(.vertical, 8)
            }
        }
    }

    /// The part in the player, by its place in the list.
    private var current: Int? {
        guard model.playerVisible, let track = model.player.track else { return nil }
        return model.tracks.firstIndex { $0 === track }
    }
}
