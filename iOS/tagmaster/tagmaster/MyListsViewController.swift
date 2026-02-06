//
//  MyListsViewController.swift
//  tagmaster
//
//  Created by David Poll on 2025.
//  Copyright © 2025 DepollSoft. All rights reserved.
//

import UIKit

@objc class MyListsViewController: UITableViewController {

    private var lists: [ListMetadata] = []

    override init(style: UITableView.Style) {
        super.init(style: .plain)
    }

    convenience init() {
        self.init(style: .plain)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        DPAppDelegate.setUpBackground(view)
        navigationItem.title = "My Lists"
        navigationItem.backBarButtonItem = UIBarButtonItem()
        navigationItem.backBarButtonItem?.title = "Lists"

        navigationItem.rightBarButtonItem = UIBarButtonItem(
            barButtonSystemItem: .add,
            target: self,
            action: #selector(createList)
        )

        NotificationCenter.default.addObserver(self,
            selector: #selector(onDataChanged),
            name: .userDataChanged,
            object: nil)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        refreshLists()
    }

    @objc private func onDataChanged() {
        refreshLists()
    }

    private func refreshLists() {
        lists = CustomListsModel.customLists()
        tableView.reloadData()
    }

    // MARK: - Table View

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if lists.isEmpty {
            let label = UILabel()
            label.text = "No custom lists yet.\nTap + to create one."
            label.textAlignment = .center
            label.numberOfLines = 0
            label.textColor = .secondaryLabel
            tableView.backgroundView = label
        } else {
            tableView.backgroundView = nil
        }
        return lists.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "ListCell")
            ?? UITableViewCell(style: .subtitle, reuseIdentifier: "ListCell")
        let meta = lists[indexPath.row]
        let count = ListModel.get(meta.key).ids.count
        cell.textLabel?.text = meta.name
        cell.detailTextLabel?.text = "\(count) tags"
        cell.accessoryType = .disclosureIndicator
        cell.backgroundColor = .clear
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let meta = lists[indexPath.row]
        let vc = CustomListViewController(listKey: meta.key)
        navigationController?.pushViewController(vc, animated: true)
    }

    // MARK: - Editing

    override func tableView(_ tableView: UITableView, commit editingStyle: UITableViewCell.EditingStyle, forRowAt indexPath: IndexPath) {
        if editingStyle == .delete {
            let meta = lists[indexPath.row]
            let alert = UIAlertController(title: "Delete List", message: "Delete \"\(meta.name)\"?", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
            alert.addAction(UIAlertAction(title: "Delete", style: .destructive) { _ in
                CustomListsModel.deleteList(meta.key)
                self.refreshLists()
            })
            present(alert, animated: true)
        }
    }

    override func tableView(_ tableView: UITableView, contextMenuConfigurationForRowAt indexPath: IndexPath, point: CGPoint) -> UIContextMenuConfiguration? {
        let meta = lists[indexPath.row]
        return UIContextMenuConfiguration(identifier: nil, previewProvider: nil) { _ in
            let rename = UIAction(title: "Rename", image: UIImage(systemName: "pencil")) { _ in
                self.showRenameDialog(meta)
            }
            let delete = UIAction(title: "Delete", image: UIImage(systemName: "trash"), attributes: .destructive) { _ in
                let alert = UIAlertController(title: "Delete List", message: "Delete \"\(meta.name)\"?", preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
                alert.addAction(UIAlertAction(title: "Delete", style: .destructive) { _ in
                    CustomListsModel.deleteList(meta.key)
                    self.refreshLists()
                })
                self.present(alert, animated: true)
            }
            return UIMenu(title: "", children: [rename, delete])
        }
    }

    // MARK: - Actions

    @objc private func createList() {
        let alert = UIAlertController(title: "Create New List", message: nil, preferredStyle: .alert)
        alert.addTextField { $0.placeholder = "List name" }
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        alert.addAction(UIAlertAction(title: "Create", style: .default) { _ in
            let name = alert.textFields?[0].text?.trimmingCharacters(in: .whitespaces) ?? ""
            if !name.isEmpty {
                CustomListsModel.createList(name)
                self.refreshLists()
            }
        })
        present(alert, animated: true)
    }

    private func showRenameDialog(_ meta: ListMetadata) {
        let alert = UIAlertController(title: "Rename List", message: nil, preferredStyle: .alert)
        alert.addTextField { $0.text = meta.name }
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        alert.addAction(UIAlertAction(title: "Rename", style: .default) { _ in
            let name = alert.textFields?[0].text?.trimmingCharacters(in: .whitespaces) ?? ""
            if !name.isEmpty {
                CustomListsModel.renameList(meta.key, newName: name)
                self.refreshLists()
            }
        })
        present(alert, animated: true)
    }
}
