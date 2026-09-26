//
//  SetListSelectorView.swift
//  pitchperfect
//
//  The set list selector: one machined part with N positions, drawn exactly
//  like the instrument's range selector — a single round-rect frame split by
//  interior hairlines, the chosen position carrying an ink wash and the lit
//  indicator dot. It scrolls horizontally inside its frame when the positions
//  overflow, and always ends in a "+" that makes a new set list.
//

import SwiftUI
import UIKit

enum SetListSelectorMetrics {
    static let height: CGFloat = 48
    static let cornerRadius: CGFloat = 5
    static let frameStroke: CGFloat = 1.5
    static let labelSize: CGFloat = 13
    /// The label always starts clear of the indicator dot, lit or not, so a
    /// name never shifts sideways when its position becomes the current one.
    static let labelLeading: CGFloat = 20
    static let labelTrailing: CGFloat = 14
    static let dotDiameter: CGFloat = 6
    static let dotInset: CGFloat = 8
    static let maximumLabelWidth: CGFloat = 180
    static let newPositionWidth: CGFloat = 44

    static func songCountPhrase(_ count: Int) -> String {
        switch count {
        case 0: return "no songs"
        case 1: return "1 song"
        default: return "\(count) songs"
        }
    }

    static func labelText(_ title: String) -> NSAttributedString {
        NSAttributedString(string: title.uppercased(), attributes: [
            .font: DPTheme.condensedFont(size: labelSize),
            .kern: labelSize * 0.16,
        ])
    }

    /// Where each position sits along the row: the frames UIKit's
    /// `.fillProportionally` stack gave them.
    @MainActor
    static func positionSpans(titles: [String], width: CGFloat) -> [Range<CGFloat>] {
        let frames = StackGeometry.frames(naturals: titles.map(naturalWidth(for:)), width: width)
        // Positions are every other arranged view: position, hairline, position, …
        return titles.indices.map { index in
            let frame = frames[index * 2]
            return frame.minX..<frame.maxX
        }
    }

    /// The width a position would like: its label (capped) plus both paddings.
    static func naturalWidth(for title: String) -> CGFloat {
        let label = ceil(labelText(title).size().width)
        return min(label, maximumLabelWidth).rounded(.up) + labelLeading + labelTrailing
    }
}

struct SetListSelector: View {
    let model: SongListModel
    @State private var scroller = ScrollViewHandle()

    var body: some View {
        let lists = model.lists
        let currentId = model.currentListId
        GeometryReader { frame in
            ScrollView(.horizontal, showsIndicators: false) {
                ProportionalRow(minimumWidth: frame.size.width) {
                    ForEach(Array(lists.enumerated()), id: \.element.id) { index, list in
                        if index > 0 { Hairline() }
                        position(list, selected: list.id == currentId)
                    }
                    Hairline()
                    newPosition
                }
                .background(ScrollViewFinder { scroller.view = $0 }.frame(width: 0, height: 0))
            }
            .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
            // Keeps the chosen position in view whenever the positions change
            // size or place, as the UIKit selector did on every layout.
            .onAppear { reveal(lists: lists, currentId: currentId, width: frame.size.width) }
            .onChange(of: SelectorLayout(currentId: currentId, width: frame.size.width,
                                         positions: lists.map { "\($0.id)\u{0}\(model.displayName($0))" })) { _, layout in
                reveal(lists: lists, currentId: layout.currentId, width: layout.width)
            }
        }
        .frame(height: SetListSelectorMetrics.height)
        .clipShape(RoundedRectangle(cornerRadius: SetListSelectorMetrics.cornerRadius))
        .overlay {
            RoundedRectangle(cornerRadius: SetListSelectorMetrics.cornerRadius)
                .strokeBorder(Plate.hairline, lineWidth: SetListSelectorMetrics.frameStroke)
        }
    }

