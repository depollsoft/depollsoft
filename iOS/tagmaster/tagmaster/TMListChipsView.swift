//
//  TMListChipsView.swift
//  tagmaster
//
//  The "In lists" row under a tag's title: one capsule per list the tag belongs
//  to, then an assist capsule that opens the picker. Tapping a membership chip
//  opens that list; long-pressing offers to remove the tag from it.
//
//  The capsules wrap like text, which no stack view does, so the row lays its
//  own subviews out and reports the resulting height through
//  `intrinsicContentSize` / `sizeThatFits:` — enough for the summary's existing
//  vertical stack to size it without any change to that stack.
//

import Foundation
import UIKit

/// A capsule that remembers which list it stands for.
final class TMListChipButton: UIButton {
    var listKey: String?
}

@objc(TMListChipsView)
@objcMembers
public final class TMListChipsView: UIView {
    /// The tag whose memberships are shown. 0 until the tag has loaded.
    public var tagId: Int32 = 0 {
        didSet { if tagId != oldValue { reload() } }
    }

    /// The screen the chips navigate and present from.
    public weak var host: UIViewController?

    private var chips: [TMListChipButton] = []
    private let interitemSpacing: CGFloat = 8
    private let lineSpacing: CGFloat = 8
    private var reportedHeight: CGFloat = 0

    public override init(frame: CGRect) {
        super.init(frame: frame)
        commonInit()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        commonInit()
    }

