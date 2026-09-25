//
//  TMListPicker.swift
//  tagmaster
//
//  "Add to list": every list the app knows, each row toggling this tag's
//  membership the moment it is tapped, so the chips and bar buttons behind the
//  sheet update live. The last row makes a new list and puts the tag in it.
//
//  A popover anchored to whatever opened it on iPad, a half-height sheet with a
//  grabber on iPhone.
//

import SwiftUI

/// The picker's state: the lists, this tag's membership, and naming a new list.
@Observable @MainActor
final class TMListPickerModel {
    let tagId: Int32
    private(set) var revision = 0
    /// The new-list naming alert while it is up; the same live-validating prompt Home uses.
    var namePrompt: TMNamePrompt?
    private var observer: NSObjectProtocol?

    init(tagId: Int32) {
        self.tagId = tagId
        observer = NotificationCenter.default.addObserver(forName: .userDataChanged, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.revision += 1 }
        }
    }

    isolated deinit {
        if let observer { NotificationCenter.default.removeObserver(observer) }
    }

    var keys: [String] {
        _ = revision
        return TMTagLists.allKeys()
    }

    func isMember(_ key: String) -> Bool {
        _ = revision
        return TMTagLists.contains(Int(tagId), in: key)
    }

    func count(_ key: String) -> Int {
        _ = revision
        return TMTagLists.ids(for: key).count
    }

    static func countText(_ count: Int) -> String {
        count == 1 ? "1 tag" : "\(count) tags"
    }

    /// Adds the tag to `key`, or takes it off, and says which.
    func toggle(_ key: String) {
        let added = TMTagLists.toggle(Int(tagId), in: key)
        let name = TMTagLists.name(for: key)
        UIAccessibility.post(notification: .announcement, argument: added ? "Added to \(name)" : "Removed from \(name)")
    }

    // MARK: New list

    func beginNewList() {
        namePrompt = .create()
    }

    /// Makes the list named in the prompt and puts this tag straight into it.
    func createList(named name: String) {
        guard let key = TMTagLists.createList(named: name) else { return }
        TMTagLists.add(Int(tagId), to: key)
        UIAccessibility.post(notification: .announcement, argument: "Added to \(name)")
    }
}

struct TMListPicker: View {
    /// The rows set their accent explicitly, as the UIKit cells did.
    private let accent = Color(DPAppDelegate.accentColor() ?? .tintColor)
    @State var model: TMListPickerModel
    /// Closes the picker; the presenter owns how.
    var onDone: (() -> Void)?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                ForEach(model.keys, id: \.self) { key in
                    row(key)
                }
                Button(action: model.beginNewList) {
                    TMPickerRowLayout(symbol: "plus.circle", iconColor: accent,
                                      name: "New list…", count: nil, member: false)
                }
                .listRowInsets(EdgeInsets())
                .accessibilityIdentifier("picker.row.new")
            }
            .listStyle(.insetGrouped)
            // Popovers and sheets draw their own glass, which UIKit's table let show through.
            .scrollContentBackground(.hidden)
            // A hardware keyboard would otherwise light the first row as focused.
            .focusEffectDisabled()
            .modifier(TMClearNavigationContainer())
            .modifier(TMInsetGroupMargins())
            .focusEffectDisabled()
            .accessibilityIdentifier("picker.table")
            .navigationTitle("Add to list")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    TMDoneButton { if let onDone { onDone() } else { dismiss() } }
                        .accessibilityIdentifier("picker.done")
                }
            }
            .tmNamePrompt($model.namePrompt) { _, name in model.createList(named: name) }
        }
    }

    private func row(_ key: String) -> some View {
        let member = model.isMember(key)
        let count = TMListPickerModel.countText(model.count(key))
        return Button { model.toggle(key) } label: {
            TMPickerRowLayout(symbol: TagSummaryModel.symbolName(for: key), iconColor: Color(.secondaryLabel),
                              name: TMTagLists.name(for: key), count: count, member: member)
        }
        .listRowInsets(EdgeInsets())
        .focusable(false)
        .accessibilityLabel(TMTagLists.name(for: key))
        .accessibilityValue(count)
        .accessibilityAddTraits(member ? [.isButton, .isSelected] : .isButton)
        .accessibilityIdentifier("picker.row.\(key)")
    }
}

extension View {
    /// Presents the list picker for the detail's tag while `source` is the one that opened
    /// it, anchored to this view.
    func tmListPicker(model: TagDetailModel, source: TMPickerSource) -> some View {
        background(TMListPickerPresenter(
            isPresented: Binding(get: { model.pickerPresented(from: source) },
                                 set: { if !$0, model.pickerSource == source { model.pickerSource = nil } }),
            tagId: model.tagId))
    }
}

