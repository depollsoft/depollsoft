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

import SwiftUI
import UIKit

@Observable
@MainActor
final class SetListPromptModel {
    /// Shown under the field of the new-set-list alert until the name needs correcting.
    static let exampleHint = "For example “Saturday show”"

    enum Kind: Equatable {
        case create
        case rename(DPSongList)
        case delete(DPSongList)
    }

    private let model: DPSongsModel
    private(set) var kind: Kind?
    var name = ""
    private var commitName: ((String) -> Void)?
    private var commitDelete: (() -> Void)?

    init(model: DPSongsModel = .sharedInstance) {
        self.model = model
    }

    func create(commit: @escaping (String) -> Void) {
        name = ""
        commitName = commit
        kind = .create
    }

    func rename(_ list: DPSongList, commit: @escaping (String) -> Void) {
        name = model.displayName(for: list)
        commitName = commit
        kind = .rename(list)
    }

    func delete(_ list: DPSongList, confirm: @escaping () -> Void) {
        commitDelete = confirm
        kind = .delete(list)
    }

    func dismiss() {
        kind = nil
        commitName = nil
        commitDelete = nil
    }

    // MARK: Naming

    var title: String {
        switch kind {
        case .create: return "New set list"
        case .rename: return "Rename set list"
        case .delete(let list): return "Delete “\(model.displayName(for: list))”?"
        case nil: return ""
        }
    }

    var actionTitle: String {
        if case .rename = kind { return "Rename" }
        return "Create"
    }

    private var excluding: String? {
        if case .rename(let list) = kind { return list.id }
        return nil
    }

    private var defaultMessage: String? {
        if case .create = kind { return Self.exampleHint }
        return nil
    }

    var problem: String? { model.nameErrorMessage(name, excluding: excluding) }
    var canConfirm: Bool { problem == nil }

    /// The hint while nothing is typed; the reason once a typed name is unusable.
    var message: String? {
        if case .delete(let list) = kind { return Self.deleteMessage(songCount: list.songs.count) }
        let untouched = DPSongsModel.normalizeName(name).isEmpty
        return untouched ? defaultMessage : (problem ?? defaultMessage)
    }

    func confirmName() {
        let commit = commitName
        let typed = name
        let excluded = excluding
        dismiss()
        guard model.validateName(typed, excluding: excluded) == nil else { return }
        commit?(DPSongsModel.normalizeName(typed))
    }

    func confirmDelete() {
        let confirm = commitDelete
        dismiss()
        confirm?()
    }

    static func deleteMessage(songCount count: Int) -> String {
        switch count {
        case 0: return "This set list is empty."
        case 1: return "This removes the set list and its 1 song. My Songs is not affected."
        default: return "This removes the set list and its \(count) songs. My Songs is not affected."
        }
    }
}

extension View {
    /// Presents whichever set-list alert `prompts` holds.
    ///
    /// These are system alerts presented directly rather than through SwiftUI's
    /// `.alert`, whose content is fixed once shown: naming validates as the user
    /// types, updating the message and enabling the confirming action live.
    func setListPrompts(_ prompts: SetListPromptModel) -> some View {
        background(SetListAlertPresenter(prompts: prompts, kind: prompts.kind).frame(width: 0, height: 0))
    }
}

private struct SetListAlertPresenter: UIViewControllerRepresentable {
    let prompts: SetListPromptModel
    /// Read in the modifier's body so a change re-runs `updateUIViewController`.
    let kind: SetListPromptModel.Kind?

    final class Presenter: UIViewController {
        var prompts: SetListPromptModel?
        weak var alert: UIAlertController?
        weak var confirm: UIAlertAction?
        var shownKind: SetListPromptModel.Kind?

        func sync(_ kind: SetListPromptModel.Kind?) {
            guard kind != shownKind else { return }
            // An action closes its alert itself; only a prompt withdrawn from
            // code needs dismissing (and never one already on its way out).
            if let alert, alert.presentingViewController != nil, !alert.isBeingDismissed {
                alert.dismiss(animated: true)
            }
            alert = nil
            shownKind = kind
            guard let kind, let prompts else { return }
            let alert = makeAlert(kind, prompts: prompts)
            self.alert = alert
            // Present once this controller is in a window; SwiftUI may update it first.
            DispatchQueue.main.async { [weak self] in
                guard let self, self.alert === alert else { return }
                var presenter: UIViewController = self
                while let next = presenter.presentedViewController { presenter = next }
                presenter.present(alert, animated: true)
            }
        }

        private func makeAlert(_ kind: SetListPromptModel.Kind, prompts: SetListPromptModel) -> UIAlertController {
            let alert = UIAlertController(title: prompts.title, message: prompts.message, preferredStyle: .alert)
            if case .delete = kind {
                alert.view.accessibilityIdentifier = "setlist.delete.alert"
                alert.addAction(UIAlertAction(title: "Cancel", style: .cancel) { _ in prompts.dismiss() })
                alert.addAction(UIAlertAction(title: "Delete", style: .destructive) { _ in prompts.confirmDelete() })
                return alert
            }
            alert.view.accessibilityIdentifier = "setlist.name.alert"
            alert.addTextField { field in
                field.placeholder = "Set list name"
                field.text = prompts.name
                field.autocapitalizationType = .sentences
                field.clearButtonMode = .whileEditing
                field.returnKeyType = .done
                field.accessibilityIdentifier = "setlist.name.field"
                field.addTarget(self, action: #selector(self.nameChanged(_:)), for: .editingChanged)
            }
            alert.addAction(UIAlertAction(title: "Cancel", style: .cancel) { _ in prompts.dismiss() })
            let confirm = UIAlertAction(title: prompts.actionTitle, style: .default) { _ in prompts.confirmName() }
            alert.addAction(confirm)
            alert.preferredAction = confirm
            self.confirm = confirm
            confirm.isEnabled = prompts.canConfirm
            return alert
        }

        @objc private func nameChanged(_ field: UITextField) {
            guard let prompts else { return }
            prompts.name = field.text ?? ""
            confirm?.isEnabled = prompts.canConfirm
            alert?.message = prompts.message
        }
    }

    func makeUIViewController(context: Context) -> Presenter {
        let presenter = Presenter()
        presenter.view.isHidden = true
        return presenter
    }

    func updateUIViewController(_ presenter: Presenter, context: Context) {
        presenter.prompts = prompts
        presenter.sync(kind)
    }
}