    private func commonInit() {
        isAccessibilityElement = false
        accessibilityContainerType = .semanticGroup
        accessibilityLabel = "In lists"
        accessibilityIdentifier = "summary.chips"
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(reload),
                                               name: .userDataChanged,
                                               object: nil)
        // The capsules are placed by hand, so a text-size change that resizes
        // them reaches no Auto Layout pass on its own: re-resolve and re-measure.
        registerForTraitChanges([UITraitPreferredContentSizeCategory.self]) { (view: TMListChipsView, _) in
            view.applyChipFont()
            view.setNeedsLayout()
            view.invalidateIntrinsicContentSize()
        }
        reload()
    }

    public override func didMoveToWindow() {
        super.didMoveToWindow()
        // A capsule built outside any window measured against the default text
        // size; the window it lands in may be set to another.
        applyChipFont()
        setNeedsLayout()
        invalidateIntrinsicContentSize()
    }

    /// Dynamic Type, resolved against this row's own traits rather than the
    /// process-wide category, so the measured height matches what is drawn.
    private func applyChipFont() {
        let font = UIFont.preferredFont(forTextStyle: .subheadline, compatibleWith: traitCollection)
        for chip in chips {
            guard var configuration = chip.configuration else { continue }
            configuration.titleTextAttributesTransformer = UIConfigurationTextAttributesTransformer { incoming in
                var outgoing = incoming
                outgoing.font = font
                return outgoing
            }
            chip.configuration = configuration
        }
    }

    // MARK: - Contents

    /// Rebuilds the row from the registry. Safe to call at any time.
    public func reload() {
        chips.forEach { $0.removeFromSuperview() }
        chips = []
        if tagId > 0 {
            for key in TMTagLists.keysContaining(Int(tagId)) {
                chips.append(makeMembershipChip(for: key))
            }
            chips.append(makeAddChip())
        }
        chips.forEach(addSubview)
        applyChipFont()
        isHidden = chips.isEmpty
        setNeedsLayout()
        invalidateIntrinsicContentSize()
    }

    private static func symbolName(for key: String) -> String {
        switch key {
        case TMTagLists.favoriteKey: return "heart.fill"
        case TMTagLists.teachableKey: return "person.2.fill"
        default: return "list.bullet"
        }
    }

    private func makeChip(title: String, symbol: String, filled: Bool) -> TMListChipButton {
        let chip = TMListChipButton(type: .system)
        var configuration = filled ? UIButton.Configuration.tinted() : UIButton.Configuration.plain()
        configuration.title = title
        configuration.image = UIImage(systemName: symbol)
        configuration.imagePadding = 6
        configuration.cornerStyle = .capsule
        configuration.contentInsets = NSDirectionalEdgeInsets(top: 8, leading: 14, bottom: 8, trailing: 14)
        configuration.baseForegroundColor = DPAppDelegate.accentColor()
        configuration.baseBackgroundColor = DPAppDelegate.accentColor()
        configuration.titleLineBreakMode = .byTruncatingTail
        if !filled {
            // The assist chip reads as an outline so it is never mistaken for membership.
            configuration.background.strokeColor = DPAppDelegate.accentColor()
            configuration.background.strokeWidth = 1
        }
        chip.configuration = configuration
        chip.titleLabel?.numberOfLines = 1
        return chip
    }

    private func makeMembershipChip(for key: String) -> TMListChipButton {
        let name = TMTagLists.name(for: key)
        let chip = makeChip(title: name, symbol: TMListChipsView.symbolName(for: key), filled: true)
        chip.listKey = key
        chip.accessibilityIdentifier = "summary.chip.\(key)"
        chip.accessibilityLabel = name
        chip.accessibilityHint = "Opens \(name)"
        chip.accessibilityCustomActions = [
            UIAccessibilityCustomAction(name: "Remove from \(name)") { [weak self] _ in
                self?.remove(from: key)
                return true
            }
        ]
        chip.addAction(UIAction { [weak self] _ in self?.open(key) }, for: .touchUpInside)
        chip.addInteraction(UIContextMenuInteraction(delegate: self))
        return chip
    }

    private func makeAddChip() -> TMListChipButton {
        let chip = makeChip(title: "Add to list", symbol: "plus", filled: false)
        chip.accessibilityIdentifier = "summary.chip.add"
        chip.accessibilityLabel = "Add to list"
        chip.addAction(UIAction { [weak self] _ in self?.showPicker(from: chip) }, for: .touchUpInside)
        return chip
    }

    // MARK: - Actions

    private func open(_ key: String) {
        guard let host else { return }
        DPAppDelegate.showList(withKey: key, from: host)
    }

    private func remove(from key: String) {
        guard tagId > 0 else { return }
        let name = TMTagLists.name(for: key)
        TMTagLists.remove(Int(tagId), from: key)
        UIAccessibility.post(notification: .announcement, argument: "Removed from \(name)")
    }

    private func showPicker(from chip: UIView) {
        guard let host, tagId > 0 else { return }
        TMListPickerController.present(forTagId: tagId, from: host, barButtonItem: nil, sourceView: chip)
    }

    // MARK: - Wrapping layout

    public override func layoutSubviews() {
        super.layoutSubviews()
        let height = arrange(width: bounds.width, apply: true)
        if abs(height - reportedHeight) > 0.5 {
            reportedHeight = height
            invalidateIntrinsicContentSize()
        }
    }

    public override var intrinsicContentSize: CGSize {
        let width = bounds.width > 0 ? bounds.width : UIScreen.main.bounds.width
        return CGSize(width: UIView.noIntrinsicMetric, height: arrange(width: width, apply: false))
    }

    public override func sizeThatFits(_ size: CGSize) -> CGSize {
        let width = size.width > 0 && size.width < .greatestFiniteMagnitude ? size.width : UIScreen.main.bounds.width
        return CGSize(width: width, height: arrange(width: width, apply: false))
    }

    /// Flows the capsules into as many rows as `width` needs, returning the
    /// total height. With `apply` it also places them.
    @discardableResult
    private func arrange(width: CGFloat, apply: Bool) -> CGFloat {
        guard !chips.isEmpty else { return 0 }
        let available = max(width, 1)
        var x: CGFloat = 0
        var y: CGFloat = 0
        var lineHeight: CGFloat = 0
        for chip in chips {
            var size = chip.sizeThatFits(CGSize(width: available, height: .greatestFiniteMagnitude))
            // A long list name wraps the row rather than the capsule, and never
            // overflows it; the title itself truncates at the capsule's edge.
            size.width = min(size.width, available)
            // Every chip stays a full touch target, whatever its text size.
            size.height = max(44, size.height)
            if x > 0 && x + size.width > available + 0.5 {
                x = 0
                y += lineHeight + lineSpacing
                lineHeight = 0
            }
            if apply { chip.frame = CGRect(x: x, y: y, width: size.width, height: size.height) }
            x += size.width + interitemSpacing
            lineHeight = max(lineHeight, size.height)
        }
        return y + lineHeight
    }
}

// MARK: - Remove by long press

extension TMListChipsView: UIContextMenuInteractionDelegate {
    public func contextMenuInteraction(_ interaction: UIContextMenuInteraction,
                                       configurationForMenuAtLocation location: CGPoint) -> UIContextMenuConfiguration? {
        guard let key = (interaction.view as? TMListChipButton)?.listKey else { return nil }
        let name = TMTagLists.name(for: key)
        return UIContextMenuConfiguration(identifier: nil, previewProvider: nil) { [weak self] _ in
            UIMenu(children: [
                UIAction(title: "Remove from \(name)",
                         image: UIImage(systemName: "minus.circle"),
                         attributes: .destructive) { _ in self?.remove(from: key) }
            ])
        }
    }
}
