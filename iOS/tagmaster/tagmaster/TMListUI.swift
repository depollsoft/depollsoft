//
//  TMListUI.swift
//  tagmaster
//
//  The pieces every Tag Master list screen is drawn from: the barber-pole
//  watermark, a tag row, the disclosure chevron and the shared tag store that
//  loads rows on demand. Metrics are the UIKit ones they replaced (DPTagCell,
//  TMLogoBackgroundView, UITableViewCell's disclosure indicator).
//

import SwiftUI
import UIKit

// MARK: - Theme

enum TMTheme {
    static var accent: Color { Color(uiColor: DPAppDelegate.accentColor()) }

    /// The row wash for the tag open beside a list: the accent at 14% (22% in dark).
    static let selectionWash = Color(uiColor: UIColor { traits in
        let alpha: CGFloat = traits.userInterfaceStyle == .dark ? 0.22 : 0.14
        return DPAppDelegate.accentColor().resolvedColor(with: traits).withAlphaComponent(alpha)
    })

    /// A text style as UIKit resolves it, so SwiftUI text matches UILabel metrics exactly.
    static func font(_ style: UIFont.TextStyle) -> Font { Font(UIFont.preferredFont(forTextStyle: style)) }

    static func boldFont(_ style: UIFont.TextStyle) -> Font {
        let base = UIFont.preferredFont(forTextStyle: style)
        let descriptor = base.fontDescriptor.withSymbolicTraits(.traitBold) ?? base.fontDescriptor
        return Font(UIFont(descriptor: descriptor, size: 0))
    }
}

// MARK: - Label metrics

/// Sizes text the way UILabel does: as many lines of the font's own line
/// height as the text wraps to, rounded up to the pixel grid, the glyphs centred
/// in that box. SwiftUI's line boxes are a fraction of a point shorter, which
/// drifts a stack of labels off UIKit's grid by a pixel every few rows.
struct TMLabelBox: Layout {
    let font: UIFont

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        guard let child = subviews.first else { return .zero }
        let size = child.sizeThatFits(proposal)
        let line = child.sizeThatFits(.unspecified).height
        let lines = line > 0 ? max(1, (size.height / line).rounded()) : 1
        let scale = UIScreen.main.scale
        return CGSize(width: size.width, height: (lines * font.lineHeight * scale).rounded(.up) / scale)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        guard let child = subviews.first else { return }
        let size = child.sizeThatFits(ProposedViewSize(width: bounds.width, height: nil))
        child.place(at: CGPoint(x: bounds.minX, y: bounds.midY - size.height / 2), anchor: .topLeading,
                    proposal: ProposedViewSize(width: bounds.width, height: size.height))
    }
}

extension View {
    /// Lays this text, set in `style`, out on UILabel's grid.
    func tmLabelMetrics(_ style: UIFont.TextStyle) -> some View {
        TMLabelBox(font: .preferredFont(forTextStyle: style)) { self }
    }
}

// MARK: - Watermark

/// The barber-pole artwork, fitted into the old 480x800 canvas exactly as
/// TMLogoBackgroundView placed its shape layer.
struct TMLogoShape: Shape {
    func path(in rect: CGRect) -> Path {
        let canvasScale = min(rect.width / 480, rect.height / 800)
        let scale = canvasScale * min(451 / TMLogoWidth, 774 / TMLogoHeight)
        let x = rect.midX + 1.5 * canvasScale - TMLogoWidth * scale / 2
        let y = rect.midY - 4 * canvasScale - TMLogoHeight * scale / 2
        return Path(TMLogoFullPath()).applying(CGAffineTransform(a: scale, b: 0, c: 0, d: scale, tx: x, ty: y))
    }
}

/// The watermark behind a screen: the whole view less 60 points at the top and
/// 44 at the bottom, measured from the screen edges rather than the safe area.
struct TMWatermark: View {
    var body: some View {
        TMLogoShape()
            .fill(Color(.sRGB, red: 128 / 255, green: 128 / 255, blue: 128 / 255, opacity: 76 / 255))
            .padding(.top, 60)
            .padding(.bottom, 44)
            .ignoresSafeArea()
            .allowsHitTesting(false)
            .accessibilityHidden(true)
    }
}

/// A screen's backdrop: its page colour and the watermark. Beside the iPad split's
/// one shared watermark it draws nothing: a grouped screen's colour then belongs
/// on its hosting controller's view (`TMHostingController.pageColor`), where
/// UIKit's sidebar treats it as the old screens' view background.
struct TMScreenBackground: View {
    var grouped = false

    var body: some View {
        if !DPAppDelegate.hasSharedBackground() {
            ZStack {
                Color(uiColor: grouped ? .systemGroupedBackground : .systemBackground).ignoresSafeArea()
                TMWatermark()
            }
        }
    }
}

// MARK: - Disclosure

/// UITableViewCell's disclosure indicator.
struct TMDisclosureChevron: View {
    var body: some View {
        Image(systemName: "chevron.forward")
            .font(.system(size: 13.6, weight: .semibold))
            .foregroundStyle(Color(uiColor: .tertiaryLabel))
            .accessibilityHidden(true)
    }
}

// MARK: - Tag store

/// Loads tags for rows on demand. A cached tag shows at once; anything else is
/// fetched off the main thread and published when it lands. The loader is
/// injectable so tests never reach the catalog.
@MainActor
@Observable
final class TMTagStore {
    static let shared = TMTagStore()

    typealias Loader = @Sendable (Int) -> DPTag?

