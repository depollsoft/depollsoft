//
//  TMListUI.swift
//  tagmaster
//
//  The pieces every Tag Master list screen is drawn from: the barber-pole
//  watermark, a tag row, the disclosure chevron and the shared tag store that
//  loads rows on demand. Metrics are the UIKit ones they replaced (DPTagCell,
//  the old UIKit watermark view, UITableViewCell's disclosure indicator).
//

import SwiftUI
import UIKit

// MARK: - Theme

extension EnvironmentValues {
    /// True while UIKit dims the screen's tint (an alert or sheet is up).
    @Entry var tmTintDimmed = false
}

enum TMTheme {
    static var accent: Color { Color(uiColor: DPAppDelegate.accentColor()) }

    /// A tint as UIKit shows it: its own colour, or UIKit's dimmed grey while dimmed.
    static func tint(_ color: UIColor, dimmed: Bool) -> Color {
        guard dimmed else { return Color(uiColor: color) }
        let probe = UIView()
        probe.tintColor = color
        probe.tintAdjustmentMode = .dimmed
        return Color(uiColor: probe.tintColor)
    }

    /// The row wash for the tag open beside a list: the accent at 14% (22% in dark).
    static let selectionWash = Color(uiColor: UIColor { traits in
        let alpha: CGFloat = traits.userInterfaceStyle == .dark ? 0.22 : 0.14
        return DPAppDelegate.accentColor().resolvedColor(with: traits).withAlphaComponent(alpha)
    })

}

extension DynamicTypeSize {
    /// The text size as UIKit traits, for resolving fonts the way UILabel does.
    var tmTraits: UITraitCollection {
        UITraitCollection(preferredContentSizeCategory: UIContentSizeCategory(self))
    }

    /// A text style as UIKit resolves it at this size, so SwiftUI text matches UILabel metrics exactly.
    func tmFont(_ style: UIFont.TextStyle, bold: Bool = false) -> UIFont {
        let base = UIFont.preferredFont(forTextStyle: style, compatibleWith: tmTraits)
        guard bold, let descriptor = base.fontDescriptor.withSymbolicTraits(.traitBold) else { return base }
        return UIFont(descriptor: descriptor, size: 0)
    }

    var tmBodyPointSize: CGFloat { tmFont(.body).pointSize }
}

/// Sets a UIKit text style at the text size in the environment, so a window's (or
/// the system's) text size change re-renders the text, as UILabel's
/// adjustsFontForContentSizeCategory did.
private struct TMFontModifier: ViewModifier {
    let style: UIFont.TextStyle
    let bold: Bool
    @Environment(\.dynamicTypeSize) private var size

    func body(content: Content) -> some View {
        content.font(Font(size.tmFont(style, bold: bold)))
    }
}

extension View {
    func tmFont(_ style: UIFont.TextStyle, bold: Bool = false) -> some View {
        modifier(TMFontModifier(style: style, bold: bold))
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
        // Measure as placement does, free of the proposed height: a height limit
        // would clamp wrapped text to one line here, and the text would then
        // overflow its box, over its neighbours, once placed.
        let size = child.sizeThatFits(ProposedViewSize(width: proposal.width, height: nil))
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
        modifier(TMLabelMetricsModifier(style: style))
    }
}

private struct TMLabelMetricsModifier: ViewModifier {
    let style: UIFont.TextStyle
    @Environment(\.dynamicTypeSize) private var size

    func body(content: Content) -> some View {
        TMLabelBox(font: size.tmFont(style)) { content }
    }
}

// MARK: - Watermark

/// The barber-pole artwork, fitted into the old 480x800 canvas exactly as
/// the UIKit watermark view placed its shape layer.
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

/// A screen's backdrop: its page colour and the watermark. Inside the iPad
/// split, which draws one watermark behind both columns, a plain screen stays
/// clear and a grouped one keeps only its colour.
struct TMScreenBackground: View {
    var grouped = false
    @Environment(\.tmSharedWatermark) private var sharedWatermark

    var body: some View {
        if sharedWatermark {
            // UIKit set a grouped screen's colour on its own view, beneath the
            // sidebar's glass; painted in SwiftUI it would sit above the glass.
            if grouped { TMPageColorHook(color: .systemGroupedBackground) }
        } else {
            ZStack {
                Color(uiColor: grouped ? .systemGroupedBackground : .systemBackground).ignoresSafeArea()
                TMWatermark()
            }
        }
    }
}

/// Colours the view of the controller a screen is hosted in, the layer UIKit
/// screens painted their page colour on.
private struct TMPageColorHook: UIViewControllerRepresentable {
    let color: UIColor

    final class Hook: UIViewController {
        var color: UIColor = .clear

        /// The screen's own controller: the ancestor a navigation controller holds.
        private var screen: UIViewController? {
            var controller: UIViewController? = parent
            while let current = controller, !(current.parent is UINavigationController) {
                controller = current.parent
            }
            return controller
        }

        func apply() {
            guard let view = screen?.view, view.backgroundColor != color else { return }
            view.backgroundColor = color
        }

        override func didMove(toParent parent: UIViewController?) {
            super.didMove(toParent: parent)
            apply()
        }

        override func viewWillAppear(_ animated: Bool) {
            super.viewWillAppear(animated)
            apply()
        }

        override func viewDidLayoutSubviews() {
            super.viewDidLayoutSubviews()
            apply()
        }
    }

    func makeUIViewController(context: Context) -> Hook {
        let hook = Hook()
        hook.view.isHidden = true
        hook.view.isUserInteractionEnabled = false
        hook.color = color
        return hook
    }

