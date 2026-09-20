//
//  DPHomeViewController.swift
//  tagmaster
//
//  Created by David Poll on 6/8/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import UIKit

/// The Lists group's section index, mirroring `TMHomeListsSection` in the header.
private let tmHomeListsSection = 1

extension DPHomeViewController: UITextFieldDelegate {
    @objc func viewDidLoadExtension() {
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(onUserDataChanged),
                                               name: .userDataChanged,
                                               object: nil)
    }
    
    @objc func onUserDataChanged() {
        // Home animates the change it made itself; a reload here would replace
        // that animation, or cancel a drag still in progress.
        guard !self.tm_applyingLocalListChange else { return }
        guard !self.tableView.hasUncommittedUpdates && !self.tableView.isDragging else { return }
        self.tableView.reloadData()
        self.updateEditButton()
        self.tm_syncSelectionForSplit()
    }

    // MARK: - Managing lists from Home

    /// Row index of `key` in the Lists group, which starts with Teachable Tags.
    private func listsRow(of key: String) -> Int? {
        TMTagLists.customKeys().firstIndex(of: key).map { $0 + 1 }
    }

    @objc func promptNewList() {
        present(TMListNamePrompt.createAlert { [weak self] name in
            guard let self else { return }
            self.tm_applyingLocalListChange = true
            let key = TMTagLists.createList(named: name)
            self.tm_applyingLocalListChange = false
            guard let key, let row = self.listsRow(of: key) else {
                self.tableView.reloadData()
                return
            }
            self.tableView.performBatchUpdates {
                self.tableView.insertRows(at: [IndexPath(row: row, section: tmHomeListsSection)],
                                          with: .automatic)
            }
            self.updateEditButton()
            UIAccessibility.post(notification: .layoutChanged, argument: nil)
        }, animated: true)
    }

    @objc func promptRenameList(_ key: String) {
        present(TMListNamePrompt.renameAlert(for: key) { [weak self] name in
            guard let self else { return }
            self.tm_applyingLocalListChange = true
            TMTagLists.renameList(key, to: name)
            self.tm_applyingLocalListChange = false
            if let row = self.listsRow(of: key) {
                self.tableView.reloadRows(at: [IndexPath(row: row, section: tmHomeListsSection)],
                                          with: .automatic)
            } else {
                self.tableView.reloadData()
            }
        }, animated: true)
    }

    @objc func confirmDeleteList(_ key: String) {
        present(TMListDeletePrompt.alert(for: key) { [weak self] in
            guard let self else { return }
            let row = self.listsRow(of: key)
            self.tm_applyingLocalListChange = true
            TMTagLists.deleteList(key)
            self.tm_applyingLocalListChange = false
            if let row {
                self.tableView.performBatchUpdates {
                    self.tableView.deleteRows(at: [IndexPath(row: row, section: tmHomeListsSection)],
                                              with: .automatic)
                }
            } else {
                self.tableView.reloadData()
            }
            self.updateEditButton()
            UIAccessibility.post(notification: .layoutChanged, argument: nil)
        }, animated: true)
    }
    
    public func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        if string.rangeOfCharacter(from: NSCharacterSet.decimalDigits) != nil || string.count == 0 {
            return true
        }
        return false
    }
    
    @objc func openTag() {
        let alert = UIAlertController(title: "Open Tag", message: "Enter Tag ID", preferredStyle: .alert)
        alert.addTextField {
            $0.keyboardType = .decimalPad
            $0.delegate = self
            $0.returnKeyType = .go
            $0.enablesReturnKeyAutomatically = true
        }
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        alert.addAction(UIAlertAction(title: "Open", style: .default, handler: { action in
            if let tagId = Int32(alert.textFields![0].text ?? "") {
                self.openTag(tagId: tagId)
            }
        }))
        self.present(alert, animated: true)
    }
    
    private func openTag(tagId: Int32) {
        DPAppDelegate.showTag(withId: tagId, from: self)
    }
    
    public func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        if Int32(textField.text ?? "") != nil {
            return true
        }
        return false
    }
}
