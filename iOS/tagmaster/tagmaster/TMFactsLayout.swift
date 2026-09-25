//
//  TMFactsLayout.swift
//  tagmaster
//
//  Caption/value rows for the Summary's facts and the Details page: every
//  caption in one column as wide as the widest caption, values 8 pt to its
//  right on the caption's baseline, and each row at least a touch target tall.
//  When a caption and a reasonable value cannot share the width (large text,
//  narrow columns) every row stacks its value under its caption instead.
//

import SwiftUI

/// What kind of value a row holds, which decides how much width it asks for.
enum TMFactValueKind {
    /// Text (or a link): wants its natural width, capped at eight ems.
    case text
    /// A composite control (the rating): wants all of its natural width.
    case unit
    /// A link: measured as text, but its touch target fills the whole row.
    case link
}

private struct TMFactValueKindKey: LayoutValueKey {
    static let defaultValue: TMFactValueKind = .text
}

private struct TMFactMinimumHeightKey: LayoutValueKey {
    static let defaultValue: CGFloat = 44
}

private struct TMFactGapBeforeKey: LayoutValueKey {
    static let defaultValue: CGFloat = 0
}

extension View {
    /// Marks a value subview of `TMFactsLayout`.
    func tmFactValue(_ kind: TMFactValueKind, minimumHeight: CGFloat = 44, gapBefore: CGFloat = 0) -> some View {
        layoutValue(key: TMFactValueKindKey.self, value: kind)
            .layoutValue(key: TMFactMinimumHeightKey.self, value: minimumHeight)
            .layoutValue(key: TMFactGapBeforeKey.self, value: gapBefore)
    }
}

/// Lays out subviews as caption/value pairs: caption 0, value 0, caption 1, value 1…
struct TMFactsLayout: Layout {
    /// Point size of the value font, for the eight-em cap on a text value's width.
    var valueFontSize: CGFloat = 17

    private struct Row {
        var captionFrame: CGRect
        var valueFrame: CGRect
        var height: CGFloat
        var gapBefore: CGFloat
    }

    private func rows(width: CGFloat, subviews: Subviews) -> [Row] {
        let pairs = stride(from: 0, to: subviews.count - 1, by: 2).map { (subviews[$0], subviews[$0 + 1]) }
        var captionWidth: CGFloat = 0
        var valueWidth: CGFloat = 96
        for (caption, value) in pairs {
            captionWidth = max(captionWidth, ceil(caption.sizeThatFits(.unspecified).width))
            let natural = value.sizeThatFits(.unspecified).width
            switch value[TMFactValueKindKey.self] {
            case .text, .link: valueWidth = max(valueWidth, min(natural, valueFontSize * 8))
            case .unit: valueWidth = max(valueWidth, natural)
            }
        }
        let stacked = captionWidth + 8 + valueWidth > width
        var result: [Row] = []
        for (index, (caption, value)) in pairs.enumerated() {
            let minimum = value[TMFactMinimumHeightKey.self]
            let captionSize = stacked ? width : captionWidth
            let valueX = stacked ? 0 : captionWidth + 8
            let valueW = max(1, width - valueX)
            let captionFit = caption.sizeThatFits(ProposedViewSize(width: captionSize, height: nil))
            let valueFit = value.sizeThatFits(ProposedViewSize(width: valueW, height: nil))
            let captionHeight = ceil(captionFit.height)
            let valueHeight = ceil(valueFit.height)
            var captionY: CGFloat = 0
            var valueY: CGFloat = stacked ? captionHeight + 4 : 0
            if !stacked {
                let captionDimensions = caption.dimensions(in: ProposedViewSize(width: captionSize, height: nil))
                let valueDimensions = value.dimensions(in: ProposedViewSize(width: valueW, height: nil))
                let captionBaseline = captionDimensions[.firstTextBaseline]
                let valueBaseline = valueDimensions[.firstTextBaseline]
                captionY = max(0, valueBaseline - captionBaseline)
                valueY = max(0, captionBaseline - valueBaseline)
            }
            let natural = max(captionY + captionHeight, valueY + valueHeight)
            let lineHeight = UIFont.preferredFont(forTextStyle: .body).lineHeight
            let kind = value[TMFactValueKindKey.self]
            let multiline = stacked || (kind == .text && valueHeight > lineHeight * 1.5)
            let fitted = ceil(max(minimum, natural + (multiline ? 8 : 0)))
            let offset = (fitted - natural) / 2
            // A link's target is the whole row; its text stays centred where it was measured.
            let linkGrowth = kind == .link && !stacked ? max(0, fitted - valueHeight) : 0
            let gap = index == 0 ? 0 : (value[TMFactGapBeforeKey.self] > 0 ? value[TMFactGapBeforeKey.self] : (stacked ? 4 : 0))
            // UILabel draws its text centred in a frame rounded up to whole points.
            let captionInset = (captionHeight - captionFit.height) / 2
            let valueInset = kind == .unit ? 0 : (valueHeight - valueFit.height) / 2
            result.append(Row(captionFrame: CGRect(x: 0, y: captionY + offset + captionInset, width: captionSize,
                                                   height: captionFit.height),
                              valueFrame: CGRect(x: valueX, y: valueY + offset - linkGrowth / 2 + valueInset, width: valueW,
                                                 height: valueFit.height + linkGrowth),
                              height: fitted, gapBefore: gap))
        }
        return result
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 320
        let rows = rows(width: width, subviews: subviews)
        let height = rows.reduce(0) { $0 + $1.gapBefore + $1.height }
        return CGSize(width: width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var y = bounds.minY
        for (index, row) in rows(width: bounds.width, subviews: subviews).enumerated() {
            y += row.gapBefore
            // UILabel's text lands on the pixel above a fractional frame origin.
            let scale = UIScreen.main.scale
            let captionY = ((y + row.captionFrame.minY) * scale).rounded(.down) / scale
            subviews[index * 2].place(at: CGPoint(x: bounds.minX + row.captionFrame.minX, y: captionY),
                                      proposal: ProposedViewSize(row.captionFrame.size))
            let valueY = ((y + row.valueFrame.minY) * scale).rounded(.down) / scale
            subviews[index * 2 + 1].place(at: CGPoint(x: bounds.minX + row.valueFrame.minX, y: valueY),
                                          proposal: ProposedViewSize(row.valueFrame.size))
            y += row.height
        }
    }
}

/// A flowing row of capsules that wraps like text: 8 pt between capsules and between lines.
struct TMFlowLayout: Layout {
    var spacing: CGFloat = 8
    var lineSpacing: CGFloat = 8

    private func arrange(width: CGFloat, subviews: Subviews) -> (frames: [CGRect], height: CGFloat) {
        var frames: [CGRect] = []
        var x: CGFloat = 0, y: CGFloat = 0, lineHeight: CGFloat = 0
        let available = max(width, 1)
        for subview in subviews {
            var size = subview.sizeThatFits(ProposedViewSize(width: available, height: nil))
            size.width = min(size.width, available)
            if x > 0, x + size.width > available + 0.5 {
                x = 0
                y += lineHeight + lineSpacing
                lineHeight = 0
            }
            frames.append(CGRect(origin: CGPoint(x: x, y: y), size: size))
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
        return (frames, subviews.isEmpty ? 0 : y + lineHeight)
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? UIScreen.main.bounds.width
        return CGSize(width: width, height: arrange(width: width, subviews: subviews).height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let frames = arrange(width: bounds.width, subviews: subviews).frames
        for (subview, frame) in zip(subviews, frames) {
            subview.place(at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY + frame.minY),
                          proposal: ProposedViewSize(frame.size))
        }
    }
}
