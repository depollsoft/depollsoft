//
//  CustomListsModel.swift
//  tagmaster
//
//  Created by David Poll on 2025.
//  Copyright © 2025 DepollSoft. All rights reserved.
//

import Foundation
import Firebase

/// Metadata for a tag list. Built-in lists (favorite, teachable) have fixed
/// display names and cannot be renamed or deleted.
struct ListMetadata {
    static let favoriteKey = "favorite"
    static let teachableKey = "teachable"

    static let favorite = ListMetadata(key: favoriteKey, name: "Favorites", order: 0, isBuiltIn: true)
    static let teachable = ListMetadata(key: teachableKey, name: "Teachable Tags", order: 1, isBuiltIn: true)

    let key: String
    let name: String
    let order: Int
    let isBuiltIn: Bool
}

/// Manages metadata for custom tag lists (name, order).
/// Built-in lists (favorite, teachable) have fixed metadata.
/// Custom list metadata is stored in UserDefaults and synced to Firestore.
/// Mirrors the Android `CustomListsModel` pattern.
class CustomListsModel {

    private static let listMetaKey = "tagmaster.listMeta"

    private static var metadata: [String: ListMetadata] = {
        guard let stored = UserDefaults.standard.dictionary(forKey: listMetaKey) as? [String: [String: Any]] else {
            return [:]
        }
        var result: [String: ListMetadata] = [:]
        for (key, map) in stored {
            let name = map["name"] as? String ?? key
            let order = (map["order"] as? NSNumber)?.intValue ?? 0
            result[key] = ListMetadata(key: key, name: name, order: order, isBuiltIn: false)
        }
        return result
    }()

    private static var shouldStore = true

    // MARK: - Test Mode

    static func setTestMode(_ enabled: Bool) {
        shouldStore = !enabled
    }

    static func resetForTesting() {
        metadata = [:]
    }

    // MARK: - Queries

    /// Returns all lists: built-in first, then custom lists sorted by order.
    static func allLists() -> [ListMetadata] {
        let builtIn = [ListMetadata.favorite, ListMetadata.teachable]
        let custom = metadata.values.sorted { $0.order < $1.order }
        return builtIn + custom
    }

    /// Returns only custom lists sorted by order.
    static func customLists() -> [ListMetadata] {
        return metadata.values.sorted { $0.order < $1.order }
    }

    /// Gets metadata for a list by key, or nil if not found.
    /// Returns built-in metadata for favorite/teachable keys.
    static func getMetadata(_ key: String) -> ListMetadata? {
        switch key {
        case ListMetadata.favoriteKey:
            return ListMetadata.favorite
        case ListMetadata.teachableKey:
            return ListMetadata.teachable
        default:
            return metadata[key]
        }
    }

    // MARK: - Mutators

    /// Creates a new custom list with the given name. Returns the generated key.
    @discardableResult
    static func createList(_ name: String) -> String {
        let key = UUID().uuidString
        let order = (metadata.values.map { $0.order }.max() ?? 1) + 1
        let meta = ListMetadata(key: key, name: name, order: order, isBuiltIn: false)
        metadata[key] = meta
        // Create the backing ListModel so it's ready to use
        _ = ListModel.get(key)
        storeLocally()
        storeToFirestore(key: key, meta: meta)
        return key
    }

    /// Renames a custom list. No-op for built-in lists.
    static func renameList(_ key: String, newName: String) {
        guard let existing = metadata[key] else { return }
        let updated = ListMetadata(key: key, name: newName, order: existing.order, isBuiltIn: false)
        metadata[key] = updated
        storeLocally()
        storeToFirestore(key: key, meta: updated)
    }

    /// Deletes a custom list and its tag IDs. No-op for built-in lists.
    static func deleteList(_ key: String) {
        guard key != ListMetadata.favoriteKey && key != ListMetadata.teachableKey else { return }
        metadata.removeValue(forKey: key)
        ListModel.deleteList(key)
        storeLocally()
        deleteFromFirestore(key: key)
    }

    // MARK: - Local Persistence

    private static func storeLocally() {
        var toStore: [String: [String: Any]] = [:]
        for (key, meta) in metadata {
            toStore[key] = ["name": meta.name, "order": meta.order]
        }
        UserDefaults.standard.set(toStore, forKey: listMetaKey)
    }

    // MARK: - Firestore

    private static func storeToFirestore(key: String, meta: ListMetadata) {
        guard shouldStore else { return }
        guard let user = Auth.auth().currentUser else { return }
        let userDoc = Firestore.firestore().document("users/\(user.uid)")
        userDoc.setData(
            ["listMeta": [key: ["name": meta.name, "order": meta.order]]],
            mergeFields: ["listMeta.\(key)"]
        )
    }

    private static func deleteFromFirestore(key: String) {
        guard shouldStore else { return }
        guard let user = Auth.auth().currentUser else { return }
        let userDoc = Firestore.firestore().document("users/\(user.uid)")
        userDoc.setData(
            [
                "lists": [key: FieldValue.delete()],
                "listMeta": [key: FieldValue.delete()]
            ],
            mergeFields: ["lists.\(key)", "listMeta.\(key)"]
        )
    }

    /// Syncs metadata from a Firestore snapshot. Gracefully handles nil (missing listMeta).
    static func fromFirestore(_ data: [String: Any]?) {
        guard let data = data else { return }
        shouldStore = false
        defer {
            shouldStore = true
        }
        var newMeta: [String: ListMetadata] = [:]
        for (key, value) in data {
            guard let map = value as? [String: Any] else { continue }
            let name = map["name"] as? String ?? key
            let order = (map["order"] as? NSNumber)?.intValue ?? 0
            newMeta[key] = ListMetadata(key: key, name: name, order: order, isBuiltIn: false)
        }
        metadata = newMeta
        storeLocally()
    }

    /// Returns metadata as a map suitable for Firestore storage.
    static func toFirestoreMap() -> [String: [String: Any]] {
        var result: [String: [String: Any]] = [:]
        for (key, meta) in metadata {
            result[key] = ["name": meta.name, "order": meta.order]
        }
        return result
    }
}
