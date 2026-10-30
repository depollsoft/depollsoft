//
//  TMMusicStand.swift
//  tagmaster
//
//  An iPhone Duo standing half open on a table (tabletop) is a music stand: a
//  tag's page reads on the upper half, the one that stands and faces the singers,
//  and what they play lies on the lower half under the fingers: the key note and
//  the sheet music, or on the Tracks page the parts. Every other window, and a
//  Duo flat, closed or folded like a book, shows the page whole.
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
                // Standing, the page fades out where it meets the fold rather than
                // slicing a line of text across.
                .mask {
                    VStack(spacing: 0) {
                        Rectangle()
                        LinearGradient(colors: [.black, .clear], startPoint: .top, endPoint: .bottom)
                            .frame(height: fold == nil ? 0 : TMStandDeck.fade)
                    }
                }
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

/// The deck's measures. Lying on the table under the fingers, the lower half is
/// something to play, so the key and the sheet music are pads rather than the
/// page's 44-point buttons, and both decks run the page's own width.
enum TMStandDeck {
    /// The fade at the foot of the standing page, where it meets the fold.
    static let fade: CGFloat = 24
    /// Between the pads, side by side or stacked.
    static let gap: CGFloat = 12
    /// Both pads' corners: the key's outline and Sheet Music's fill match.
    static let corner: CGFloat = 12
    /// Kept clear above and below stacked pads, the Key caption included.
    static let margin: CGFloat = 24

    /// A pad's height on a deck `deck` tall: a good part of the deck when the pads
    /// sit side by side, half what is left when large text stacks them, never
    /// shorter than a comfortable button nor so tall that they read as panels.
    static func padHeight(deck: CGFloat, stacked: Bool) -> CGFloat {
        let share = stacked ? (deck - 2 * margin - gap) / 2 : deck * 0.32
        return min(max(share, 64), 120)
    }
}

/// The deck under Summary, Details and Videos: the key note and the sheet music,
/// the two things a singer reaches for with the tag up on the stand, side by side
/// across the page's width (stacked when large text will not fit them so) and
/// centred down the deck.
struct TMStandKeyDeck: View {
    let model: TagSummaryModel
    @State private var visible = false

    /// Whether the tag has anything for this deck.
    static func has(_ model: TagSummaryModel) -> Bool {
        guard let tag = model.tag else { return false }
        return !(tag.writtenKey ?? "").isEmpty || tag.sheetMusicUri != nil
    }

    var body: some View {
        if let tag = model.tag {
            TMStandDeckScroll { deck in
                ViewThatFits(in: .horizontal) {
                    pads(tag, stacked: false, deck: deck)
                    pads(tag, stacked: true, deck: deck)
                }
            }
            .onReceive(TagSummaryPage.keyNoteTicks(active: visible && model.keyNotePlaying)) { _ in
                model.syncKeyNote()
            }
            .onAppear { visible = true }
            .onDisappear {
                visible = false
                model.stopKeyNote()
            }
        }
    }

    private func pads(_ tag: DPTag, stacked: Bool, deck: CGFloat) -> some View {
        let height = TMStandDeck.padHeight(deck: deck, stacked: stacked)
        let layout = stacked
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: TMStandDeck.gap))
            : AnyLayout(HStackLayout(alignment: .bottom, spacing: TMStandDeck.gap))
        return layout {
            if let key = tag.writtenKey, !key.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    TMCaption(text: "Key")
                    TMKeyNoteButton(model: model, title: key, fillsHeight: true, font: .title3,
                                    cornerRadius: TMStandDeck.corner)
                        .frame(height: height)
                        .accessibilityIdentifier("summary.key")
                }
            }
            if tag.sheetMusicUri != nil {
                TagSummaryPage.sheetMusicButton(model, textStyle: .title3, cornerRadius: TMStandDeck.corner)
                    .frame(maxWidth: .infinity)
                    .frame(height: height)
            }
        }
    }
}

/// The deck under Tracks: one row per part, as the page lists them. The player
/// stays on the page above, with the recording notes.
struct TMStandPartsDeck: View {
    let model: TagTracksModel

    var body: some View {
        TMStandDeckScroll { _ in
            VStack(alignment: .leading, spacing: 0) {
                Text("Tracks")
                    .font(.subheadline)
                    .foregroundStyle(Color(.secondaryLabel))
                    .padding(.leading, 4)
                    .padding(.bottom, 4)
                ForEach(Array(model.tracks.enumerated()), id: \.offset) { index, track in
                    TMTrackRow(model: model, track: track)
                        .padding(.leading, 3)
                    if index < model.tracks.count - 1 { Divider().padding(.leading, 48) }
                }
            }
        }
    }
}

/// A deck's body: in the page's readable column, centred down the deck while it
/// fits and scrolling when it doesn't (large text). Its content learns the deck's height.
private struct TMStandDeckScroll<Content: View>: View {
    @ViewBuilder let content: (CGFloat) -> Content

    var body: some View {
        GeometryReader { proxy in
            ScrollView {
                TMReadableColumn { content(proxy.size.height) }
                    .frame(minHeight: proxy.size.height)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
    }
}
