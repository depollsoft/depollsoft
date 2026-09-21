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

import Foundation
import UIKit

@objc public class SetListSelectorView: UIView {
    /// Machined-part geometry, shared with the range selector.
    static let height: CGFloat = 48
    private static let cornerRadius: CGFloat = 5
    private static let frameStroke: CGFloat = 1.5
    private static let labelSize: CGFloat = 13
    /// The label always starts clear of the indicator dot, lit or not, so a
    /// name never shifts sideways when its position becomes the current one.
    private static let labelLeading: CGFloat = 20
    private static let labelTrailing: CGFloat = 14
    private static let dotDiameter: CGFloat = 6
    private static let dotInset: CGFloat = 8
    private static let maximumLabelWidth: CGFloat = 180
    private static let newPositionWidth: CGFloat = 44

    /// Called with the id of the position the user picked.
    @objc public var onSelect: ((String) -> Void)?
    /// Called when the trailing "+" is pressed.
    @objc public var onCreate: (() -> Void)?
    /// Supplies the menu a long press on a position shows: the list's own actions.
    @objc public var menuForList: ((String) -> UIMenu?)?

    private let scrollView = UIScrollView()
    private let stack = UIStackView()
    private let feedback = UISelectionFeedbackGenerator()
    private var listIds: [String] = []
    private var selectedButton: UIButton?

    /// Every position button, in order, ending with the "+".
    @objc public private(set) var positionButtons: [UIButton] = []