/// Presents the picker the way UIKit did: a popover with its arrow on whatever opened it
/// on iPad (a bar button, a chip), a half-height sheet with a grabber on iPhone.
/// SwiftUI's popover inside a toolbar lands on top of the button without an arrow.
struct TMListPickerPresenter: UIViewRepresentable {
    @Binding var isPresented: Bool
    let tagId: Int32

    /// Marks a presented picker, so only one is ever up.
    final class Host: UIHostingController<TMListPicker> {}

    final class Coordinator: NSObject, UIAdaptivePresentationControllerDelegate, UIPopoverPresentationControllerDelegate {
        var dismissed: () -> Void = {}
        func presentationControllerDidDismiss(_ presentationController: UIPresentationController) { dismissed() }
        // A popover stays a popover on iPad even in a narrow column.
        func adaptivePresentationStyle(for controller: UIPresentationController,
                                       traitCollection: UITraitCollection) -> UIModalPresentationStyle {
            traitCollection.userInterfaceIdiom == .pad ? .none : .pageSheet
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> TMPresentingAnchor { TMPresentingAnchor() }

    func updateUIView(_ anchor: TMPresentingAnchor, context: Context) {
        let binding = $isPresented
        let coordinator = context.coordinator
        coordinator.dismissed = { [weak anchor] in
            anchor?.forget()
            binding.wrappedValue = false
        }
        let tagId = tagId
        anchor.alreadyUp = { $0 is Host }
        anchor.make = { anchor in
            let picker = Host(rootView: TMListPicker(
                model: TMListPickerModel(tagId: tagId),
                onDone: { [weak anchor] in
                    binding.wrappedValue = false
                    anchor?.wanted = false
                    anchor?.sync()
                }))
            picker.view.backgroundColor = .clear
            if anchor.traitCollection.userInterfaceIdiom == .pad {
                picker.modalPresentationStyle = .popover
                // UIKit sized the navigation controller's list at 340 × 420 and added its bar.
                picker.preferredContentSize = CGSize(width: 340, height: 420 + 63)
                let popover = picker.popoverPresentationController
                popover?.sourceView = anchor
                popover?.sourceRect = anchor.bounds
                popover?.permittedArrowDirections = [.up, .down]
                popover?.delegate = coordinator
            } else {
                picker.modalPresentationStyle = .pageSheet
                picker.sheetPresentationController?.detents = [.medium(), .large()]
                picker.sheetPresentationController?.prefersGrabberVisible = true
                picker.presentationController?.delegate = coordinator
            }
            return picker
        }
        anchor.wanted = isPresented
        // Let the bar finish placing this copy before anchoring to it.
        DispatchQueue.main.async { anchor.sync() }
    }
}

/// A picker row laid out as UITableViewCell's value1 style laid it out: the icon
/// centred in its column, the name 48 pt in, the count at the trailing edge
/// (or beside the checkmark accessory).
private struct TMPickerRowLayout: View {
    private let accent = Color(DPAppDelegate.accentColor() ?? .tintColor)
    let symbol: String
    let iconColor: Color
    let name: String
    let count: String?
    let member: Bool

    var body: some View {
        HStack(spacing: 0) {
            Image(systemName: symbol)
                .foregroundStyle(iconColor)
                .frame(width: 40.8)
            Text(name)
                .foregroundStyle(Color(.label))
                .padding(.leading, 7.2)
                .alignmentGuide(.listRowSeparatorLeading) { $0[.leading] + 7.2 }
            Spacer(minLength: 8)
            if let count {
                Text(count).foregroundStyle(Color(.secondaryLabel))
            }
            if member {
                Image(systemName: "checkmark")
                    .fontWeight(.semibold)
                    .foregroundStyle(accent)
                    .padding(.leading, 13)
            }
        }
        .font(.body)
        .padding(.trailing, member ? 18.67 : 10.33)
        .frame(minHeight: 44)
        .contentShape(Rectangle())
    }
}

/// A navigation bar Done button in UIKit's .done style: the prominent accent glass on iOS 26.
struct TMDoneButton: View {
    private let accent = Color(DPAppDelegate.accentColor() ?? .tintColor)
    let action: () -> Void

    var body: some View {
        if #available(iOS 26.0, *) {
            Button("Done", action: action)
                .buttonStyle(.glassProminent)
                .tint(accent)
        } else {
            Button("Done", action: action).fontWeight(.semibold)
        }
    }
}

/// On iPhone UITableView's inset groups sat 4.67 pt further in than SwiftUI's.
private struct TMInsetGroupMargins: ViewModifier {
    @Environment(\.horizontalSizeClass) private var sizeClass

    func body(content: Content) -> some View {
        if sizeClass == .compact {
            content.contentMargins(.horizontal, 25.33, for: .scrollContent)
        } else {
            content
        }
    }
}


/// Lets the popover's or sheet's own glass show through the navigation container.
private struct TMClearNavigationContainer: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 18.0, *) {
            content.containerBackground(.clear, for: .navigation)
        } else {
            content
        }
    }
}
