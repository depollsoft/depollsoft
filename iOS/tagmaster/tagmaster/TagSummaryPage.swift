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
    /// The readable width UIKit's guide gives the page right now.
    @State private var readable = TMReadable.width

    var body: some View {
        ScrollView {
            content
                .frame(maxWidth: readable, alignment: .leading)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 16)
                .padding(.vertical, 4)
        }
        .background(TMReadableProbe(width: $readable).accessibilityHidden(true))
    }
}

enum TMReadable {
    /// UIKit's readable content width at the default text size, until measured.
    static let width: CGFloat = 672
}

/// The UIKit page's container: a view with 16-point side margins whose
/// `readableContentGuide` set the content's width. It reports that width, which
/// follows the text size, the size class and the page's own width exactly.
struct TMReadableProbe: UIViewRepresentable {
    @Binding var width: CGFloat

    final class Probe: UIView {
        var report: (CGFloat) -> Void = { _ in }

        override init(frame: CGRect) {
            super.init(frame: frame)
            directionalLayoutMargins = NSDirectionalEdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16)
            isUserInteractionEnabled = false
            isAccessibilityElement = false
        }

        required init?(coder: NSCoder) { fatalError("TMReadableProbe is created in code") }

        override func layoutSubviews() {
            super.layoutSubviews()
            let measured = readableContentGuide.layoutFrame.width
            if measured > 0 { report(measured) }
        }

        override func traitCollectionDidChange(_ previous: UITraitCollection?) {
            super.traitCollectionDidChange(previous)
            setNeedsLayout()
        }
    }

    func makeUIView(context: Context) -> Probe { Probe() }

    func updateUIView(_ probe: Probe, context: Context) {
        let binding = $width
        probe.report = { measured in
            DispatchQueue.main.async { if abs(binding.wrappedValue - measured) > 0.5 { binding.wrappedValue = measured } }
        }
    }
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
        // UIKit held Sheet Music and the key to one height, the taller of the two.
        TMMatchedHeightStack {
            facts(tag)
            if let key = tag.writtenKey, !key.isEmpty {
                TMCaption(text: "Key").layoutValue(key: TMGapBefore.self, value: 16)
                TMKeyNoteButton(model: model, title: key, fillsHeight: true)
                    .accessibilityIdentifier("summary.key")
                    .layoutValue(key: TMGapBefore.self, value: 4)
                    .layoutValue(key: TMMatchesHeight.self, value: true)
            }
            if tag.sheetMusicUri != nil {
                sheetMusicButton
                    .layoutValue(key: TMGapBefore.self, value: 16)
                    .layoutValue(key: TMMatchesHeight.self, value: true)
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
        TMSheetMusicButton(busy: model.sheetMusicBusy) { model.openSheetMusic() }
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
    @Environment(\.tmAccent) private var accent
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
            .tint(accent)
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
    /// Takes any taller height it is offered (Sheet Music's), outline and all, as
    /// the UIKit key did under its equal-height constraint.
    var fillsHeight = false
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
        .frame(maxWidth: singleLine ? nil : .infinity, minHeight: 44, maxHeight: fillsHeight ? .infinity : nil)
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
        let accent = DPAppDelegate.accentColor
        return UIColor { traits in
            let contrast = UITraitCollection(traitsFrom: [traits, UITraitCollection(accessibilityContrast: .high)])
            return accent.resolvedColor(with: contrast)
        }
    }()
}

/// Sheet Music as UIKit drew it: a filled button (white title and icon on the
/// accent, medium corners, 8/44 insets, wrapping title) that greys out exactly as
/// UIKit's does while something is presented over the page.
struct TMSheetMusicButton: UIViewRepresentable {
    let busy: Bool
    let action: () -> Void

