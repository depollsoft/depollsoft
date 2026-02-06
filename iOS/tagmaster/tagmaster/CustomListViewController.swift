//
//  CustomListViewController.swift
//  tagmaster
//
//  Created by David Poll on 2025.
//  Copyright © 2025 DepollSoft. All rights reserved.
//

import UIKit

@objc class CustomListViewController: UITableViewController {

    let listKey: String
    private var displayName: String

    init(listKey: String) {
        self.listKey = listKey
        self.displayName = CustomListsModel.getMetadata(listKey)?.name ?? listKey
        super.init(style: .plain)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        DPAppDelegate.setUpBackground(view)
        tableView.register(DPTagCell.self, forCellReuseIdentifier: "Tag")
        navigationItem.title = displayName
        navigationItem.rightBarButtonItem = editButtonItem

        NotificationCenter.default.addObserver(self,
            selector: #selector(onDataChanged),
            name: .userDataChanged,
            object: nil)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        tableView.reloadData()
    }

    @objc private func onDataChanged() {
        tableView.reloadData()
    }

    private var tagIds: [Int] {
        return ListModel.get(listKey).ids
    }

    // MARK: - Table View

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        let count = tagIds.count
        if count == 0 {
            let label = UILabel()
            label.text = "No tags in this list."
            label.textAlignment = .center
            label.textColor = .secondaryLabel
            tableView.backgroundView = label
        } else {
            tableView.backgroundView = nil
        }
        return count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let tagId = Int32(tagIds[indexPath.row])
        let tagCell = tableView.dequeueReusableCell(withIdentifier: "Tag", for: indexPath) as! DPTagCell
        tagCell.tagId = tagId
        return tagCell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let tagId = Int32(tagIds[indexPath.row])
        let tvc = DPTagViewController()
        tvc.tagId = tagId
        navigationController?.pushViewController(tvc, animated: true)
    }

    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        let tag = DPTag.load(fromCache: Int32(tagIds[indexPath.row]))
        return DPTagCell.tagHeight(tag)
    }

    // MARK: - Editing

    override func tableView(_ tableView: UITableView, canEditRowAt indexPath: IndexPath) -> Bool { true }

    override func tableView(_ tableView: UITableView, commit editingStyle: UITableViewCell.EditingStyle, forRowAt indexPath: IndexPath) {
        if editingStyle == .delete {
            ListModel.get(listKey).remove(tagIds[indexPath.row])
            tableView.reloadData()
        }
    }

    override func tableView(_ tableView: UITableView, canMoveRowAt indexPath: IndexPath) -> Bool { true }

    override func tableView(_ tableView: UITableView, moveRowAt sourceIndexPath: IndexPath, to destinationIndexPath: IndexPath) {
        let model = ListModel.get(listKey)
        var ids = model.ids
        let item = ids.remove(at: sourceIndexPath.row)
        ids.insert(item, at: destinationIndexPath.row)
        model.setIds(ids)
    }
}
