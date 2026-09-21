//
//  TMListChipsView.swift
//  tagmaster
//
//  The "In lists" row under a tag's title: one capsule per list the tag belongs
//  to, then an assist capsule that opens the picker. The name half of a
//  membership capsule opens that list; the visible remove button at its trailing
//  end takes the tag out of it, undoably.
//
//  The capsules wrap like text, which no stack view does, so the row lays its
//  own subviews out and reports the resulting height through
//  `intrinsicContentSize` / `sizeThatFits:` — enough for the summary's existing
//  vertical stack to size it without any change to that stack.
//

import Foundation
import UIKit

/// One capsule: an outlined neutral pill holding the list's name, and — for a
/// list the tag is actually in — a remove button of its own.
///
/// The two controls are separate accessibility elements, so VoiceOver offers
/// "open this list" and "remove from this list" as the two distinct things they
/// are, instead of hiding the second one behind a rotor action.
final class TMListChipView: UIView {
    /// The list this capsule stands for; nil on the trailing assist capsule.
    let listKey: String?
    let nameButton: UIButton
    let removeButton: UIButton?

    /// The width the remove button is given, which is also its touch target.
    static let removeWidth: CGFloat = 44
    /// The shortest a capsule ever is, so every one of them is a full target.
    static let minimumHeight: CGFloat = 44

    var title: String { nameButton.configuration?.title ?? "" }

    init(listKey: String?, nameButton: UIButton, removeButton: UIButton?) {
        self.listKey = listKey
        self.nameButton = nameButton
        self.removeButton = removeButton
        super.init(frame: .zero)
        isAccessibilityElement = false
        addSubview(nameButton)
        if let removeButton { addSubview(removeButton) }
        layer.borderWidth = 1
        layer.cornerCurve = .continuous
        applyBorderColor()
        // A CGColor does not follow light/dark the way a UIColor does.
        registerForTraitChanges([UITraitUserInterfaceStyle.self]) { (chip: TMListChipView, _) in
            chip.applyBorderColor()
        }
    }

    required init?(coder: NSCoder) {
        fatalError("TMListChipView is created in code")
    }

    private func applyBorderColor() {
        layer.borderColor = UIColor.separator.resolvedColor(with: traitCollection).cgColor
    }

    override func sizeThatFits(_ size: CGSize) -> CGSize {
        let removal = removeButton == nil ? 0 : TMListChipView.removeWidth
        let room = max(size.width - removal, 1)
        let name = nameButton.sizeThatFits(CGSize(width: room, height: .greatestFiniteMagnitude))
        return CGSize(width: min(name.width + removal, max(size.width, 1)),
                      height: max(TMListChipView.minimumHeight, name.height))
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        layer.cornerRadius = bounds.height / 2
        let removal = removeButton == nil ? 0 : TMListChipView.removeWidth
        nameButton.frame = CGRect(x: 0, y: 0, width: max(bounds.width - removal, 0), height: bounds.height)
        removeButton?.frame = CGRect(x: bounds.width - removal, y: 0, width: removal, height: bounds.height)
    }
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

    /// The screen whose undo manager a removal is registered with, so a shake
    /// puts the tag back. The summary controller sets it to itself.
    public weak var undoHost: UIViewController?

    private var chips: [TMListChipView] = []
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
            guard var configuration = chip.nameButton.configuration else { continue }
            configuration.titleTextAttributesTransformer = UIConfigurationTextAttributesTransformer { incoming in
                var outgoing = incoming
                outgoing.font = font
                return outgoing
            }
            chip.nameButton.configuration = configuration
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

    /// The name half of a capsule: icon and text, no fill, leaving the outline
    /// to the capsule itself.
    private func makeNameButton(title: String, symbol: String, accent: Bool, room: Bool) -> UIButton {
        let button = UIButton(type: .system)
        var configuration = UIButton.Configuration.plain()
        configuration.title = title
        configuration.image = UIImage(systemName: symbol)
        // Sized off the same text style as the title, so icon and name grow together.
        configuration.preferredSymbolConfigurationForImage = UIImage.SymbolConfiguration(textStyle: .subheadline)
        configuration.imagePadding = 6
        configuration.contentInsets = NSDirectionalEdgeInsets(top: 8,
                                                              leading: 14,
                                                              bottom: 8,
                                                              trailing: room ? 4 : 14)
        // Neutral by default — a membership capsule marks membership by being
        // there, not by shouting. Only the assist capsule takes the accent.
        let accentColor = DPAppDelegate.accentColor() ?? .tintColor
        let foreground: UIColor = accent ? accentColor : .label
        let iconColor: UIColor = accent ? accentColor : .secondaryLabel
        configuration.baseForegroundColor = foreground
        configuration.imageColorTransformer = UIConfigurationColorTransformer { _ in iconColor }
        configuration.titleLineBreakMode = .byTruncatingTail
        configuration.background.backgroundColor = .clear
        button.configuration = configuration
        button.titleLabel?.numberOfLines = 1
        return button
    }

