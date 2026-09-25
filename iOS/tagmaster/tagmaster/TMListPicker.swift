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
    var namingNewList = false
    var newListName = ""
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

    static let exampleHint = "For example “Easy tags” or “High and lows”"

    func beginNewList() {
        newListName = ""
        namingNewList = true
    }

    /// Why the typed name cannot be used, or nil when it can.
    var newListProblem: String? { TMTagLists.nameErrorMessage(newListName, excluding: nil) }

    /// Nothing typed yet is not a mistake worth reporting; the hint stays.
    var newListMessage: String {
        TMTagLists.normalizeName(newListName).isEmpty ? TMListPickerModel.exampleHint : (newListProblem ?? TMListPickerModel.exampleHint)
    }

    func createNewList() {
        guard TMTagLists.validateName(newListName, excluding: nil) == .none else { return }
        let name = TMTagLists.normalizeName(newListName)
        guard let key = TMTagLists.createList(named: name) else { return }
        TMTagLists.add(Int(tagId), to: key)
        UIAccessibility.post(notification: .announcement, argument: "Added to \(name)")
    }
}

struct TMListPicker: View {
    /// The rows set their accent explicitly, as the UIKit cells did.
    private let accent = Color(DPAppDelegate.accentColor() ?? .tintColor)
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State var model: TMListPickerModel
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
            .modifier(TMInsetGroupMargins())
            .focusEffectDisabled()
            .accessibilityIdentifier("picker.table")
            .navigationTitle("Add to list")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    TMDoneButton { dismiss() }
                        .accessibilityIdentifier("picker.done")
                }
            }
            // The grabber belongs to the iPhone sheet, not the iPad popover.
            .presentationDragIndicator(sizeClass == .compact ? .visible : .hidden)
            .alert("New list", isPresented: $model.namingNewList) {
                TextField("List name", text: $model.newListName)
                    .textInputAutocapitalization(.sentences)
                    .submitLabel(.done)
                    .accessibilityIdentifier("list.name.field")
                Button("Cancel", role: .cancel) {}
                Button("Create", action: model.createNewList)
                    .disabled(model.newListProblem != nil)
                    .keyboardShortcut(.defaultAction)
            } message: {
                Text(model.newListMessage)
            }
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
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(TMTagLists.name(for: key))
        .accessibilityValue(count)
        .accessibilityAddTraits(member ? [.isButton, .isSelected] : .isButton)
        .accessibilityIdentifier("picker.row.\(key)")
    }
}

extension View {
    /// Presents the list picker for the detail's tag while `source` is the one that opened it.
    func tmListPicker(model: TagDetailModel, source: TMPickerSource) -> some View {
        popover(isPresented: Binding(get: { model.pickerPresented(from: source) },
                                     set: { if !$0, model.pickerSource == source { model.pickerSource = nil } }),
                attachmentAnchor: .rect(.bounds), arrowEdge: .top) {
            TMListPicker(model: TMListPickerModel(tagId: model.tagId))
                .frame(idealWidth: 340, idealHeight: 420)
                .presentationCompactAdaptation(.sheet)
                .presentationDetents([.medium, .large])
        }
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
