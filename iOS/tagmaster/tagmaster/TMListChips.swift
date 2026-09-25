//
//  TMListChips.swift
//  tagmaster
//
//  The "In lists" row under a tag's title: one capsule per list the tag
//  belongs to, then an assist capsule that opens the picker. The name half of
//  a membership capsule opens that list; its visible remove button takes the
//  tag out of it, undoably. Capsules wrap like text.
//

import SwiftUI

struct TMListChips: View {
    let model: TagSummaryModel

    var body: some View {
        let keys = model.memberships
        if model.tag != nil {
            TMFlowLayout(spacing: 8, lineSpacing: 8) {
                ForEach(keys, id: \.self) { key in
                    TMListChip(model: model, key: key)
                }
                TMListChip(model: model, key: nil)
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel("In lists")
            .accessibilityIdentifier("summary.chips")
        }
    }
}

/// One capsule: an outlined neutral pill holding the list's name, and, for a list
/// the tag is in, its own remove button. The two are separate accessibility
/// elements, so "open" and "remove" are the two distinct things they are.
struct TMListChip: View {
    @Environment(\.tmAccent) private var accent
    let model: TagSummaryModel
    /// The list this capsule stands for; nil on the trailing assist capsule.
    let key: String?

    static let removeWidth: CGFloat = 44
    static let minimumHeight: CGFloat = 44

    var body: some View {
        HStack(spacing: 0) {
            nameButton
            if let key {
                removeButton(key)
            }
        }
        .frame(minHeight: TMListChip.minimumHeight)
        .overlay(Capsule(style: .continuous).strokeBorder(Color(.separator), lineWidth: 1))
        .modifier(TMChipPickerAnchor(model: model, isAssist: key == nil))
        .contextMenu {
            if let key {
                Button(role: .destructive) { model.remove(from: key) } label: {
                    Label("Remove from \(TMTagLists.name(for: key))", systemImage: "minus.circle")
                }
            }
        }
    }

    private var name: String { key.map(TMTagLists.name(for:)) ?? "Add to list" }

    private var nameButton: some View {
        let accent = self.accent
        return Button {
            if let key { model.openList(key) } else { model.showPicker() }
        } label: {
            HStack(spacing: 6) {
                Image(systemName: key.map(TagSummaryModel.symbolName(for:)) ?? "plus")
                    .imageScale(.large)
                    .foregroundStyle(key == nil ? accent : Color(.secondaryLabel))
                Text(name)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .foregroundStyle(key == nil ? accent : Color(.label))
            }
            .font(.subheadline)
            .padding(.top, 8)
            .padding(.bottom, 8)
            .padding(.leading, 14)
            .padding(.trailing, key == nil ? 14 : 4)
            .frame(minHeight: TMListChip.minimumHeight)
            .contentShape(Rectangle())
        }
        .buttonStyle(.borderless)
        .accessibilityLabel(name)
        .accessibilityHint(key.map { "Opens \(TMTagLists.name(for: $0))" } ?? "")
        .accessibilityIdentifier(key.map { "summary.chip.\($0)" } ?? "summary.chip.add")
        .accessibilityActions {
            if let key {
                Button("Remove from \(name)") { model.remove(from: key) }
            }
        }
    }

    private func removeButton(_ key: String) -> some View {
        Button { model.remove(from: key) } label: {
            Image(systemName: "xmark.circle.fill")
                .font(.subheadline)
                .imageScale(.large)
                .foregroundStyle(Color(.secondaryLabel))
                .frame(width: TMListChip.removeWidth)
                .frame(maxHeight: .infinity)
                .contentShape(Rectangle())
        }
        .buttonStyle(.borderless)
        .accessibilityLabel("Remove from \(name)")
        .accessibilityIdentifier("summary.chip.\(key).remove")
    }
}

/// The assist chip is where the picker it opens is anchored.
private struct TMChipPickerAnchor: ViewModifier {
    let model: TagSummaryModel
    let isAssist: Bool

    func body(content: Content) -> some View {
        if isAssist, let detail = model.detail {
            content.tmListPicker(model: detail, source: .chip)
        } else {
            content
        }
    }
}
