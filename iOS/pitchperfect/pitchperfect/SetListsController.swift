//
//  SetListsController.swift
//  pitchperfect
//
//  "Set Lists": the manage screen pushed onto the Songs navigation stack.
//  My Songs is the first row and never moves or leaves; every custom row
//  carries a drag handle, renames on tap, and duplicates or deletes by swipe.
//  Rows are transparent over the score, separated by hairlines, as everywhere.
//

import Foundation
import UIKit

/// One manage row: the list's display name and how many songs it holds.
final class SetListRowCell: UITableViewCell {
    static let identifier = "SetListRow"

    private let nameLabel = UILabel()
    private let countLabel = UILabel()

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

        contentView.addSubview(nameLabel)
        contentView.addSubview(countLabel)
        NSLayoutConstraint.activate([
            nameLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 20),
            nameLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 14),
            nameLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -14),
            countLabel.leadingAnchor.constraint(greaterThanOrEqualTo: nameLabel.trailingAnchor, constant: 16),
            countLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -20),
            countLabel.centerYAnchor.constraint(equalTo: nameLabel.centerYAnchor),
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    func show(name: String, songCount: Int) {
        nameLabel.text = name
        countLabel.text = SetListsController.countLabel(songCount)
        accessibilityLabel = "\(name), \(SetListSelectorView.songCountPhrase(songCount))"
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
            row.show(name: model.displayName(for: list), songCount: list.songs.count)
            row.accessibilityIdentifier = "setlist.row.\(list.id)"
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

    override public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)
        guard let list = list(at: indexPath) else { return }
        promptRename(list)
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
        return UISwipeActionsConfiguration(actions: [delete, duplicate])
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