    func makeUIView(context: Context) -> UIButton {
        var configuration = UIButton.Configuration.filled()
        configuration.baseForegroundColor = .white
        configuration.cornerStyle = .medium
        configuration.image = UIImage(systemName: "doc.richtext")
        configuration.imagePadding = 8
        configuration.titleLineBreakMode = .byWordWrapping
        configuration.contentInsets = NSDirectionalEdgeInsets(top: 8, leading: 44, bottom: 8, trailing: 44)
        configuration.titleTextAttributesTransformer = UIConfigurationTextAttributesTransformer { attributes in
            var attributes = attributes
            attributes.font = UIFont.preferredFont(forTextStyle: .body)
            return attributes
        }
        let button = UIButton(configuration: configuration)
        button.setTitle("Sheet Music", for: .normal)
        button.titleLabel?.adjustsFontForContentSizeCategory = true
        button.tintColor = DPAppDelegate.accentColor
        button.addAction(UIAction { _ in context.coordinator.action() }, for: .touchUpInside)
        button.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        return button
    }

    func updateUIView(_ button: UIButton, context: Context) {
        context.coordinator.action = action
        button.isEnabled = !busy
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView button: UIButton, context: Context) -> CGSize? {
        // Asked for its ideal size (no width proposed), the button gives its natural
        // width, as a UIButton's intrinsic size would; never an unbounded one.
        guard let width = proposal.width, width.isFinite else {
            let natural = button.intrinsicContentSize
            return CGSize(width: natural.width, height: max(44, natural.height))
        }
        let fitted = button.systemLayoutSizeFitting(CGSize(width: width, height: UIView.layoutFittingCompressedSize.height),
                                                    withHorizontalFittingPriority: .required,
                                                    verticalFittingPriority: .fittingSizeLevel)
        let natural = max(44, fitted.height)
        // Given a taller height (the key's, in TMMatchedHeightStack), the button takes
        // it and centres its content, as the UIKit button did under an equal-height constraint.
        guard let height = proposal.height, height.isFinite else { return CGSize(width: width, height: natural) }
        return CGSize(width: width, height: max(natural, height))
    }

    func makeCoordinator() -> Coordinator { Coordinator(action: action) }

    final class Coordinator {
        var action: () -> Void
        init(action: @escaping () -> Void) { self.action = action }
    }
}

/// The gap above a view in a `TMMatchedHeightStack`.
struct TMGapBefore: LayoutValueKey {
    static let defaultValue: CGFloat = 0
}

/// Views in a `TMMatchedHeightStack` marked with this share the tallest one's height.
struct TMMatchesHeight: LayoutValueKey {
    static let defaultValue = false
}

/// A leading-aligned column, each view as wide as the column, whose marked views
/// all take the tallest one's natural height. The heights are measured unconstrained
/// in the same pass that places them, so the shared height shrinks as well as grows
/// when the text size or width changes.
struct TMMatchedHeightStack: Layout {
    /// The heights last measured, and the width they were measured at.
    struct Cache {
        var width: CGFloat?
        var heights: [CGFloat] = []
    }

    func makeCache(subviews: Subviews) -> Cache { Cache() }

    func updateCache(_ cache: inout Cache, subviews: Subviews) { cache = Cache() }

    private func heights(_ width: CGFloat, _ subviews: Subviews, _ cache: inout Cache) -> [CGFloat] {
        if cache.width == width, cache.heights.count == subviews.count { return cache.heights }
        let natural = subviews.map { $0.sizeThatFits(ProposedViewSize(width: width, height: nil)).height }
        let shared = zip(subviews, natural).filter { $0.0[TMMatchesHeight.self] }.map(\.1).max() ?? 0
        cache.width = width
        cache.heights = zip(subviews, natural).map { $0.0[TMMatchesHeight.self] ? shared : $0.1 }
        return cache.heights
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout Cache) -> CGSize {
        let width = proposal.width ?? subviews.map { $0.sizeThatFits(.unspecified).width }.max() ?? 0
        let heights = heights(width, subviews, &cache)
        let gaps = subviews.dropFirst().map { $0[TMGapBefore.self] }.reduce(0, +)
        return CGSize(width: width, height: heights.reduce(0, +) + gaps)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout Cache) {
        let heights = heights(bounds.width, subviews, &cache)
        var y = bounds.minY
        for (index, subview) in subviews.enumerated() {
            if index > 0 { y += subview[TMGapBefore.self] }
            subview.place(at: CGPoint(x: bounds.minX, y: y), anchor: .topLeading,
                          proposal: ProposedViewSize(width: bounds.width, height: heights[index]))
            y += heights[index]
        }
    }
}