    /// Scrolls the chosen position into view (with its frame's stroke) once the
    /// row has been laid out with the change, as scrollRectToVisible in the UIKit
    /// selector's layoutSubviews did. (ScrollViewReader cannot reach views placed
    /// by a custom Layout, so the rect comes from the same arithmetic.)
    private func reveal(lists: [DPSongList], currentId: String, width: CGFloat) {
        guard let index = lists.firstIndex(where: { $0.id == currentId }) else { return }
        let spans = SetListSelectorMetrics.positionSpans(titles: lists.map(model.displayName), width: width)
        let span = spans[index]
        DispatchQueue.main.async {
            guard let scrollView = scroller.view, scrollView.bounds.width > 0,
                  scrollView.contentSize.width > scrollView.bounds.width else { return }
            let rect = CGRect(x: span.lowerBound, y: 0, width: span.upperBound - span.lowerBound,
                              height: SetListSelectorMetrics.height)
                .insetBy(dx: -SetListSelectorMetrics.frameStroke, dy: 0)
            scrollView.scrollRectToVisible(rect, animated: false)
        }
    }

    private func position(_ list: DPSongList, selected: Bool) -> some View {
        let title = model.displayName(list)
        return Menu {
            SetListActions(list: list, model: model)
        } label: {
            PositionLabel(title: title, selected: selected)
        } primaryAction: {
            model.choosePosition(listId: list.id)
        }
        .menuStyle(.button)
        .buttonStyle(.plain)
        .menuIndicator(.hidden)
        .accessibilityLabel("\(title), \(SetListSelectorMetrics.songCountPhrase(list.songs.count))")
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
        .accessibilityIdentifier("setlist.\(list.id)")
        .layoutValue(key: NaturalWidth.self, value: SetListSelectorMetrics.naturalWidth(for: title))
    }

