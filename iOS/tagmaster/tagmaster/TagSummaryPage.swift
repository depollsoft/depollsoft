//
//  TagSummaryPage.swift
//  tagmaster
//
//  The Summary page: the tag's name and the lists it is on, then its facts,
//  the key note, sheet music, and lyrics and notes (beside them when wide).
//

import Combine
import SwiftUI

/// The page body every detail page shares: scrolls within the safe area, 16 pt in
/// from the edges, limited to a readable width.
struct TMPageScroll<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        ScrollView {
            content
                .frame(maxWidth: TMReadable.width, alignment: .leading)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 16)
                .padding(.vertical, 4)
        }
    }
}

enum TMReadable {
    /// UIKit's readable content width at the default text size.
    static let width: CGFloat = 672
}

struct TMCaption: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(Color(.label))
            .fixedSize(horizontal: false, vertical: true)
    }
}

struct TagSummaryPage: View {
    @Environment(\.tmAccent) private var accent
    let model: TagSummaryModel
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var visible = false

    var body: some View {
        if let tag = model.tag {
            TMPageScroll {
                VStack(alignment: .leading, spacing: 8) {
                    identity(tag)
                    columns(tag)
                }
            }
            // The note is watched only while it sounds on a page someone can see,
            // as the UIKit key button's display link ran only while it was in a window.
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

    static func keyNoteTicks(active: Bool) -> AnyPublisher<Date, Never> {
        active
            ? Timer.publish(every: 1.0 / 30, on: .main, in: .common).autoconnect().eraseToAnyPublisher()
            : Empty(completeImmediately: false).eraseToAnyPublisher()
    }

    private func identity(_ tag: DPTag) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(tag.title ?? "")
                .font(.title)
                .fixedSize(horizontal: false, vertical: true)
            if let aka = tag.alternativeTitle, !aka.isEmpty {
                Text("a.k.a. \(aka)")
                    .font(.title3)
                    .foregroundStyle(Color(.secondaryLabel))
                    .fixedSize(horizontal: false, vertical: true)
            }
            if let version = tag.version, !version.isEmpty {
                Text("Version: \(version)")
                    .font(.body)
                    .foregroundStyle(Color(.secondaryLabel))
                    .fixedSize(horizontal: false, vertical: true)
            }
            TMListChips(model: model)
        }
    }

    private var hasProse: Bool {
        !(model.tag?.lyrics ?? "").isEmpty || !(model.tag?.notes ?? "").isEmpty
    }

    @ViewBuilder
    private func columns(_ tag: DPTag) -> some View {
        ViewThatFits(in: .horizontal) {
            if hasProse {
                HStack(alignment: .top, spacing: 16) {
                    performance(tag).frame(maxWidth: .infinity, alignment: .leading)
                    prose(tag).frame(maxWidth: .infinity, alignment: .leading)
                }
                .frame(minWidth: 560 * dynamicTypeSize.tmBodyPointSize / 17 + 16)
            }
            VStack(alignment: .leading, spacing: 16) {
                performance(tag)
                if hasProse { prose(tag) }
            }
        }
    }

    private func performance(_ tag: DPTag) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            facts(tag)
            if let key = tag.writtenKey, !key.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    TMCaption(text: "Key")
                    TMKeyNoteButton(model: model, title: key)
                        .accessibilityIdentifier("summary.key")
                }
            }
            if tag.sheetMusicUri != nil {
                sheetMusicButton
            }
        }
    }

    private func facts(_ tag: DPTag) -> some View {
        TMFactsLayout(bodyFont: dynamicTypeSize.tmFont(.body)) {
            TMCaption(text: "Tag ID")
            TMFactText(text: "\(tag.tagId)").tmFactValue(.text, minimumHeight: 28)
            TMCaption(text: "Parts")
            TMFactText(text: "\(tag.parts)").tmFactValue(.text, minimumHeight: 28)
            TMCaption(text: "Type")
            TMFactText(text: tag.tagType ?? "").tmFactValue(.text, minimumHeight: 28)
            if tag.classicTagNumber != 0 {
                TMCaption(text: "Classic Tag")
                TMFactText(text: "\(tag.classicTagNumber)").tmFactValue(.text, minimumHeight: 28)
            }
            TMCaption(text: "Rating")
            TMRatingUnit(model: model, rating: tag.rating).tmFactValue(.unit, minimumHeight: 44, gapBefore: 8)
        }
    }

    private var sheetMusicButton: some View {
        Button(action: { model.openSheetMusic() }) {
            HStack(spacing: 8) {
                Image(systemName: "doc.richtext").imageScale(.large)
                Text("Sheet Music")
            }
            .font(.body)
            .foregroundStyle(.white)
            .padding(.vertical, 8)
            .padding(.horizontal, 44)
            .frame(maxWidth: .infinity, minHeight: 44)
            .background(RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(accent.opacity(model.sheetMusicBusy ? 0.5 : 1)))
        }
        .buttonStyle(.plain)
        .disabled(model.sheetMusicBusy)
        .overlay(alignment: .trailing) {
            TMBarberPole.operation("Opening sheet music…", active: model.sheetMusicBusy)
                .padding(.trailing, 12)
        }
    }

    private func prose(_ tag: DPTag) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            if let lyrics = tag.lyrics, !lyrics.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    TMCaption(text: "Lyrics")
                    TMFactText(text: lyrics).accessibilityIdentifier("summary.lyrics")
                }
            }
            if let notes = tag.notes, !notes.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    TMCaption(text: "Notes")
                    TMFactText(text: notes)
                }
            }
        }
    }
}

