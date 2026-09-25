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

    var isNaming: Bool {
        get { if case .create = kind { return true }; if case .rename = kind { return true }; return false }
        set { if !newValue { dismiss() } }
    }

    var isConfirmingDelete: Bool {
        get { if case .delete = kind { return true }; return false }
        set { if !newValue { dismiss() } }
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
    func setListPrompts(_ prompts: SetListPromptModel) -> some View {
        modifier(SetListPromptAlerts(prompts: prompts))
    }
}

private struct SetListPromptAlerts: ViewModifier {
    @Bindable var prompts: SetListPromptModel

    func body(content: Content) -> some View {
        content
            .alert(prompts.title, isPresented: $prompts.isNaming) {
                TextField("Set list name", text: $prompts.name)
                    .textInputAutocapitalization(.sentences)
                    .submitLabel(.done)
                    .accessibilityIdentifier("setlist.name.field")
                Button("Cancel", role: .cancel) { prompts.dismiss() }
                Button(prompts.actionTitle) { prompts.confirmName() }
                    .disabled(!prompts.canConfirm)
                    .keyboardShortcut(.defaultAction)
            } message: {
                if let message = prompts.message { Text(message) }
            }
            .alert(prompts.title, isPresented: $prompts.isConfirmingDelete) {
                Button("Cancel", role: .cancel) { prompts.dismiss() }
                Button("Delete", role: .destructive) { prompts.confirmDelete() }
            } message: {
                if let message = prompts.message { Text(message) }
            }
    }
}
