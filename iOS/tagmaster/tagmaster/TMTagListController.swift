//
//  TMTagListController.swift
//  tagmaster
//
//  One user-created list: the same screen Teachable Tags has always been, over
//  an arbitrary list key. Edit reorders and removes; the overflow menu renames
//  and deletes the list itself. A list deleted on another device takes its
//  screen with it rather than leaving a stale title over an empty table.
//

import Foundation
import UIKit

@objc(TMTagListController)
public class TMTagListController: UITableViewController, TMTagListSource {
    @objc public private(set) var listKey: String
    private var emptyHeader: UIView?
    private weak var emptyHeading: UILabel?

    @objc public init(listKey: String) {
        self.listKey = listKey
        super.init(style: .plain)
    }

    required init?(coder: NSCoder) {
        fatalError("TMTagListController is created in code")
    }

    private var tagIds: [Int] { TMTagLists.ids(for: listKey) }

    // MARK: - Lifecycle

    public override func viewDidLoad() {
        super.viewDidLoad()
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(splitSelectionChanged),
                                               name: .TMTagSelectionDidChange,
                                               object: nil)
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(onUserDataChanged),
                                               name: .userDataChanged,
                                               object: nil)
        DPAppDelegate.setUpBackground(view)
        tableView.register(DPTagCell.self, forCellReuseIdentifier: "Tag")
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 100
        tableView.accessibilityIdentifier = "list.table"

        navigationItem.largeTitleDisplayMode = .never
        navigationItem.backBarButtonItem = UIBarButtonItem()
        navigationItem.backBarButtonItem?.title = "List"
        let overflow = UIBarButtonItem(image: UIImage(systemName: "ellipsis.circle"), menu: nil)
        overflow.accessibilityIdentifier = "list.menu"
        overflow.accessibilityLabel = "List options"
        navigationItem.rightBarButtonItems = [editButtonItem, overflow]
        refreshTitleAndMenu()
        // The row for the tag open beside this list stays selected instead of
        // clearing when the screen reappears in an expanded split.
        clearsSelectionOnViewWillAppear = !(splitViewController != nil && splitViewController?.isCollapsed == false)
    }

    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        tm_syncSelectionForSplit()
    }

    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        tableView.reloadData()
        tm_syncSelectionForSplit()
    }

    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        tm_syncSelectionForSplit()
        guard let header = tableView.tableHeaderView else { return }
        let width = tableView.bounds.width
        guard width > 0 else { return }
        let fit = header.systemLayoutSizeFitting(CGSize(width: width, height: 0),
                                                 withHorizontalFittingPriority: .required,
                                                 verticalFittingPriority: .fittingSizeLevel)
        if abs(header.bounds.width - width) > 0.5 || abs(header.bounds.height - ceil(fit.height)) > 0.5 {
            header.frame = CGRect(x: 0, y: 0, width: width, height: ceil(fit.height))
            tableView.tableHeaderView = header
        }
    }

    // MARK: - Reacting to changes

    @objc private func onUserDataChanged() {
        guard isViewLoaded else { return }
        // Gone from another device (or just deleted here): this screen has nothing left to show.
        guard TMTagLists.customKeys().contains(listKey) else {
            popSelf()
            return
        }
        refreshTitleAndMenu()
        // Reloading mid-drag would cancel the gesture and snap the row back.
        guard !tableView.hasUncommittedUpdates && !tableView.isDragging else { return }
        tableView.reloadData()
        tm_syncSelectionForSplit()
    }

    private func popSelf() {
        guard let navigation = navigationController, navigation.viewControllers.contains(self) else { return }
        navigation.popViewController(animated: !UIAccessibility.isReduceMotionEnabled)
    }

    private func refreshTitleAndMenu() {
        let name = TMTagLists.name(for: listKey)
        navigationItem.title = name
        let overflow = navigationItem.rightBarButtonItems?.last
        overflow?.menu = UIMenu(children: [
            UIAction(title: "Rename list…", image: UIImage(systemName: "pencil")) { [weak self] _ in
                self?.promptRename()
            },
            UIAction(title: "Delete list…", image: UIImage(systemName: "trash"), attributes: .destructive) { [weak self] _ in
                self?.confirmDelete()
            }
        ])
    }

    @objc public func promptRename() {
        let key = listKey
        present(TMListNamePrompt.renameAlert(for: key) { name in
            TMTagLists.renameList(key, to: name)
        }, animated: true)
    }

    @objc public func confirmDelete() {
        let key = listKey
        present(TMListDeletePrompt.alert(for: key) { [weak self] in
            TMTagLists.deleteList(key)
            self?.popSelf()
        }, animated: true)
    }

    // MARK: - Empty state

    private func makeEmptyHeader() -> UIView {
        // The empty state names this list, so a rename rewrites it in place.
        let title = "Nothing in \(TMTagLists.name(for: listKey)) yet."
        if let emptyHeader {
            emptyHeading?.text = title
            return emptyHeader
        }
        let heading = UILabel()
        heading.text = title
        let titleFont = UIFont.preferredFont(forTextStyle: .title2)
        heading.font = UIFont(descriptor: titleFont.fontDescriptor.withSymbolicTraits(.traitBold) ?? titleFont.fontDescriptor,
                              size: 0)
        heading.accessibilityTraits.insert(.header)
        heading.accessibilityIdentifier = "list.empty.title"
        let guidance = UILabel()
        guidance.text = "Open a tag and choose Add to list to build this list."
        guidance.font = .preferredFont(forTextStyle: .body)
        guidance.textColor = .secondaryLabel
        for label in [heading, guidance] {
            label.numberOfLines = 0
            label.adjustsFontForContentSizeCategory = true
            label.textAlignment = .center
        }
        let browse = UIButton(type: .system)
        browse.setTitle("Browse Tags", for: .normal)
        browse.titleLabel?.font = .preferredFont(forTextStyle: .body)
        browse.titleLabel?.adjustsFontForContentSizeCategory = true
        browse.titleLabel?.numberOfLines = 0
        browse.titleLabel?.textAlignment = .center
        browse.accessibilityIdentifier = "list.browse"
        browse.heightAnchor.constraint(greaterThanOrEqualToConstant: 44).isActive = true
        browse.addAction(UIAction { [weak self] _ in self?.browseTags() }, for: .touchUpInside)
        let stack = UIStackView(arrangedSubviews: [heading, guidance, browse])
        stack.axis = .vertical
        stack.spacing = 16
        stack.translatesAutoresizingMaskIntoConstraints = false
        let header = UIView()
        header.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: header.leadingAnchor, constant: 32),
            stack.trailingAnchor.constraint(equalTo: header.trailingAnchor, constant: -32),
            stack.topAnchor.constraint(equalTo: header.topAnchor, constant: 32),
            stack.bottomAnchor.constraint(equalTo: header.bottomAnchor, constant: -32)
        ])
        emptyHeader = header
        emptyHeading = heading
        return header
    }

    @objc private func browseTags() {
        navigationController?.pushViewController(DPBrowseViewController(), animated: true)
    }

    // MARK: - Table view

    public override func numberOfSections(in tableView: UITableView) -> Int { 1 }

    public override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        let count = tagIds.count
        if count == 0 {
            let header = makeEmptyHeader()
            if tableView.tableHeaderView !== header {
                tableView.tableHeaderView = header
                view.setNeedsLayout()
            }
        } else if tableView.tableHeaderView != nil {
            tableView.tableHeaderView = nil
            view.setNeedsLayout()
        }
        editButtonItem.isEnabled = count > 0
        return count
    }

    public override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "Tag", for: indexPath) as! DPTagCell
        let ids = tagIds
        if indexPath.row < ids.count { cell.tagId = Int32(ids[indexPath.row]) }
        let expanded = splitViewController != nil && splitViewController?.isCollapsed == false
        cell.accessoryType = expanded ? .none : .disclosureIndicator
        return cell
    }

    public override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        UITableView.automaticDimension
    }

    public override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let expanded = splitViewController != nil && splitViewController?.isCollapsed == false
        if !expanded { tableView.deselectRow(at: indexPath, animated: true) }
        let ids = tagIds
        guard indexPath.row < ids.count else { return }
        DPAppDelegate.showTag(withId: Int32(ids[indexPath.row]), from: self)
    }

    public override func tableView(_ tableView: UITableView, canEditRowAt indexPath: IndexPath) -> Bool { true }

    public override func tableView(_ tableView: UITableView, canMoveRowAt indexPath: IndexPath) -> Bool { true }

    public override func tableView(_ tableView: UITableView,
                                   commit editingStyle: UITableViewCell.EditingStyle,
                                   forRowAt indexPath: IndexPath) {
        guard editingStyle == .delete else { return }
        let ids = tagIds
        guard indexPath.row < ids.count else { return }
        TMTagLists.remove(ids[indexPath.row], from: listKey)
        tableView.reloadData()
    }

    public override func tableView(_ tableView: UITableView,
                                   moveRowAt sourceIndexPath: IndexPath,
                                   to destinationIndexPath: IndexPath) {
        TMTagLists.move(in: listKey, from: sourceIndexPath.row, to: destinationIndexPath.row)
    }

    // MARK: - TMTagListSource

    public func tm_listedTagIds() -> [NSNumber] {
        tagIds.map { NSNumber(value: $0) }
    }

    public func tm_didStep(toTagId tagId: Int32) {
        guard let index = tagIds.firstIndex(of: Int(tagId)) else { return }
        let path = IndexPath(row: index, section: 0)
        let animated = !UIAccessibility.isReduceMotionEnabled
        tableView.selectRow(at: path, animated: animated, scrollPosition: .none)
        tableView.scrollToRow(at: path, at: .none, animated: animated)
    }

    @objc private func splitSelectionChanged() {
        tm_syncSelectionForSplit()
    }

    /// Re-selects the row for the tag currently open in an expanded split, or
    /// deselects when collapsed.
    @objc public func tm_syncSelectionForSplit() {
        guard isViewLoaded else { return }
        let expanded = splitViewController != nil && splitViewController?.isCollapsed == false
        clearsSelectionOnViewWillAppear = !expanded
        for cell in tableView.visibleCells where cell is DPTagCell {
            let accessory: UITableViewCell.AccessoryType = expanded ? .none : .disclosureIndicator
            if cell.accessoryType != accessory { cell.accessoryType = accessory }
        }
        let current = expanded ? DPAppDelegate.currentSplitTagId(for: self) : nil
        let index = current.flatMap { tagIds.firstIndex(of: $0.intValue) }
        let path = index.map { IndexPath(row: $0, section: 0) }
        let selected = tableView.indexPathForSelectedRow
        if let selected, selected != path { tableView.deselectRow(at: selected, animated: false) }
        if let path, selected != path { tableView.selectRow(at: path, animated: false, scrollPosition: .none) }
    }
}