struct TMFactText: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.body)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Rating

/// The rating number over its bar, then Rate and the slot its progress pole uses.
/// When the two do not fit side by side (accessibility text on a narrow phone),
/// Rate moves under the number rather than either being squeezed.
struct TMRatingUnit: View {
    let model: TagSummaryModel
    let rating: Double

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .center, spacing: 8) {
                value
                action
                Spacer(minLength: 0)
            }
            VStack(alignment: .leading, spacing: 8) {
                value
                action
            }
        }
    }

    private var value: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(String(format: "%1.2f", rating))
                .font(.body)
                .fixedSize()
                .accessibilityLabel("Rating out of 5")
                .accessibilityValue(String(format: "%1.2f", rating))
            TMRatingBar(progress: rating / 5)
        }
        .fixedSize()
        .alignmentGuide(.firstTextBaseline) { $0[.firstTextBaseline] }
    }

    private var action: some View {
        HStack(alignment: .center, spacing: 8) {
            Button(action: model.showRating) {
                HStack(spacing: 8) {
                    Image(systemName: "star").imageScale(.large)
                    Text(model.rated ? "Rated" : "Rate")
                }
                .font(.body)
                .padding(.vertical, 4)
                .padding(.horizontal, 8)
                .frame(minWidth: 44, minHeight: 44)
                .contentShape(Rectangle())
            }
            .buttonStyle(.borderless)
            .disabled(model.rated || model.ratingBusy)
            .accessibilityLabel(model.rated ? "Rating submitted" : "Rate tag")
            .accessibilityIdentifier("summary.rate")
            // UIKit's action sheet with Rate as its source, as the UIKit page presented
            // it: hanging from Rate on every device.
            .background(TMActionSheet(isPresented: Binding(get: { model.ratingDialogPresented },
                                                           set: { model.ratingDialogPresented = $0 }),
                                      actions: model.ratingActions,
                                      title: "Rating",
                                      message: "Rate the tag on a scale of 1-5 stars",
                                      anchoredEverywhere: true))
            TMBarberPole.operation("Sending rating…", active: model.ratingBusy)
        }
        .fixedSize()
    }
}

/// UIProgressView's bar style: a flat 2.5 pt track with the accent fill.
struct TMRatingBar: View {
    @Environment(\.tmAccent) private var accent
    let progress: Double
    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Rectangle().fill(Color(.tertiarySystemFill))
                Rectangle().fill(accent)
                    .frame(width: proxy.size.width * max(0, min(1, progress)))
            }
        }
        .frame(height: 7.0 / 3)
        .accessibilityHidden(true)
    }
}

// MARK: - Key note

/// The written key, outlined in the accent, sounding its note while held. It
/// fills while the note sounds; VoiceOver plays it for a moment instead.
struct TMKeyNoteButton: View {
    @Environment(\.tmAccent) private var accent
    let model: TagSummaryModel
    let title: String
    var singleLine = false
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let playing = model.keyNotePlaying
        let fill = colorScheme == .dark ? accent : Color(TMKeyNoteButton.highContrastAccent)
        let foreground: Color = playing ? (colorScheme == .dark ? .black : .white) : accent
        HStack(spacing: 8) {
            Image(systemName: "key").imageScale(.large)
            Text(title)
                .lineLimit(singleLine ? 1 : nil)
        }
        .font(.body)
        .foregroundStyle(foreground)
        .padding(.vertical, 4)
        .padding(.horizontal, 8)
        .frame(maxWidth: singleLine ? nil : .infinity, minHeight: 44)
        .frame(minWidth: 44)
        .background(RoundedRectangle(cornerRadius: 8).fill(playing ? fill : .clear))
        .overlay(RoundedRectangle(cornerRadius: 8).strokeBorder(accent, lineWidth: 1.5))
        .contentShape(Rectangle())
        // A button, so a scroll that starts on the key scrolls, and a press the
        // system cancels (a call, a system gesture) lets the note go, as the
        // UIKit button's touch-cancel did.
        .overlay {
            Button {} label: { Color.clear.contentShape(Rectangle()) }
                .buttonStyle(TMKeyPressStyle(model: model))
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Play key note \(model.keyNote?.description ?? title)")
        .accessibilityHint("Plays for one and a half seconds")
        .accessibilityAddTraits(.isButton)
        .accessibilityAction { model.playTimedKeyNote() }
    }

    /// Sounds the key for exactly as long as the button is held.
    private struct TMKeyPressStyle: ButtonStyle {
        let model: TagSummaryModel

        func makeBody(configuration: Configuration) -> some View {
            configuration.label
                .onChange(of: configuration.isPressed) { _, pressed in
                    if pressed { model.pressKey() } else { model.releaseKey() }
                }
        }
    }

    /// UIKit's high-contrast accent, for white text on the filled key in light mode.
    static let highContrastAccent: UIColor = {
        let accent = DPAppDelegate.accentColor() ?? .tintColor
        return UIColor { traits in
            let contrast = UITraitCollection(traitsFrom: [traits, UITraitCollection(accessibilityContrast: .high)])
            return accent.resolvedColor(with: contrast)
        }
    }()
}
