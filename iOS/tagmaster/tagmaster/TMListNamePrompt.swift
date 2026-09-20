//
//  TMListNamePrompt.swift
//  tagmaster
//
//  The two alerts every list-management entry point shares: naming a list (new
//  or renamed) and confirming a delete.
//
//  Naming validates as the user types through `TMTagLists.nameErrorMessage`, so
//  the alert never closes on a name the registry would reject: the confirming
//  action stays disabled and the reason takes the alert's message line. An empty
//  field is not yet a mistake, so it keeps the example hint instead.
//

import Foundation
import ObjectiveC
import UIKit

public final class TMListNamePrompt: NSObject {
    /// Shown under the field of the new-list alert until the name needs correcting.
    public static let exampleHint = "For example “Afterglow set”"

    /// Address-only key for the association that keeps a validator alive.
    private static var validatorKey: UInt8 = 0

    private let excluding: String?
    private let defaultMessage: String?
    private weak var alert: UIAlertController?
    private weak var confirmAction: UIAlertAction?

    private init(excluding: String?, defaultMessage: String?) {
        self.excluding = excluding
        self.defaultMessage = defaultMessage
    }

    /// The `New list` alert. `commit` receives the normalized, already-validated name.
    public static func createAlert(commit: @escaping (String) -> Void) -> UIAlertController {
        makeAlert(title: "New list",
                  actionTitle: "Create",
                  initialName: nil,
                  excluding: nil,
                  defaultMessage: exampleHint,
                  commit: commit)
    }

    /// The `Rename list` alert for an existing custom list, prefilled with its name.
    public static func renameAlert(for key: String, commit: @escaping (String) -> Void) -> UIAlertController {
        makeAlert(title: "Rename list",
                  actionTitle: "Rename",
                  initialName: TMTagLists.name(for: key),
                  excluding: key,
                  defaultMessage: nil,
                  commit: commit)
    }

    private static func makeAlert(title: String,
                                  actionTitle: String,
                                  initialName: String?,
                                  excluding: String?,
                                  defaultMessage: String?,
                                  commit: @escaping (String) -> Void) -> UIAlertController {
        let prompt = TMListNamePrompt(excluding: excluding, defaultMessage: defaultMessage)
        let alert = UIAlertController(title: title, message: defaultMessage, preferredStyle: .alert)
        alert.view.accessibilityIdentifier = "list.name.alert"
        alert.addTextField { field in
            field.placeholder = "List name"
            field.text = initialName
            field.autocapitalizationType = .sentences
            field.clearButtonMode = .whileEditing
            field.returnKeyType = .done
            field.accessibilityIdentifier = "list.name.field"
            field.addTarget(prompt, action: #selector(nameChanged(_:)), for: .editingChanged)
        }
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        let confirm = UIAlertAction(title: actionTitle, style: .default) { [weak alert] _ in
            let typed = alert?.textFields?.first?.text ?? ""
            guard TMTagLists.validateName(typed, excluding: excluding) == .none else { return }
            commit(TMTagLists.normalizeName(typed))
        }
        alert.addAction(confirm)
        alert.preferredAction = confirm
        prompt.alert = alert
        prompt.confirmAction = confirm
        // The text field does not retain its target, and nothing else owns the
        // validator; the alert it validates does, for exactly as long as it lives.
        objc_setAssociatedObject(alert, &TMListNamePrompt.validatorKey, prompt, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        prompt.validate(initialName ?? "")
        return alert
    }

    @objc private func nameChanged(_ field: UITextField) {
        validate(field.text ?? "")
    }

    private func validate(_ name: String) {
        let problem = TMTagLists.nameErrorMessage(name, excluding: excluding)
        confirmAction?.isEnabled = problem == nil
        // Nothing typed yet is not a mistake worth reporting; keep the hint.
        let untouched = TMTagLists.normalizeName(name).isEmpty
        alert?.message = untouched ? defaultMessage : (problem ?? defaultMessage)
    }
}

/// The confirmation every delete of a custom list goes through, naming the list
/// and how many tags leave the user's lists with it.
public final class TMListDeletePrompt: NSObject {
    public static func alert(for key: String, confirm: @escaping () -> Void) -> UIAlertController {
        let count = TMTagLists.ids(for: key).count
        let message: String
        switch count {
        case 0: message = "This list is empty."
        case 1: message = "This removes the list and its 1 tag from your lists. Tags stay in the catalog."
        default: message = "This removes the list and its \(count) tags from your lists. Tags stay in the catalog."
        }
        let alert = UIAlertController(title: "Delete “\(TMTagLists.name(for: key))”?",
                                      message: message,
                                      preferredStyle: .alert)
        alert.view.accessibilityIdentifier = "list.delete.alert"
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        alert.addAction(UIAlertAction(title: "Delete", style: .destructive) { _ in confirm() })
        return alert
    }
}
