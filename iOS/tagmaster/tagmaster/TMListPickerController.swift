//
//  TMListPickerController.swift
//  tagmaster
//
//  "Add to list": every list the app knows, each row toggling this tag's
//  membership the moment it is tapped, so the chips and bar buttons behind the
//  sheet update live. The last row makes a new list and puts the tag in it.
//
//  Presented as a popover from the bar button that opened it on iPad, and as a
//  half-height sheet on iPhone.
//

import Foundation
import UIKit

@objc(TMListPickerController)
public class TMListPickerController: UITableViewController {
    @objc public let tagId: Int32
    private var keys: [String] = []

    @objc public init(tagId: Int32) {
        self.tagId = tagId
        super.init(style: .insetGrouped)
    }

    required init?(coder: NSCoder) {
        fatalError("TMListPickerController is created in code")
    }

    public override func viewDidLoad() {
        super.viewDidLoad()
        title = "Add to list"
        navigationItem.largeTitleDisplayMode = .never
        let done = UIBarButtonItem(title: "Done", style: .done, target: self, action: #selector(dismissPicker))
        done.accessibilityIdentifier = "picker.done"
        navigationItem.rightBarButtonItem = done
        tableView.accessibilityIdentifier = "picker.table"
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 44
        keys = TMTagLists.allKeys()
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(onUserDataChanged),
                                               name: .userDataChanged,
                                               object: nil)
    }

    @objc private func onUserDataChanged() {
        guard isViewLoaded else { return }
        keys = TMTagLists.allKeys()
        tableView.reloadData()
    }

    @objc private func dismissPicker() {
        presentingViewController?.dismiss(animated: true)
    }

    // MARK: - Rows

    public override func numberOfSections(in tableView: UITableView) -> Int { 1 }

    public override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        keys.count + 1
    }

    public override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = UITableViewCell(style: .default, reuseIdentifier: nil)
        cell.textLabel?.font = .preferredFont(forTextStyle: .body)
        cell.textLabel?.adjustsFontForContentSizeCategory = true
        cell.textLabel?.numberOfLines = 0
        guard indexPath.row < keys.count else {
            cell.textLabel?.text = "New list…"
            cell.imageView?.image = UIImage(systemName: "plus.circle")
            cell.imageView?.tintColor = DPAppDelegate.accentColor()
            cell.accessibilityIdentifier = "picker.row.new"
            return cell
        }
        let key = keys[indexPath.row]
        let member = TMTagLists.contains(Int(tagId), in: key)
        cell.textLabel?.text = TMTagLists.name(for: key)
        cell.accessoryType = member ? .checkmark : .none
        cell.accessibilityIdentifier = "picker.row.\(key)"
        cell.accessibilityTraits = member ? [.button, .selected] : .button
        return cell
    }

    public override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard indexPath.row < keys.count else {
            promptForNewList()
            return
        }
        let key = keys[indexPath.row]
        let added = TMTagLists.toggle(Int(tagId), in: key)
        // The registry's change notification already rebuilt every row.
        let name = TMTagLists.name(for: key)
        UIAccessibility.post(notification: .announcement,
                             argument: added ? "Added to \(name)" : "Removed from \(name)")
    }

    private func promptForNewList() {
        present(TMListNamePrompt.createAlert { [weak self] name in
            guard let self, let key = TMTagLists.createList(named: name) else { return }
            TMTagLists.add(Int(self.tagId), to: key)
            UIAccessibility.post(notification: .announcement, argument: "Added to \(name)")
        }, animated: true)
    }

    // MARK: - Presentation

    /// Opens the picker for `tagId` the way the platform expects: a popover
    /// anchored to whatever opened it on iPad, a half-height sheet on iPhone.
    @objc public static func present(forTagId tagId: Int32,
                                     from presenter: UIViewController,
                                     barButtonItem: UIBarButtonItem?,
                                     sourceView: UIView?) {
        let picker = TMListPickerController(tagId: tagId)
        let navigation = UINavigationController(rootViewController: picker)
        if UIDevice.current.userInterfaceIdiom == .pad {
            navigation.modalPresentationStyle = .popover
            navigation.preferredContentSize = CGSize(width: 340, height: 420)
            let popover = navigation.popoverPresentationController
            if let barButtonItem {
                popover?.barButtonItem = barButtonItem
            } else if let sourceView {
                popover?.sourceView = sourceView
                popover?.sourceRect = sourceView.bounds
            }
        } else {
            navigation.modalPresentationStyle = .pageSheet
            navigation.sheetPresentationController?.detents = [.medium(), .large()]
            navigation.sheetPresentationController?.prefersGrabberVisible = true
        }
        presenter.present(navigation, animated: true)
    }
}
