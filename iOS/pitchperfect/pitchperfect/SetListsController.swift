//
//  SetListsController.swift
//  pitchperfect
//
//  "Set Lists": the manage screen pushed onto the Songs navigation stack.
//  My Songs is the first row and never moves or leaves; every custom row
//  carries a drag handle and a swipe. Tapping a row switches to that list and
//  returns to Songs — the same thing tapping its position in the selector does
//  — so the list's own actions live in the row's trailing "…" menu instead.
//  Rows are transparent over the score, separated by hairlines, as everywhere.
//

import Foundation
import UIKit

/// One manage row: the list's display name, how many songs it holds, the
/// selector's lit indicator when it is the current list, and a "…" menu.
final class SetListRowCell: UITableViewCell {
    static let identifier = "SetListRow"
    /// The indicator, drawn exactly as the selector draws it.
    private static let dotDiameter: CGFloat = 6
    private static let dotInset: CGFloat = 8

    private let nameLabel = UILabel()
    private let countLabel = UILabel()
    private let currentDot = UIView()
    /// The row's own actions, shown on press; also what the tests read.
    let menuButton = UIButton(type: .system)

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        selectionStyle = .none
        DPTheme.styleListCell(self)

        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.font = DPTheme.listTitleFont(size: 20)
        nameLabel.textColor = DPTheme.plateInk
        nameLabel.numberOfLines = 1
        nameLabel.lineBreakMode = .byTruncatingTail

        countLabel.translatesAutoresizingMaskIntoConstraints = false
        countLabel.font = DPTheme.monospacedFont(size: 14)
        countLabel.textColor = DPTheme.plateInkSecondary
        countLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
        countLabel.setContentHuggingPriority(.required, for: .horizontal)

        currentDot.translatesAutoresizingMaskIntoConstraints = false
        currentDot.backgroundColor = DPTheme.plateLit
        currentDot.layer.cornerRadius = SetListRowCell.dotDiameter / 2
        currentDot.isUserInteractionEnabled = false
        currentDot.isHidden = true

        menuButton.translatesAutoresizingMaskIntoConstraints = false
        menuButton.setImage(UIImage(systemName: "ellipsis.circle"), for: .normal)
        menuButton.tintColor = DPTheme.plateInkSecondary
        menuButton.showsMenuAsPrimaryAction = true
        menuButton.accessibilityLabel = "More"
        menuButton.setContentCompressionResistancePriority(.required, for: .horizontal)

        contentView.addSubview(currentDot)
        contentView.addSubview(nameLabel)
        contentView.addSubview(countLabel)
        contentView.addSubview(menuButton)
        NSLayoutConstraint.activate([
            currentDot.leadingAnchor.constraint(equalTo: contentView.leadingAnchor,
                                                constant: SetListRowCell.dotInset),
            currentDot.centerYAnchor.constraint(equalTo: nameLabel.centerYAnchor),
            currentDot.widthAnchor.constraint(equalToConstant: SetListRowCell.dotDiameter),
            currentDot.heightAnchor.constraint(equalToConstant: SetListRowCell.dotDiameter),
            // Always clear of the dot, lit or not, so a name never shifts.
            nameLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 26),
            nameLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 14),
            nameLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -14),
            countLabel.leadingAnchor.constraint(greaterThanOrEqualTo: nameLabel.trailingAnchor, constant: 16),
            countLabel.centerYAnchor.constraint(equalTo: nameLabel.centerYAnchor),
            menuButton.leadingAnchor.constraint(equalTo: countLabel.trailingAnchor, constant: 8),
            menuButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -4),
            menuButton.centerYAnchor.constraint(equalTo: nameLabel.centerYAnchor),
            menuButton.widthAnchor.constraint(equalToConstant: 44),
            menuButton.heightAnchor.constraint(equalToConstant: 44),
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    func show(name: String, songCount: Int, isCurrent: Bool, menu: UIMenu, menuIdentifier: String) {
        nameLabel.text = name
        countLabel.text = SetListsController.countLabel(songCount)
        currentDot.isHidden = !isCurrent
        menuButton.menu = menu
        menuButton.accessibilityIdentifier = menuIdentifier
        menuButton.accessibilityLabel = "Actions for \(name)"
        accessibilityLabel = isCurrent
            ? "\(name), \(SetListSelectorView.songCountPhrase(songCount)), current"
            : "\(name), \(SetListSelectorView.songCountPhrase(songCount))"
        accessibilityTraits = isCurrent ? [.button, .selected] : .button
    }
}

