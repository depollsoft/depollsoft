//
//  TMNamePrompt.swift
//  tagmaster
//
//  The two alerts every list-management entry point shares, in SwiftUI:
//  naming a list (new or renamed) and confirming a delete.
//
//  Naming validates as the user types through `TMTagLists.nameErrorMessage`,
//  so the alert never closes on a name the registry would reject: the
//  confirming button stays disabled and the reason takes the message line. An
//  empty field is not yet a mistake, so it keeps the example hint instead.
//

import SwiftUI

/// A naming alert's state: what it says, what has been typed, whether it can commit.
struct TMNamePrompt: Identifiable, Equatable {
    /// Shown under the field of the new-list alert until the name needs correcting.
    static let exampleHint = "For example “Easy tags” or “High and lows”"

    enum Purpose: Equatable {
        case create
        case rename(String)
    }

    let id = UUID()
    let purpose: Purpose
    var text: String

    static func create() -> TMNamePrompt { TMNamePrompt(purpose: .create, text: "") }

    static func rename(_ key: String) -> TMNamePrompt {
        TMNamePrompt(purpose: .rename(key), text: TMTagLists.name(for: key))
    }

    var title: String { purpose == .create ? "New list" : "Rename list" }
    var actionTitle: String { purpose == .create ? "Create" : "Rename" }

    private var excluding: String? {
        if case .rename(let key) = purpose { return key }
        return nil
    }

    private var hint: String? { purpose == .create ? TMNamePrompt.exampleHint : nil }

    /// Why the typed name cannot be used, or nil when it can.
    var problem: String? { TMTagLists.nameErrorMessage(text, excluding: excluding) }

    var canConfirm: Bool { problem == nil }

    /// The line under the title: the hint until something is typed, then any problem.
    var message: String? {
        TMTagLists.normalizeName(text).isEmpty ? hint : (problem ?? hint)
    }

    /// The name to store, or nil when the registry would reject it.
    var committedName: String? {
        canConfirm ? TMTagLists.normalizeName(text) : nil
    }
}

/// A delete confirmation naming the list and how many tags leave with it.
struct TMDeletePrompt: Identifiable, Equatable {
    let key: String
    var id: String { key }

    var title: String { "Delete “\(TMTagLists.name(for: key))”?" }

    var message: String {
        let count = TMTagLists.ids(for: key).count
        switch count {
        case 0: return "“\(TMTagLists.name(for: key))” has no tags. It will be removed from your lists."
        case 1: return "This removes the list and its 1 tag from your lists. Tags stay in the catalog."
        default: return "This removes the list and its \(count) tags from your lists. Tags stay in the catalog."
        }
    }
}

extension View {
    /// Presents `prompt` as the list-naming alert; `commit` receives the normalized name.
    func tmNamePrompt(_ prompt: Binding<TMNamePrompt?>, commit: @escaping (TMNamePrompt, String) -> Void) -> some View {
        background {
            TMAlertPresenter(item: prompt) { shown in
                TMListNamePrompt.alert(for: shown, cancel: { prompt.wrappedValue = nil }) { name in
                    prompt.wrappedValue = nil
                    commit(shown, name)
                }
            }
        }
    }

    /// Presents a delete confirmation; `settled` runs whichever way it is answered.
    func tmDeletePrompt(_ prompt: Binding<TMDeletePrompt?>,
                        settled: @escaping () -> Void = {},
                        confirm: @escaping (String) -> Void) -> some View {
        alert(prompt.wrappedValue?.title ?? "",
              isPresented: Binding(get: { prompt.wrappedValue != nil },
                                   set: { if !$0 { prompt.wrappedValue = nil } }),
              presenting: prompt.wrappedValue) { shown in
            Button("Cancel", role: .cancel) {
                prompt.wrappedValue = nil
                settled()
            }
            Button("Delete", role: .destructive) {
                prompt.wrappedValue = nil
                confirm(shown.key)
                settled()
            }
        } message: { shown in
            Text(shown.message)
        }
    }
}

/// Presents a UIKit alert for as long as `item` is set: the one place a list
/// screen needs an alert SwiftUI cannot express.
struct TMAlertPresenter<Item: Identifiable>: UIViewControllerRepresentable {
    @Binding var item: Item?
    let makeAlert: (Item) -> UIViewController

    final class Coordinator {
        var shownId: Item.ID?
        weak var alert: UIViewController?
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIViewController(context: Context) -> UIViewController {
        let anchor = UIViewController()
        anchor.view.isHidden = true
        return anchor
    }

    func updateUIViewController(_ anchor: UIViewController, context: Context) {
        let coordinator = context.coordinator
        if let item, coordinator.shownId != item.id {
            coordinator.shownId = item.id
            let alert = makeAlert(item)
            coordinator.alert = alert
            DispatchQueue.main.async {
                guard anchor.view.window != nil else { return }
                anchor.present(alert, animated: !UIAccessibility.isReduceMotionEnabled)
            }
        } else if item == nil, coordinator.shownId != nil {
            coordinator.shownId = nil
            if let alert = coordinator.alert, alert.presentingViewController != nil, !alert.isBeingDismissed {
                alert.dismiss(animated: false)
            }
        }
    }
}