    func updateUIViewController(_ hook: Hook, context: Context) {
        hook.color = color
        hook.apply()
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
    /// When each failed fetch failed. A row asks again once `retryInterval` has
    /// passed, and every failure is forgotten when the app comes back or the lists
    /// change, as the UIKit cell fetched afresh each time it was configured.
    private var failed: [Int: Date] = [:]
    /// Fetched tags, for a loader that does not also fill the cache.
    private var fetched: [Int: DPTag] = [:]
    private let cached: (Int) -> DPTag?
    private let load: Loader
    private let now: () -> Date
    static let retryInterval: TimeInterval = 30
    private var observers: [NSObjectProtocol] = []

    init(cached: @escaping (Int) -> DPTag? = { DPTag.load(fromCache: Int32($0)) },
         load: @escaping Loader = { id in
             // loadTagById: raises on some malformed responses; a row just shows its id then.
             TMObjC.catching { DPTag.load(byId: Int32(id), refresh: false) } as? DPTag
         },
         now: @escaping () -> Date = Date.init) {
        self.cached = cached
        self.load = load
        self.now = now
        let center = NotificationCenter.default
        for name in [UIApplication.didBecomeActiveNotification, Notification.Name.userDataChanged] {
            observers.append(center.addObserver(forName: name, object: nil, queue: .main) { [weak self] _ in
                MainActor.assumeIsolated { self?.retryFailed() }
            })
        }
    }

    isolated deinit {
        observers.forEach(NotificationCenter.default.removeObserver)
    }

    /// The tag for a row, starting a fetch when it is not cached. The cache is
    /// read every time, so a tag refreshed elsewhere shows its new details.
    func tag(_ id: Int) -> DPTag? {
        _ = revision
        if let tag = cached(id) ?? fetched[id] { return tag }
        if !loading.contains(id), canFetch(id) { fetch(id) }
        return nil
    }

    func isLoading(_ id: Int) -> Bool { loading.contains(id) }

    private func canFetch(_ id: Int) -> Bool {
        guard let failedAt = failed[id] else { return true }
        return now().timeIntervalSince(failedAt) >= TMTagStore.retryInterval
    }

    /// Forgets failed fetches so their rows ask again.
    func retryFailed() {
        guard !failed.isEmpty else { return }
        failed = [:]
        revision += 1
    }

    /// Forgets failed and fetched tags (tests starting afresh).
    func reset() {
        failed = [:]
        fetched = [:]
        revision += 1
    }

    private func fetch(_ id: Int) {
        loading.insert(id)
        failed[id] = nil
        let load = self.load
        Task.detached(priority: .userInitiated) {
            let tag = load(id)
            await MainActor.run {
                self.loading.remove(id)
                if let tag, Int(tag.tagId) == id { self.fetched[id] = tag } else { self.failed[id] = self.now() }
                self.revision += 1
            }
        }
    }
}

// MARK: - Row identity

/// A saved list may name a tag twice (edited on another device, say); UITableView
/// did not mind, but SwiftUI rows need distinct identities. Each occurrence is
/// keyed by its tag and how many times that tag came before it, so a row keeps
/// its identity through moves, and only the first occurrence is a scroll target.
struct TMListedTag: Identifiable, Hashable {
    let tagId: Int
    let occurrence: Int
    var id: String { "\(tagId)#\(occurrence)" }
    var isFirst: Bool { occurrence == 0 }

    static func keyed(_ ids: [Int]) -> [TMListedTag] {
        var seen: [Int: Int] = [:]
        return ids.map { id in
            let occurrence = seen[id, default: 0]
            seen[id] = occurrence + 1
            return TMListedTag(tagId: id, occurrence: occurrence)
        }
    }
}

extension View {
    /// The scroll target for a listed tag: its first occurrence only.
    @ViewBuilder
    func tmScrollTarget(_ listed: TMListedTag) -> some View {
        if listed.isFirst { id(listed.tagId) } else { self }
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
    @Environment(\.tmTintDimmed) private var dimmed
    let content: TMTagRowContent
    var loading = false
    var showsChevron = true
    var selected = false

    var body: some View {
        HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 4) {
                Text(content.title).tmFont(.headline).tmLabelMetrics(.headline)
                if let aka = content.aka {
                    Text(aka).tmFont(.footnote).tmLabelMetrics(.footnote)
                }
                Text(content.details).tmFont(.footnote).tmLabelMetrics(.footnote)
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
                        .tmFont(.body)
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
                .tmFont(.body)
                .frame(width: 20, height: 20)
                .foregroundStyle(on ? TMTheme.tint(.systemGreen, dimmed: dimmed) : Color(uiColor: .secondaryLabel))
            Text(label).tmFont(.caption1).tmLabelMetrics(.caption1)
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

/// Reads UIKit's tint dimming where the screen sits and hands it to SwiftUI.
private struct TMTintDimmingReader: UIViewRepresentable {
    @Binding var dimmed: Bool

    final class Probe: UIView {
        var changed: ((Bool) -> Void)?
        override func tintColorDidChange() {
            super.tintColorDidChange()
            changed?(tintAdjustmentMode == .dimmed)
        }
    }

    func makeUIView(context: Context) -> Probe {
        let probe = Probe()
        probe.isUserInteractionEnabled = false
        probe.changed = { value in DispatchQueue.main.async { if dimmed != value { dimmed = value } } }
        return probe
    }

    func updateUIView(_ probe: Probe, context: Context) {}
}

private struct TMTintDimming: ViewModifier {
    @State private var dimmed = false

    func body(content: Content) -> some View {
        content
            .environment(\.tmTintDimmed, dimmed)
            .background { TMTintDimmingReader(dimmed: $dimmed).accessibilityHidden(true) }
    }
}

extension View {
    /// Tinted colours inside follow UIKit's dimming, as UIKit views did.
    func tmFollowsTintDimming() -> some View { modifier(TMTintDimming()) }
}
