//
//  ListModel.swift
//  tagmaster
//
//  Created by David Poll on 2025.
//  Copyright © 2025 DepollSoft. All rights reserved.
//

import Foundation
import Firebase

/// Manages a named list of tag IDs, persisted to UserDefaults and synced to Firestore.
/// Mirrors the Android `ListModel.kt` pattern.
@objc class ListModel: NSObject {

    // MARK: - Constants

    static let listsKey = "tagmaster.lists"
    private static let oldListsKey = "depollsoft.pitchperfect.lists"

    // MARK: - Static State

    private static var modelInstances: [String: ListModel] = [:]
    private static var shouldStore = true
    private static var registration: ListenerRegistration?

    /// All list data keyed by list name, backed by UserDefaults.
    static var preferences: [String: [Int]] = {
        migrateOldListsKey()
        return UserDefaults.standard.dictionary(forKey: listsKey) as? [String: [Int]] ?? [:]
    }()

    // MARK: - Instance Properties

    let listName: String
    private(set) var ids: [Int]

    // MARK: - Init

    private init(listName: String) {
        self.listName = listName
        self.ids = ListModel.preferences[listName] ?? []
        super.init()
    }

    // MARK: - Factory

    /// Returns a cached `ListModel` for the given list name, creating one if needed.
    @objc static func get(_ listName: String) -> ListModel {
        if let existing = modelInstances[listName] {
            return existing
        }
        let model = ListModel(listName: listName)
        modelInstances[listName] = model
        return model
    }

    // MARK: - Mutators

    @objc func add(_ id: Int) {
        guard !ids.contains(id) else { return }
        ids.append(id)
        storeValue()
    }

    @objc func remove(_ id: Int) {
        ids.removeAll { $0 == id }
        storeValue()
    }

    @objc func contains(_ id: Int) -> Bool {
        return ids.contains(id)
    }

    @objc func moveUp(_ id: Int) {
        guard let index = ids.firstIndex(of: id), index > 0 else { return }
        ids.remove(at: index)
        ids.insert(id, at: index - 1)
        storeValue()
    }

    @objc func moveDown(_ id: Int) {
        guard let index = ids.firstIndex(of: id), index < ids.count - 1 else { return }
        ids.remove(at: index)
        ids.insert(id, at: index + 1)
        storeValue()
    }

    @objc func canMoveUp(_ id: Int) -> Bool {
        guard let index = ids.firstIndex(of: id) else { return false }
        return index > 0
    }

    @objc func canMoveDown(_ id: Int) -> Bool {
        guard let index = ids.firstIndex(of: id) else { return false }
        return index < ids.count - 1
    }

    @objc func reset() {
        ids.removeAll()
        storeValue()
    }

    /// Replaces the entire list. Used during Firestore sync.
    func setIds(_ newIds: [Int], notify: Bool = true) {
        ids = newIds
        if ids.isEmpty {
            ListModel.preferences.removeValue(forKey: listName)
        } else {
            ListModel.preferences[listName] = ids
        }
        if notify {
            NotificationCenter.default.post(name: .userDataChanged, object: nil)
        }
    }

    // MARK: - Persistence

    private func storeValue() {
        if ids.isEmpty {
            ListModel.preferences.removeValue(forKey: listName)
        } else {
            ListModel.preferences[listName] = ids
        }
        ListModel.storeToDefaults()
        NotificationCenter.default.post(name: .userDataChanged, object: nil)

        guard ListModel.shouldStore else { return }

        guard let user = Auth.auth().currentUser else { return }
        let userDoc = Firestore.firestore().document("users/\(user.uid)")
        let value: Any = ids.isEmpty ? FieldValue.delete() : ids
        userDoc.setData(
            ["lists": [listName: value]],
            mergeFields: ["lists.\(listName)"]
        )
    }

    private static func storeToDefaults() {
        UserDefaults.standard.set(preferences, forKey: listsKey)
    }

    // MARK: - Migration

    /// Migrates from the old PitchPerfect lists key to the new TagMaster key.
    private static func migrateOldListsKey() {
        if UserDefaults.standard.dictionary(forKey: listsKey) != nil {
            // New key already exists; clean up old key if present
            UserDefaults.standard.removeObject(forKey: oldListsKey)
            return
        }
        if let oldData = UserDefaults.standard.dictionary(forKey: oldListsKey) as? [String: [Int]] {
            UserDefaults.standard.set(oldData, forKey: listsKey)
            UserDefaults.standard.removeObject(forKey: oldListsKey)
        }
    }

    // MARK: - Delete

    /// Deletes a list from local preferences. Firestore deletion is handled by CustomListsModel.
    static func deleteList(_ key: String) {
        let model = modelInstances[key]
        model?.ids.removeAll()
        preferences.removeValue(forKey: key)
        modelInstances.removeValue(forKey: key)
        storeToDefaults()
    }

    // MARK: - Test Mode

    @objc static func setTestMode(_ enabled: Bool) {
        shouldStore = !enabled
    }

    /// Clears cached model instances and preferences. For testing only.
    static func resetModelInstances() {
        modelInstances.removeAll()
        preferences.removeAll()
    }

    // MARK: - Firestore Sync

    /// Sets up a Firestore snapshot listener for the current user's document.
    @objc static func connectToFirestore() {
        registration?.remove()
        guard let user = Auth.auth().currentUser else { return }
        let userDoc = Firestore.firestore().document("users/\(user.uid)")
        registration = userDoc.addSnapshotListener { snapshot, error in
            if let error = error {
                print(error)
                return
            }
            guard let snapshot = snapshot else { return }

            if !snapshot.exists {
                // No existing user document — initialize it
                toFirestore()
                return
            }

            fromFirestore(snapshot.get("lists") as? [String: Any] ?? [:])
            CustomListsModel.fromFirestore(snapshot.get("listMeta") as? [String: Any])
        }
    }

    /// Applies Firestore list data to local state without writing back.
    static func fromFirestore(_ data: [String: Any]) {
        shouldStore = false
        defer {
            shouldStore = true
            storeToDefaults()
        }

        // Remove lists that are no longer in Firestore data
        let remoteKeys = Set(data.keys)
        for key in preferences.keys where !remoteKeys.contains(key) {
            let model = ListModel.get(key)
            model.ids.removeAll()
            preferences.removeValue(forKey: key)
        }

        // Update/add lists from Firestore data
        for (key, value) in data {
            let model = ListModel.get(key)
            let newIds: [Int]
            if let intArray = value as? [Int] {
                newIds = intArray
            } else if let numArray = value as? [NSNumber] {
                newIds = numArray.map { $0.intValue }
            } else {
                newIds = []
            }
            if newIds != model.ids {
                model.ids = newIds
                if newIds.isEmpty {
                    preferences.removeValue(forKey: key)
                } else {
                    preferences[key] = newIds
                }
            }
        }

        NotificationCenter.default.post(name: .userDataChanged, object: nil)
    }

    /// Writes all list data (and list metadata) to Firestore.
    static func toFirestore() {
        guard let user = Auth.auth().currentUser else { return }
        let userDoc = Firestore.firestore().document("users/\(user.uid)")
        var data: [String: Any] = ["lists": preferences]
        let listMetaMap = CustomListsModel.toFirestoreMap()
        if !listMetaMap.isEmpty {
            data["listMeta"] = listMetaMap
        }
        userDoc.setData(data, merge: true)
    }

    /// Removes the Firestore snapshot listener.
    @objc static func disconnectFromFirestore() {
        registration?.remove()
        registration = nil
    }
}