    private func makeRemoveButton(for key: String, listName: String) -> UIButton {
        let button = UIButton(type: .system)
        var configuration = UIButton.Configuration.plain()
        configuration.image = UIImage(systemName: "xmark.circle.fill")
        // Plainly visible, but never louder than the name beside it.
        configuration.preferredSymbolConfigurationForImage = UIImage.SymbolConfiguration(textStyle: .subheadline)
        configuration.contentInsets = .zero
        configuration.baseForegroundColor = .secondaryLabel
        configuration.background.backgroundColor = .clear
        button.configuration = configuration
        button.accessibilityIdentifier = "summary.chip.\(key).remove"
        button.accessibilityLabel = "Remove from \(listName)"
        button.addAction(UIAction { [weak self] _ in self?.remove(from: key) }, for: .touchUpInside)
        return button
    }

    private func makeMembershipChip(for key: String) -> TMListChipView {
        let name = TMTagLists.name(for: key)
        let nameButton = makeNameButton(title: name,
                                        symbol: TMListChipsView.symbolName(for: key),
                                        accent: false,
                                        room: true)
        nameButton.accessibilityIdentifier = "summary.chip.\(key)"
        nameButton.accessibilityLabel = name
        nameButton.accessibilityHint = "Opens \(name)"
        nameButton.accessibilityCustomActions = [
            UIAccessibilityCustomAction(name: "Remove from \(name)") { [weak self] _ in
                self?.remove(from: key)
                return true
            }
        ]
        nameButton.addAction(UIAction { [weak self] _ in self?.open(key) }, for: .touchUpInside)
        let chip = TMListChipView(listKey: key,
                                  nameButton: nameButton,
                                  removeButton: makeRemoveButton(for: key, listName: name))
        chip.addInteraction(UIContextMenuInteraction(delegate: self))
        return chip
    }

    private func makeAddChip() -> TMListChipView {
        let nameButton = makeNameButton(title: "Add to list", symbol: "plus", accent: true, room: false)
        nameButton.accessibilityIdentifier = "summary.chip.add"
        nameButton.accessibilityLabel = "Add to list"
        let chip = TMListChipView(listKey: nil, nameButton: nameButton, removeButton: nil)
        nameButton.addAction(UIAction { [weak self] _ in self?.showPicker(from: chip) }, for: .touchUpInside)
        return chip
    }

    // MARK: - Actions

    private func open(_ key: String) {
        guard let host else { return }
        DPAppDelegate.showList(withKey: key, from: host)
    }

    /// The undo manager a removal registers with: the hosting screen's, so a
    /// shake anywhere on that screen puts the tag back.
    private var chipUndoManager: UndoManager? {
        undoHost?.undoManager ?? host?.undoManager ?? undoManager
    }

    private func remove(from key: String) {
        guard tagId > 0 else { return }
        let name = TMTagLists.name(for: key)
        let before = TMTagLists.ids(for: key)
        guard before.contains(Int(tagId)) else { return }
        TMTagLists.remove(Int(tagId), from: key)
        if let manager = chipUndoManager {
            TMListChipsView.registerRemoval(of: Int(tagId), from: key, restoring: before, named: name, with: manager)
        }
        UIAccessibility.post(notification: .announcement, argument: "Removed from \(name)")
    }

    /// Records the removal so undo restores the list exactly as it was — the tag
    /// back at the position it held — and redo takes it out again.
    ///
    /// The manager is its own undo target: nothing here needs the chips view,
    /// which may well be gone by the time the user shakes.
    private static func registerRemoval(of tagId: Int,
                                        from key: String,
                                        restoring previous: [Int],
                                        named name: String,
                                        with manager: UndoManager) {
        manager.setActionName("Remove from \(name)")
        manager.registerUndo(withTarget: manager) { target in
            TMTagLists.setIds(previous, for: key)
            UIAccessibility.post(notification: .announcement, argument: "Added to \(name)")
            registerRemoval(of: tagId, from: key, restoring: previous, named: name, with: target)
        }
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
            // A long list name wraps the row rather than the capsule, and never
            // overflows it; the title itself truncates at the capsule's edge.
            let size = chip.sizeThatFits(CGSize(width: available, height: .greatestFiniteMagnitude))
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
        guard let key = (interaction.view as? TMListChipView)?.listKey else { return nil }
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
