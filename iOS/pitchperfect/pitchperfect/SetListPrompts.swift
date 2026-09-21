//
//  SetListPrompts.swift
//  pitchperfect
//
//  The two alerts every set-list entry point shares: naming a set list (new or
//  renamed) and confirming a delete.
//
//  Naming validates as the user types through `DPSongsModel.nameErrorMessage`,
//  so the alert never closes on a name the model would reject: the confirming
//  action stays disabled and the reason takes the alert's message line. An
//  empty field is not yet a mistake, so it keeps the example hint instead.
//

import Foundation
import ObjectiveC
import UIKit

@objc public final class SetListPrompts: NSObject {
    /// Shown under the field of the new-set-list alert until the name needs correcting.
    @objc public static let exampleHint = "For example “Saturday show”"

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

    /// The "New set list" alert. `commit` receives the normalized, already-validated name.
    @objc public static func createAlert(commit: @escaping (String) -> Void) -> UIAlertController {
        makeAlert(title: "New set list",
                  actionTitle: "Create",
                  initialName: nil,
                  excluding: nil,
                  defaultMessage: exampleHint,
                  commit: commit)
    }

    /// The "Rename set list" alert, prefilled with the list's display name.
    @objc public static func renameAlert(for list: DPSongList,
                                         commit: @escaping (String) -> Void) -> UIAlertController {
        makeAlert(title: "Rename set list",
                  actionTitle: "Rename",
                  initialName: DPSongsModel.sharedInstance.displayName(for: list),
                  excluding: list.id,
                  defaultMessage: nil,
                  commit: commit)
    }

    private static func makeAlert(title: String,
                                  actionTitle: String,
                                  initialName: String?,
                                  excluding: String?,
                                  defaultMessage: String?,
                                  commit: @escaping (String) -> Void) -> UIAlertController {
        let prompt = SetListPrompts(excluding: excluding, defaultMessage: defaultMessage)
        let alert = UIAlertController(title: title, message: defaultMessage, preferredStyle: .alert)
        alert.view.accessibilityIdentifier = "setlist.name.alert"
        alert.addTextField { field in
            field.placeholder = "Set list name"
            field.text = initialName
            field.autocapitalizationType = .sentences
            field.clearButtonMode = .whileEditing
            field.returnKeyType = .done
            field.accessibilityIdentifier = "setlist.name.field"
            field.addTarget(prompt, action: #selector(nameChanged(_:)), for: .editingChanged)
        }
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        let confirm = UIAlertAction(title: actionTitle, style: .default) { [weak alert] _ in
            let typed = alert?.textFields?.first?.text ?? ""
            guard DPSongsModel.sharedInstance.validateName(typed, excluding: excluding) == nil else { return }
            commit(DPSongsModel.normalizeName(typed))
        }
        alert.addAction(confirm)
        alert.preferredAction = confirm
        prompt.alert = alert
        prompt.confirmAction = confirm
        // The text field does not retain its target, and nothing else owns the
        // validator; the alert it validates does, for exactly as long as it lives.
        objc_setAssociatedObject(alert, &SetListPrompts.validatorKey, prompt, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        prompt.validate(initialName ?? "")
        return alert
    }

    @objc private func nameChanged(_ field: UITextField) {
        validate(field.text ?? "")
    }

    private func validate(_ name: String) {
        let problem = DPSongsModel.sharedInstance.nameErrorMessage(name, excluding: excluding)
        confirmAction?.isEnabled = problem == nil
        // Nothing typed yet is not a mistake worth reporting; keep the hint.
        let untouched = DPSongsModel.normalizeName(name).isEmpty
        alert?.message = untouched ? defaultMessage : (problem ?? defaultMessage)
    }

    /// The confirmation every set-list delete goes through, naming the list and
    /// the songs that leave with it.
    @objc public static func deleteAlert(for list: DPSongList,
                                         confirm: @escaping () -> Void) -> UIAlertController {
        let model = DPSongsModel.sharedInstance
        let count = list.songs.count
        let message: String
        switch count {
        case 0: message = "This set list is empty."
        case 1: message = "This removes the set list and its 1 song. My Songs is not affected."
        default: message = "This removes the set list and its \(count) songs. My Songs is not affected."
        }
        let alert = UIAlertController(title: "Delete “\(model.displayName(for: list))”?",
                                      message: message,
                                      preferredStyle: .alert)
        alert.view.accessibilityIdentifier = "setlist.delete.alert"
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        alert.addAction(UIAlertAction(title: "Delete", style: .destructive) { _ in confirm() })
        return alert
    }
}
