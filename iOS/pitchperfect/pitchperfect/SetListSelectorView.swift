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
    private let feedback = UISelectionFeedbackGenerator()

    var body: some View {
        let lists = model.lists
        let currentId = model.currentListId
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                ProportionalRow {
                    ForEach(Array(lists.enumerated()), id: \.element.id) { index, list in
                        if index > 0 { Hairline() }
                        position(list, selected: list.id == currentId)
                            .id(list.id)
                    }
                    Hairline()
                    newPosition
                }
            }
            .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
            .onAppear { proxy.scrollTo(currentId) }
            .onChange(of: currentId) { _, id in proxy.scrollTo(id) }
        }
        .frame(height: SetListSelectorMetrics.height)
        .clipShape(RoundedRectangle(cornerRadius: SetListSelectorMetrics.cornerRadius))
        .overlay {
            RoundedRectangle(cornerRadius: SetListSelectorMetrics.cornerRadius)
                .strokeBorder(Plate.hairline, lineWidth: SetListSelectorMetrics.frameStroke)
        }
    }

    private func position(_ list: DPSongList, selected: Bool) -> some View {
        let title = model.displayName(list)
        return Menu {
            SetListActions(list: list, model: model)
        } label: {
            PositionLabel(title: title, selected: selected)
        } primaryAction: {
            feedback.selectionChanged()
            feedback.prepare()
            model.select(listId: list.id)
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
        .layoutValue(key: FixedWidth.self, value: SetListSelectorMetrics.newPositionWidth)
    }
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
            .layoutValue(key: FixedWidth.self, value: 1)
    }
}

private struct NaturalWidth: LayoutValueKey {
    static let defaultValue: CGFloat? = nil
}

private struct FixedWidth: LayoutValueKey {
    static let defaultValue: CGFloat? = nil
}

/// UIStackView's `.fillProportionally`: fixed pieces keep their width; the
/// positions share what is left in proportion to their natural widths, so two
/// or three positions fill the whole frame. When they do not fit, every
/// position takes its natural width and the row scrolls.
private struct ProportionalRow: Layout {
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let natural = subviews.reduce(CGFloat(0)) { $0 + width(of: $1) }
        let height = proposal.height ?? SetListSelectorMetrics.height
        return CGSize(width: max(natural, proposal.width ?? natural), height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let fixed = subviews.filter { $0[FixedWidth.self] != nil }.reduce(CGFloat(0)) { $0 + width(of: $1) }
        let flexible = subviews.filter { $0[FixedWidth.self] == nil }.reduce(CGFloat(0)) { $0 + width(of: $1) }
        let scale = flexible > 0 ? max(1, (bounds.width - fixed) / flexible) : 1
        var x = bounds.minX
        for subview in subviews {
            let natural = width(of: subview)
            let width = subview[FixedWidth.self] != nil ? natural : natural * scale
            subview.place(at: CGPoint(x: x, y: bounds.minY), anchor: .topLeading,
                          proposal: ProposedViewSize(width: width, height: bounds.height))
            x += width
        }
    }

    private func width(of subview: LayoutSubview) -> CGFloat {
        subview[FixedWidth.self] ?? subview[NaturalWidth.self] ?? 0
    }
}