@objc public class SetListsController: UITableViewController {
    private var lists: [DPSongList] = []
    private var committingMove = false

    @objc public init() {
        super.init(style: .plain)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    /// "12 songs", "1 song", "No songs".
    static func countLabel(_ count: Int) -> String {
        switch count {
        case 0: return "No songs"
        case 1: return "1 song"
        default: return "\(count) songs"
        }
    }

    override public func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.title = "Set Lists"
        tableView.register(SetListRowCell.self, forCellReuseIdentifier: SetListRowCell.identifier)
        tableView.backgroundColor = DPTheme.staffBackgroundColor()
        let background = UIView()
        background.backgroundColor = DPTheme.staffBackgroundColor()
        tableView.backgroundView = background
        tableView.separatorColor = DPTheme.plateHairline
        tableView.separatorInset = UIEdgeInsets(top: 0, left: 20, bottom: 0, right: 20)
        tableView.allowsSelectionDuringEditing = true
        tableView.setEditing(true, animated: false)
        navigationItem.rightBarButtonItem = DPCommon.barButton(
            systemName: "plus", target: self, selector: #selector(promptCreate)
        )
        NotificationCenter.default.addObserver(
            self, selector: #selector(listsChanged),
            name: DPSongsModel.songsChangedNotificationName, object: nil
        )
        reload()
    }

    private var model: DPSongsModel { DPSongsModel.sharedInstance }

    private func reload() {
        lists = model.orderedLists
        tableView.reloadData()
    }

    @objc private func listsChanged() {
        guard !committingMove else { return }
        reload()
    }

    // MARK: - Rows