    private var newPosition: some View {
        Button { model.promptNewSetList() } label: {
            Text("+")
                .font(Plate.display(20))
                .foregroundStyle(Plate.inkSecondary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("New set list")
        .accessibilityIdentifier("setlist.new")
    }
}

/// The selector's scroll view, once found.
private final class ScrollViewHandle {
    weak var view: UIScrollView?
}

/// Hands over the scroll view this sits in.
private struct ScrollViewFinder: UIViewRepresentable {
    let found: (UIScrollView) -> Void

    final class Finder: UIView {
        var found: (UIScrollView) -> Void = { _ in }
        override func didMoveToWindow() {
            super.didMoveToWindow()
            var view = superview
            while let current = view, !(current is UIScrollView) { view = current.superview }
            if let scrollView = view as? UIScrollView { found(scrollView) }
        }
    }

    func makeUIView(context: Context) -> Finder {
        let finder = Finder()
        finder.isUserInteractionEnabled = false
        finder.isAccessibilityElement = false
        return finder
    }

    func updateUIView(_ finder: Finder, context: Context) { finder.found = found }
}

/// What the selector's scroll position depends on.
private struct SelectorLayout: Equatable {
    var currentId: String
    var width: CGFloat
    var positions: [String]
}

private struct PositionLabel: View {
    let title: String
    let selected: Bool

    var body: some View {
        ZStack(alignment: .leading) {
            (selected ? Plate.ink.opacity(0.10) : Color.clear)
            if selected {
                Circle()
                    .fill(Plate.lit)
                    .frame(width: SetListSelectorMetrics.dotDiameter, height: SetListSelectorMetrics.dotDiameter)
                    .padding(.leading, SetListSelectorMetrics.dotInset)
            }
            Text(title.uppercased())
                .font(Plate.display(SetListSelectorMetrics.labelSize))
                .kerning(SetListSelectorMetrics.labelSize * 0.16)
                .foregroundStyle(selected ? Plate.ink : Plate.inkSecondary)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: SetListSelectorMetrics.maximumLabelWidth, alignment: .leading)
                .padding(.leading, SetListSelectorMetrics.labelLeading)
                .padding(.trailing, SetListSelectorMetrics.labelTrailing)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .contentShape(Rectangle())
    }
}

private struct Hairline: View {
    var body: some View {
        Plate.hairline
            .frame(width: 1)
            .accessibilityHidden(true)
    }
}

private struct NaturalWidth: LayoutValueKey {
    static let defaultValue: CGFloat? = nil
}

/// The UIKit selector's own stack, laid out off screen, so the SwiftUI row puts
/// every piece exactly where `.fillProportionally` did. (Its division of spare
/// width is not a plain proportional share: the fixed hairlines and "+" take
/// part in it and are then held to their constraints.)
@MainActor
enum StackGeometry {
    private final class Sized: UIView {
        var natural: CGSize = .zero
        override var intrinsicContentSize: CGSize { natural }
    }

    private static var cache: [[CGFloat]: [CGRect]] = [:]

    /// Frames of the arranged views in order: position, hairline, position, …,
    /// hairline, "+". `naturals` are the positions' natural widths; `width` is
    /// the frame the row scrolls within.
    static func frames(naturals: [CGFloat], width: CGFloat) -> [CGRect] {
        let key = naturals + [width]
        if let frames = cache[key] { return frames }
        let height = SetListSelectorMetrics.height
        let scrollView = UIScrollView(frame: CGRect(x: 0, y: 0, width: width, height: height))
        let stack = UIStackView()
        stack.translatesAutoresizingMaskIntoConstraints = false
        stack.axis = .horizontal
        stack.alignment = .fill
        stack.distribution = .fillProportionally
        stack.spacing = 0
        scrollView.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.trailingAnchor),
            stack.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor),
            stack.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor),
            stack.heightAnchor.constraint(equalTo: scrollView.frameLayoutGuide.heightAnchor),
            stack.widthAnchor.constraint(greaterThanOrEqualTo: scrollView.frameLayoutGuide.widthAnchor),
        ])
        func hairline() -> UIView {
            let view = UIView()
            view.translatesAutoresizingMaskIntoConstraints = false
            view.widthAnchor.constraint(equalToConstant: 1).isActive = true
            return view
        }
        for (index, natural) in naturals.enumerated() {
            if index > 0 { stack.addArrangedSubview(hairline()) }
            let position = Sized()
            position.translatesAutoresizingMaskIntoConstraints = false
            position.natural = CGSize(width: natural, height: height)
            stack.addArrangedSubview(position)
        }
        stack.addArrangedSubview(hairline())
        let plus = Sized()
        plus.translatesAutoresizingMaskIntoConstraints = false
        plus.natural = CGSize(width: SetListSelectorMetrics.newPositionWidth, height: height)
        plus.widthAnchor.constraint(equalToConstant: SetListSelectorMetrics.newPositionWidth).isActive = true
        stack.addArrangedSubview(plus)
        scrollView.layoutIfNeeded()
        let frames = stack.arrangedSubviews.map(\.frame)
        cache[key] = frames
        return frames
    }
}

/// Places the selector's pieces at the frames UIKit's stack gave them. When the
/// positions do not fit, every position keeps its natural width and the row scrolls.
private struct ProportionalRow: Layout {
    /// The frame the row scrolls within; the positions never leave it part-empty.
    var minimumWidth: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let frames = Self.frames(subviews, width: minimumWidth)
        let height = proposal.height ?? SetListSelectorMetrics.height
        return CGSize(width: max(frames.last?.maxX ?? 0, minimumWidth), height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let frames = Self.frames(subviews, width: minimumWidth)
        for (subview, frame) in zip(subviews, frames) {
            subview.place(at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY), anchor: .topLeading,
                          proposal: ProposedViewSize(width: frame.width, height: bounds.height))
        }
    }

    private static func frames(_ subviews: Subviews, width: CGFloat) -> [CGRect] {
        let naturals = subviews.compactMap { $0[NaturalWidth.self] }
        return MainActor.assumeIsolated { StackGeometry.frames(naturals: naturals, width: width) }
    }
}
