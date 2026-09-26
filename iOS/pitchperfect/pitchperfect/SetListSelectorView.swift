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

    /// The width a position would like: its label (capped) plus both paddings.
    static func naturalWidth(for title: String) -> CGFloat {
        let label = ceil(labelText(title).size().width)
        return min(label, maximumLabelWidth).rounded(.up) + labelLeading + labelTrailing
    }
}

struct SetListSelector: View {
    let model: SongListModel
    @State private var scroller = ScrollViewHandle()
    @State private var geometry = SelectorGeometry()

    var body: some View {
        let lists = model.lists
        let currentId = model.currentListId
        let titles = lists.map(model.displayName)
        GeometryReader { frame in
            // Measured here, on the main actor, where UIKit may be asked; the row's
            // Layout only places what it is given (SwiftUI may lay out off the main thread).
            let frames = geometry.frames(titles: titles, width: frame.size.width)
            ScrollView(.horizontal, showsIndicators: false) {
                ProportionalRow(frames: frames, minimumWidth: frame.size.width) {
                    ForEach(Array(lists.enumerated()), id: \.element.id) { index, list in
                        if index > 0 { Hairline() }
                        position(list, selected: list.id == currentId)
                    }
                    Hairline()
                    newPosition
                }
                .background(EnclosingScrollView(UIScrollView.self) { scroller.view = $0 }.frame(width: 0, height: 0))
            }
            .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
            // Keeps the chosen position in view whenever the positions change
            // size or place, as the UIKit selector did on every layout.
            .onAppear { reveal(lists: lists, currentId: currentId, frames: frames) }
            .onChange(of: SelectorLayout(currentId: currentId, width: frame.size.width,
                                         positions: lists.map { "\($0.id)\u{0}\(model.displayName($0))" })) { _, layout in
                reveal(lists: lists, currentId: layout.currentId, frames: frames)
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
    /// selector's layoutSubviews did.
    private func reveal(lists: [DPSongList], currentId: String, frames: [CGRect]) {
        guard let index = lists.firstIndex(where: { $0.id == currentId }), frames.indices.contains(index * 2) else { return }
        let position = frames[index * 2]
        DispatchQueue.main.async {
            guard let scrollView = scroller.view, scrollView.bounds.width > 0,
                  scrollView.contentSize.width > scrollView.bounds.width else { return }
            let rect = CGRect(x: position.minX, y: 0, width: position.width, height: SetListSelectorMetrics.height)
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

    /// Frames of the arranged views in order: position, hairline, position, …,
    /// hairline, "+". `naturals` are the positions' natural widths; `width` is
    /// the frame the row scrolls within.
    static func frames(naturals: [CGFloat], width: CGFloat) -> [CGRect] {
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
        return stack.arrangedSubviews.map(\.frame)
    }
}

/// One selector's frames for its current titles and width: measured again only
/// when either changes, and never kept for layouts it no longer has.
@MainActor
final class SelectorGeometry {
    private var key: [CGFloat] = []
    private var cached: [CGRect] = []

    func frames(titles: [String], width: CGFloat) -> [CGRect] {
        let naturals = titles.map(SetListSelectorMetrics.naturalWidth(for:))
        let key = naturals + [width]
        if key != self.key {
            self.key = key
            cached = StackGeometry.frames(naturals: naturals, width: width)
        }
        return cached
    }
}

/// Places the selector's pieces at the frames UIKit's stack gave them (measured
/// by the selector). When the positions do not fit, every position keeps its
/// natural width and the row scrolls.
private struct ProportionalRow: Layout {
    let frames: [CGRect]
    /// The frame the row scrolls within; the positions never leave it part-empty.
    var minimumWidth: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let height = proposal.height ?? SetListSelectorMetrics.height
        return CGSize(width: max(frames.last?.maxX ?? 0, minimumWidth), height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        for (subview, frame) in zip(subviews, frames) {
            subview.place(at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY), anchor: .topLeading,
                          proposal: ProposedViewSize(width: frame.width, height: bounds.height))
        }
    }
}