    /// Bumped whenever a fetch lands, so rows reading the cache look again.
    private(set) var revision = 0
    private(set) var loading: Set<Int> = []
    private var failed: Set<Int> = []
    /// Fetched tags, for a loader that does not also fill the cache.
    private var fetched: [Int: DPTag] = [:]
    private let cached: (Int) -> DPTag?
    private let load: Loader

    init(cached: @escaping (Int) -> DPTag? = { DPTag.load(fromCache: Int32($0)) },
         load: @escaping Loader = { id in
             // loadTagById: raises on some malformed responses; a row just shows its id then.
             TMObjC.catching { DPTag.load(byId: Int32(id), refresh: false) } as? DPTag
         }) {
        self.cached = cached
        self.load = load
    }

    /// The tag for a row, starting a fetch when it is not cached. The cache is
    /// read every time, so a tag refreshed elsewhere shows its new details.
    func tag(_ id: Int) -> DPTag? {
        _ = revision
        if let tag = cached(id) ?? fetched[id] { return tag }
        if !loading.contains(id), !failed.contains(id) { fetch(id) }
        return nil
    }

    func isLoading(_ id: Int) -> Bool { loading.contains(id) }

    /// Forgets failed fetches so rows try again (a retry, or tests starting afresh).
    func reset() {
        failed = []
        fetched = [:]
        revision += 1
    }

    private func fetch(_ id: Int) {
        loading.insert(id)
        let load = self.load
        Task.detached(priority: .userInitiated) {
            let tag = load(id)
            await MainActor.run {
                self.loading.remove(id)
                if let tag, Int(tag.tagId) == id { self.fetched[id] = tag } else { self.failed.insert(id) }
                self.revision += 1
            }
        }
    }
}

// MARK: - Tag row

/// The shape of a tag row, computed once so the view and its tests agree.
struct TMTagRowContent: Equatable {
    let title: String
    let aka: String?
    let details: String
    let hasSheetMusic: Bool
    let hasLearningTracks: Bool
    let accessibilityLabel: String

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "MM/dd/yy"
        formatter.locale = Locale(identifier: "en_US")
        return formatter
    }()

    private static let spokenDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        return formatter
    }()

    init(tagId: Int, tag: DPTag?) {
        title = tag?.title ?? "Tag"
        aka = tag?.alternativeTitle.map { "a.k.a. \($0)" }
        hasSheetMusic = tag?.sheetMusicUri != nil
        hasLearningTracks = (tag?.tracks.count ?? 0) > 0
        guard let tag else {
            details = "Open tag to load details"
            accessibilityLabel = "Tag \(tagId). Open to load details."
            return
        }
        var text = "Posted: " + (tag.posted.map { Self.dateFormatter.string(from: $0) } ?? "Unknown")
        if tag.rating != 0 { text = String(format: "Rating: %1.2f ", tag.rating) + text }
        if tag.downloadCount != 0 { text += " DLs: \(tag.downloadCount)" }
        details = "ID: \(tag.tagId) " + text
        let posted = tag.posted.map { Self.spokenDate.string(from: $0) } ?? "unknown"
        accessibilityLabel = String(format: "%@. %@. Tag ID %d. Rating %.2f out of 5. Posted %@. %d downloads. Sheet music %@. Learning tracks %@.",
                                    title, aka ?? "", tag.tagId, tag.rating, posted, tag.downloadCount,
                                    hasSheetMusic ? "available" : "unavailable",
                                    hasLearningTracks ? "available" : "unavailable")
    }
}

/// One tag in a list: title, a.k.a., the facts line and the two media marks, as
/// DPTagCell drew them.
struct TMTagRow: View {
    let content: TMTagRowContent
    var loading = false
    var showsChevron = true
    var selected = false

    var body: some View {
        HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 4) {
                Text(content.title).font(TMTheme.font(.headline)).tmLabelMetrics(.headline)
                if let aka = content.aka {
                    Text(aka).font(TMTheme.font(.footnote)).tmLabelMetrics(.footnote)
                }
                Text(content.details).font(TMTheme.font(.footnote)).tmLabelMetrics(.footnote)
                // UIKit settled the first mark's row a point taller than the second.
                mark(content.hasSheetMusic, "Sheet music").frame(height: 21)
                mark(content.hasLearningTracks, "Learning tracks").frame(height: 20)
            }
            .foregroundStyle(Color(uiColor: .label))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .overlay {
                if loading {
                    Text("Loading tag…")
                        .font(TMTheme.font(.body))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(Color(uiColor: .systemBackground))
                }
            }
            if showsChevron {
                TMDisclosureChevron().padding(.trailing, 20)
            }
        }
        .contentShape(Rectangle())
    }

    private func mark(_ on: Bool, _ label: String) -> some View {
        HStack(spacing: 8) {
            // UIImageView draws a symbol at its own point size, centred, never stretched.
            Image(systemName: on ? "checkmark" : "xmark")
                .font(TMTheme.font(.body))
                .frame(width: 20, height: 20)
                .foregroundStyle(on ? Color(uiColor: .systemGreen) : Color(uiColor: .secondaryLabel))
            Text(label).font(TMTheme.font(.caption1)).tmLabelMetrics(.caption1)
        }
    }
}

extension View {
    /// A tag row's button names its tag, and reads as selected when that tag is open beside the list.
    func tmTagRowAccessibility(_ content: TMTagRowContent, selected: Bool) -> some View {
        accessibilityLabel(content.accessibilityLabel)
            .accessibilityAddTraits(selected ? .isSelected : [])
            .accessibilityRemoveTraits(selected ? [] : .isSelected)
    }
}