    override public init(frame: CGRect) {
        super.init(frame: frame)
        isAccessibilityElement = false
        backgroundColor = .clear
        clipsToBounds = true
        layer.cornerRadius = SetListSelectorView.cornerRadius
        layer.borderWidth = SetListSelectorView.frameStroke
        applyFrameColor()

        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.showsHorizontalScrollIndicator = false
        scrollView.showsVerticalScrollIndicator = false
        scrollView.backgroundColor = .clear
        addSubview(scrollView)

        stack.translatesAutoresizingMaskIntoConstraints = false
        stack.axis = .horizontal
        stack.alignment = .fill
        // Proportional, so two or three positions share the whole frame instead
        // of huddling at its leading edge; overflow still scrolls.
        stack.distribution = .fillProportionally
        stack.spacing = 0
        scrollView.addSubview(stack)

        NSLayoutConstraint.activate([
            scrollView.leadingAnchor.constraint(equalTo: leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: trailingAnchor),
            scrollView.topAnchor.constraint(equalTo: topAnchor),
            scrollView.bottomAnchor.constraint(equalTo: bottomAnchor),
            stack.leadingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.trailingAnchor),
            stack.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor),
            stack.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor),
            stack.heightAnchor.constraint(equalTo: scrollView.frameLayoutGuide.heightAnchor),
            stack.widthAnchor.constraint(greaterThanOrEqualTo: scrollView.frameLayoutGuide.widthAnchor),
        ])
        feedback.prepare()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    override public var intrinsicContentSize: CGSize {
        CGSize(width: UIView.noIntrinsicMetric, height: SetListSelectorView.height)
    }

    override public func traitCollectionDidChange(_ previous: UITraitCollection?) {
        super.traitCollectionDidChange(previous)
        applyFrameColor()
    }

    private func applyFrameColor() {
        layer.borderColor = DPTheme.plateHairline.resolvedColor(with: traitCollection).cgColor
    }

    // MARK: - Rendering

    /// Rebuilds every position from the model's list order and current list.
    @objc public func render(model: DPSongsModel) {
        let lists = model.orderedLists
        let currentId = model.currentListId
        listIds = lists.map(\.id)
        for view in stack.arrangedSubviews {
            stack.removeArrangedSubview(view)
            view.removeFromSuperview()
        }
        positionButtons = []
        selectedButton = nil

        for (index, list) in lists.enumerated() {
            if index > 0 { stack.addArrangedSubview(makeHairline()) }
            let selected = list.id == currentId
            let button = makePosition(
                title: model.displayName(for: list),
                songCount: list.songs.count,
                identifier: "setlist.\(list.id)",
                selected: selected
            )
            button.tag = index
            button.addTarget(self, action: #selector(positionPressed(_:)), for: .touchUpInside)
            // A tap still switches; the menu is the long press, exactly as UIKit does it.
            button.menu = menuForList?(list.id)
            button.showsMenuAsPrimaryAction = false
            stack.addArrangedSubview(button)
            positionButtons.append(button)
            if selected { selectedButton = button }
        }

        stack.addArrangedSubview(makeHairline())
        let plus = makeNewPosition()
        stack.addArrangedSubview(plus)
        positionButtons.append(plus)

        setNeedsLayout()
    }

    override public func layoutSubviews() {
        super.layoutSubviews()
        scrollSelectionIntoView(animated: false)
    }

    private func scrollSelectionIntoView(animated: Bool) {
        guard let selectedButton, scrollView.bounds.width > 0 else { return }
        let frame = selectedButton.frame.insetBy(dx: -SetListSelectorView.frameStroke, dy: 0)
        guard frame.width > 0, scrollView.contentSize.width > scrollView.bounds.width else { return }
        scrollView.scrollRectToVisible(frame, animated: animated)
    }

    // MARK: - Pieces

    private func makeHairline() -> UIView {
        let hairline = UIView()
        hairline.translatesAutoresizingMaskIntoConstraints = false
        hairline.backgroundColor = DPTheme.plateHairline
        hairline.isUserInteractionEnabled = false
        hairline.widthAnchor.constraint(equalToConstant: 1).isActive = true
        return hairline
    }

    private func makePosition(title: String, songCount: Int, identifier: String, selected: Bool) -> UIButton {
        let button = SetListPositionButton(type: .custom)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.backgroundColor = selected
            ? DPTheme.plateInk.withAlphaComponent(0.10)
            : .clear

        let label = UILabel()
        label.translatesAutoresizingMaskIntoConstraints = false
        label.numberOfLines = 1
        label.lineBreakMode = .byTruncatingTail
        label.isUserInteractionEnabled = false
        label.attributedText = NSAttributedString(
            string: title.uppercased(),
            attributes: [
                .font: DPTheme.condensedFont(size: SetListSelectorView.labelSize),
                .foregroundColor: selected
                    ? DPTheme.plateInk
                    : DPTheme.plateInkSecondary,
                .kern: SetListSelectorView.labelSize * 0.16,
            ]
        )
        button.addSubview(label)
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: button.leadingAnchor,
                                           constant: SetListSelectorView.labelLeading),
            // Not an equality: a proportionally stretched position keeps its
            // label where it started instead of growing past the truncation.
            label.trailingAnchor.constraint(lessThanOrEqualTo: button.trailingAnchor,
                                            constant: -SetListSelectorView.labelTrailing),
            label.centerYAnchor.constraint(equalTo: button.centerYAnchor),
            label.widthAnchor.constraint(lessThanOrEqualToConstant: SetListSelectorView.maximumLabelWidth),
        ])
        // The natural width the stack shares out: the label plus its paddings.
        button.naturalSize = CGSize(
            width: min(label.intrinsicContentSize.width, SetListSelectorView.maximumLabelWidth).rounded(.up)
                + SetListSelectorView.labelLeading + SetListSelectorView.labelTrailing,
            height: SetListSelectorView.height
        )

        if selected {
            // The range selector's indicator, seated inside the leading edge.
            let dot = UIView()
            dot.translatesAutoresizingMaskIntoConstraints = false
            dot.backgroundColor = DPTheme.plateLit
            dot.layer.cornerRadius = SetListSelectorView.dotDiameter / 2
            dot.isUserInteractionEnabled = false
            button.addSubview(dot)
            NSLayoutConstraint.activate([
                dot.leadingAnchor.constraint(equalTo: button.leadingAnchor,
                                             constant: SetListSelectorView.dotInset),
                dot.centerYAnchor.constraint(equalTo: button.centerYAnchor),
                dot.widthAnchor.constraint(equalToConstant: SetListSelectorView.dotDiameter),
                dot.heightAnchor.constraint(equalToConstant: SetListSelectorView.dotDiameter),
            ])
        }

        button.isAccessibilityElement = true
        button.accessibilityIdentifier = identifier
        button.accessibilityTraits = selected ? [.button, .selected] : .button
        button.accessibilityLabel = "\(title), \(SetListSelectorView.songCountPhrase(songCount))"
        return button
    }

    static func songCountPhrase(_ count: Int) -> String {
        switch count {
        case 0: return "no songs"
        case 1: return "1 song"
        default: return "\(count) songs"
        }
    }

    private func makeNewPosition() -> UIButton {
        let button = SetListPositionButton(type: .custom)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.backgroundColor = .clear
        // Fixed: the "+" is a control, not a name, so it never stretches.
        button.naturalSize = CGSize(width: SetListSelectorView.newPositionWidth,
                                    height: SetListSelectorView.height)
        button.widthAnchor.constraint(equalToConstant: SetListSelectorView.newPositionWidth).isActive = true
        button.setAttributedTitle(
            NSAttributedString(
                string: "+",
                attributes: [
                    .font: DPTheme.condensedFont(size: 20),
                    .foregroundColor: DPTheme.plateInkSecondary,
                ]
            ),
            for: .normal
        )
        button.accessibilityIdentifier = "setlist.new"
        button.accessibilityLabel = "New set list"
        button.addTarget(self, action: #selector(newPressed), for: .touchUpInside)
        return button
    }

    // MARK: - Actions

    @objc private func positionPressed(_ sender: UIButton) {
        guard listIds.indices.contains(sender.tag) else { return }
        let id = listIds[sender.tag]
        feedback.selectionChanged()
        feedback.prepare()
        onSelect?(id)
    }

    @objc private func newPressed() {
        onCreate?()
    }
}

/// A position that reports the width it would like, so `fillProportionally`
/// has something to divide: a plain button carrying only a subview label has
/// no intrinsic size of its own.
private final class SetListPositionButton: UIButton {
    var naturalSize: CGSize = .zero
    override var intrinsicContentSize: CGSize { naturalSize }
}