    override public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        lists.count
    }

    override public func tableView(_ tableView: UITableView,
                                   cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: SetListRowCell.identifier, for: indexPath)
        DPTheme.styleListCell(cell)
        if let row = cell as? SetListRowCell, let list = list(at: indexPath) {
            // The reorder control is a drag; VoiceOver and Switch Control reorder
            // through named actions instead, one row at a time.
            row.accessibilityCustomActions = isHome(list) ? nil : moveActions(for: list)
            let current = list.id == model.currentListId
            row.show(name: model.displayName(for: list),
                     songCount: list.songs.count,
                     isCurrent: current,
                     menu: rowMenu(for: list),
                     menuIdentifier: "setlist.row.menu.\(list.id)")
            // The current row says so in its identifier as well as its traits.
            row.accessibilityIdentifier = current
                ? "setlist.row.\(list.id).current"
                : "setlist.row.\(list.id)"
        }
        return cell
    }

    private func list(at indexPath: IndexPath) -> DPSongList? {
        lists.indices.contains(indexPath.row) ? lists[indexPath.row] : nil
    }

    private func isHome(_ list: DPSongList) -> Bool {
        list.id == DPSongsModel.defaultListId
    }

    // MARK: - Reordering

    private var customLists: [DPSongList] { lists.filter { !isHome($0) } }

    private func moveActions(for list: DPSongList) -> [UIAccessibilityCustomAction] {
        var actions: [UIAccessibilityCustomAction] = []
        let index = customLists.firstIndex { $0 === list } ?? 0
        if index > 0 {
            actions.append(UIAccessibilityCustomAction(name: "Move up") { [weak self] _ in
                self?.move(list, by: -1) ?? false
            })
        }
        if index < customLists.count - 1 {
            actions.append(UIAccessibilityCustomAction(name: "Move down") { [weak self] _ in
                self?.move(list, by: 1) ?? false
            })
        }
        return actions
    }

    /// Moves `list` one step among the custom lists and commits at once.
    @discardableResult
    @objc public func move(_ list: DPSongList, by delta: Int) -> Bool {
        var custom = customLists
        guard let from = custom.firstIndex(where: { $0 === list }) else { return false }
        let to = from + delta
        guard custom.indices.contains(to) else { return false }
        custom.insert(custom.remove(at: from), at: to)
        model.reorderLists(custom.map(\.id))
        return true
    }

    override public func tableView(_ tableView: UITableView, canMoveRowAt indexPath: IndexPath) -> Bool {
        list(at: indexPath).map { !isHome($0) } ?? false
    }

    override public func tableView(_ tableView: UITableView,
                                   targetIndexPathForMoveFromRowAt source: IndexPath,
                                   toProposedIndexPath proposed: IndexPath) -> IndexPath {
        // My Songs is always first; nothing may take its place.
        proposed.row == 0 ? IndexPath(row: 1, section: 0) : proposed
    }

    override public func tableView(_ tableView: UITableView,
                                   moveRowAt source: IndexPath, to destination: IndexPath) {
        guard let moved = list(at: source) else { return }
        var reordered = lists
        reordered.remove(at: source.row)
        reordered.insert(moved, at: min(destination.row, reordered.count))
        lists = reordered
        committingMove = true
        // Committed once, on drop: `order` is rewritten for every custom list.
        model.reorderLists(reordered.map(\.id))
        committingMove = false
    }

    override public func tableView(_ tableView: UITableView,
                                   editingStyleForRowAt indexPath: IndexPath) -> UITableViewCell.EditingStyle {
        .none
    }

    override public func tableView(_ tableView: UITableView,
                                   shouldIndentWhileEditingRowAt indexPath: IndexPath) -> Bool {
        false
    }

    override public func tableView(_ tableView: UITableView, canEditRowAt indexPath: IndexPath) -> Bool {
        list(at: indexPath).map { !isHome($0) } ?? false
    }

    // MARK: - Row actions

    /// Tapping a row does what tapping its position does: it switches to that
    /// list and hands the Songs tab back. Renaming lives in the row's menu.
    override public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)
        guard let list = list(at: indexPath) else { return }
        model.currentListId = list.id
        navigationController?.popViewController(animated: true)
    }

    /// The actions a row offers, headed by the list they act on. Also what the
    /// trailing "…" button presents.
    @objc public func menu(forRowAt indexPath: IndexPath) -> UIMenu? {
        list(at: indexPath).map { rowMenu(for: $0) }
    }

    private func rowMenu(for list: DPSongList) -> UIMenu {
        var actions: [UIMenuElement] = [
            UIAction(title: "Rename set list…",
                     image: UIImage(systemName: "pencil")) { [weak self] _ in
                self?.promptRename(list)
            },
            UIAction(title: "Duplicate set list",
                     image: UIImage(systemName: "plus.square.on.square")) { [weak self] _ in
                self?.duplicateSetList(list)
            },
        ]
        if !isHome(list) {
            actions.append(UIAction(title: "Delete set list…",
                                    image: UIImage(systemName: "trash"),
                                    attributes: .destructive) { [weak self] _ in
                self?.confirmDelete(list)
            })
        }
        return UIMenu(title: model.displayName(for: list), children: actions)
    }

    override public func tableView(
        _ tableView: UITableView,
        trailingSwipeActionsConfigurationForRowAt indexPath: IndexPath
    ) -> UISwipeActionsConfiguration? {
        guard let list = list(at: indexPath), !isHome(list) else { return nil }
        let duplicate = UIContextualAction(style: .normal, title: "Duplicate") { [weak self] _, _, done in
            self?.duplicateSetList(list)
            done(true)
        }
        let delete = UIContextualAction(style: .destructive, title: "Delete") { [weak self] _, _, done in
            self?.confirmDelete(list)
            done(true)
        }
        let rename = UIContextualAction(style: .normal, title: "Rename") { [weak self] _, _, done in
            self?.promptRename(list)
            done(true)
        }
        return UISwipeActionsConfiguration(actions: [delete, duplicate, rename])
    }

    // MARK: - Commands

    @objc public func promptCreate() {
        present(SetListPrompts.createAlert { [weak self] name in
            guard let self, let created = self.model.createList(named: name) else { return }
            self.model.currentListId = created.id
            self.reload()
        }, animated: true)
    }

    @objc public func promptRename(_ list: DPSongList) {
        present(SetListPrompts.renameAlert(for: list) { [weak self] name in
            self?.model.renameList(list, to: name)
            self?.reload()
        }, animated: true)
    }

    @objc public func duplicateSetList(_ list: DPSongList) {
        guard let copy = model.duplicateList(list) else { return }
        reload()
        UIAccessibility.post(notification: .announcement,
                             argument: "Duplicated as \(model.displayName(for: copy))")
    }

    @objc public func confirmDelete(_ list: DPSongList) {
        present(SetListPrompts.deleteAlert(for: list) { [weak self] in
            self?.model.deleteList(list)
            self?.reload()
        }, animated: true)
    }
}
