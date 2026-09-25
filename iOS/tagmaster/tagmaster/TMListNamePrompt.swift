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
    public static let exampleHint = "For example “Easy tags” or “High and lows”"

    /// Address-only key for the association that keeps a validator alive.
    private static var validatorKey: UInt8 = 0

    private let excluding: String?
    private weak var alert: UIAlertController?
    private weak var confirmAction: UIAlertAction?

    private init(excluding: String?) {
        self.excluding = excluding
    }

    /// The `New list` alert. `commit` receives the normalized, already-validated name.
    public static func createAlert(cancel: (() -> Void)? = nil,
                                   commit: @escaping (String) -> Void) -> UIAlertController {
        makeAlert(title: "New list",
                  actionTitle: "Create",
                  initialName: nil,
                  excluding: nil,
                  defaultMessage: exampleHint,
                  cancel: cancel,
                  commit: commit)
    }

    /// The `Rename list` alert for an existing custom list, prefilled with its name.
    public static func renameAlert(for key: String, cancel: (() -> Void)? = nil,
                                   commit: @escaping (String) -> Void) -> UIAlertController {
        makeAlert(title: "Rename list",
                  actionTitle: "Rename",
                  initialName: TMTagLists.name(for: key),
                  excluding: key,
                  defaultMessage: nil,
                  cancel: cancel,
                  commit: commit)
    }

    /// The alert for a SwiftUI screen's prompt state. The message line must follow
    /// the typing, which a SwiftUI alert cannot do, so the prompt is this UIKit alert.
    static func alert(for prompt: TMNamePrompt, cancel: @escaping () -> Void,
                      commit: @escaping (String) -> Void) -> UIAlertController {
        switch prompt.purpose {
        case .create: return createAlert(cancel: cancel, commit: commit)
        case .rename(let key): return renameAlert(for: key, cancel: cancel, commit: commit)
        }
    }

    private static func makeAlert(title: String,
                                  actionTitle: String,
                                  initialName: String?,
                                  excluding: String?,
                                  defaultMessage: String?,
                                  cancel: (() -> Void)?,
                                  commit: @escaping (String) -> Void) -> UIAlertController {
        let prompt = TMListNamePrompt(excluding: excluding)
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
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel) { _ in cancel?() })
        let confirm = UIAlertAction(title: actionTitle, style: .default) { [weak alert] _ in
            let typed = alert?.textFields?.first?.text ?? ""
            guard let name = TMListNamePrompt.prompt(excluding: excluding, text: typed).committedName else { return }
            commit(name)
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
        let prompt = TMListNamePrompt.prompt(excluding: excluding, text: name)
        confirmAction?.isEnabled = prompt.canConfirm
        alert?.message = prompt.message
    }

    /// The same rules the SwiftUI screens hold their prompts to.
    private static func prompt(excluding: String?, text: String) -> TMNamePrompt {
        TMNamePrompt(purpose: excluding.map { .rename($0) } ?? .create, text: text)
    }
}

/// The confirmation every delete of a custom list goes through, naming the list
/// and how many tags leave the user's lists with it.
public final class TMListDeletePrompt: NSObject {
    /// `settled` runs whichever way the alert goes, for the caller that has a
    /// half-open swipe waiting on the answer.
    public static func alert(for key: String,
                             settled: (() -> Void)? = nil,
                             confirm: @escaping () -> Void) -> UIAlertController {
        let count = TMTagLists.ids(for: key).count
        let message: String
        switch count {
        case 0: message = "“\(TMTagLists.name(for: key))” has no tags. It will be removed from your lists."
        case 1: message = "This removes the list and its 1 tag from your lists. Tags stay in the catalog."
        default: message = "This removes the list and its \(count) tags from your lists. Tags stay in the catalog."
        }
        let alert = UIAlertController(title: "Delete “\(TMTagLists.name(for: key))”?",
                                      message: message,
                                      preferredStyle: .alert)
        alert.view.accessibilityIdentifier = "list.delete.alert"
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel) { _ in settled?() })
        alert.addAction(UIAlertAction(title: "Delete", style: .destructive) { _ in
            confirm()
            settled?()
        })
        return alert
    }
}
