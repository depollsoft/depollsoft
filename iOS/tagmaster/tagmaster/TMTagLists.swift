//
//  TMTagLists.swift
//  tagmaster
//
//  The registry of a user's tag lists.
//
//  Every list is a key into the `lists` map of the Firestore user document (`users/{uid}`), whose
//  value is the ordered array of tag ids. Two keys are special: `favorite` and `teachable` always
//  exist, cannot be renamed or deleted, and are surfaced at the top level of the app. Every other
//  key is a user-created list whose display name and position live in a sibling `listInfo` map
//  (`listInfo.{key} = { name, order }`). A key with ids but no `listInfo` entry (data written by an
//  older app) is shown under its raw key.
//
//  Keys are stable slugs, so renaming a list never moves its tags and an edit made offline on
//  another device still lands in the right list. Every change posts `.userDataChanged`.
//

import Foundation
import FirebaseFirestore

@objc public enum TMListNameError: Int {
    case none
    case empty
    case tooLong
    case duplicate
    case reserved
}

@objcMembers
public final class TMTagLists: NSObject {
    public static let favoriteKey = "favorite"
    public static let teachableKey = "teachable"
    public static let reservedKeys: Set<String> = [favoriteKey, teachableKey]
    public static let maxNameLength = 60

    /// UserDefaults key of the `[key: [Int]]` dictionary of every list's ids.
    static let listsDefaultsKey = "depollsoft.pitchperfect.lists"
    /// UserDefaults key of the `[key: ["name": String, "order": Int]]` dictionary of custom-list metadata.
    static let infoDefaultsKey = "depollsoft.tagmaster.listInfo"
    /// UserDefaults key of the uid whose account this device's lists belong to.
    static let syncedUidDefaultsKey = "depollsoft.tagmaster.syncedUid"

    /// The signed-in user's document while a Firestore listener is attached; nil when signed out.
    static var userDoc: DocumentReference?

    // MARK: - Reading

    public static func isCustom(_ key: String) -> Bool {
        !reservedKeys.contains(key)
    }

    /// Ordered keys of the user-created lists.
    public static func customKeys() -> [String] {
        let info = storedInfo()
        let lists = storedLists()
        var seen = Set<String>()
        var ordered: [(Int, String, String)] = []
        for (key, entry) in info where isCustom(key) {
            let name = (entry["name"] as? String) ?? key
            let order = (entry["order"] as? Int) ?? Int.max
            ordered.append((order, name.lowercased(), key))
            seen.insert(key)
        }
        for key in lists.keys where isCustom(key) && !seen.contains(key) {
            ordered.append((Int.max, key.lowercased(), key))
            seen.insert(key)
        }
        return ordered.sorted { lhs, rhs in
            if lhs.0 != rhs.0 { return lhs.0 < rhs.0 }
            if lhs.1 != rhs.1 { return lhs.1 < rhs.1 }
            return lhs.2 < rhs.2
        }.map { $0.2 }
    }

    /// Every list key in display order: favorites, teachable, then the custom lists.
    public static func allKeys() -> [String] {
        [favoriteKey, teachableKey] + customKeys()
    }

    /// The user-facing name of any list.
    public static func name(for key: String) -> String {
        switch key {
        case favoriteKey: return "Favorites"
        case teachableKey: return "Teachable Tags"
        default: return (storedInfo()[key]?["name"] as? String) ?? key
        }
    }

    public static func ids(for key: String) -> [Int] {
        (storedLists()[key] as? [Any])?.compactMap { ($0 as? NSNumber)?.intValue } ?? []
    }

    public static func contains(_ tagId: Int, in key: String) -> Bool {
        ids(for: key).contains(tagId)
    }

    /// The keys of every list containing `tagId`, in display order.
    public static func keysContaining(_ tagId: Int) -> [String] {
        allKeys().filter { contains(tagId, in: $0) }
    }

    // MARK: - Editing tags

    public static func setIds(_ ids: [Int], for key: String) {
        setIds(ids, for: key, doSave: true)
    }

    static func setIds(_ ids: [Int], for key: String, doSave: Bool) {
        let old = self.ids(for: key)
        var lists = storedLists()
        lists[key] = ids
        UserDefaults.standard.set(lists, forKey: listsDefaultsKey)
        guard !old.elementsEqual(ids) else { return }
        if doSave, let doc = userDoc {
            doc.setData(["lists": [key: ids]], mergeFields: [FieldPath(["lists", key])])
        }
        NotificationCenter.default.post(name: .userDataChanged, object: nil)
    }

    public static func add(_ tagId: Int, to key: String) {
        var ids = self.ids(for: key)
        guard !ids.contains(tagId) else { return }
        ids.append(tagId)
        setIds(ids, for: key)
    }

    public static func remove(_ tagId: Int, from key: String) {
        let ids = self.ids(for: key).filter { $0 != tagId }
        setIds(ids, for: key)
    }

    /// Toggles membership; returns whether the tag is in the list afterwards.
    @discardableResult
    public static func toggle(_ tagId: Int, in key: String) -> Bool {
        if contains(tagId, in: key) {
            remove(tagId, from: key)
            return false
        }
        add(tagId, to: key)
        return true
    }

    public static func move(in key: String, from: Int, to: Int) {
        var ids = self.ids(for: key)
        guard from != to, ids.indices.contains(from), ids.indices.contains(to) else { return }
        ids.insert(ids.remove(at: from), at: to)
        setIds(ids, for: key)
    }

    // MARK: - Managing lists

    public static func normalizeName(_ name: String) -> String {
        name.components(separatedBy: .whitespacesAndNewlines).filter { !$0.isEmpty }.joined(separator: " ")
    }

    /// Why `name` cannot be used for a list, or `.none` when it can. `excluding` is the list being renamed.
    public static func validateName(_ name: String, excluding key: String?) -> TMListNameError {
        let normalized = normalizeName(name)
        if normalized.isEmpty { return .empty }
        if normalized.count > maxNameLength { return .tooLong }
        let folded = normalized.lowercased()
        if ["favorites", "favorite", "teachable tags", "teachable"].contains(folded) { return .reserved }
        for other in customKeys() where other != key && self.name(for: other).lowercased() == folded {
            return .duplicate
        }
        return .none
    }

    /// A short message for a rejected name, or nil when the name is acceptable.
    public static func nameErrorMessage(_ name: String, excluding key: String?) -> String? {
        switch validateName(name, excluding: key) {
        case .none: return nil
        case .empty: return "Give the list a name."
        case .tooLong: return "Keep the name under \(maxNameLength + 1) characters."
        case .duplicate: return "You already have a list with that name."
        case .reserved: return "That name is used by a built-in list."
        }
    }

    /// Creates a list and returns its key, or nil when the name is rejected.
    public static func createList(named name: String) -> String? {
        let normalized = normalizeName(name)
        guard validateName(normalized, excluding: nil) == .none else { return nil }
        var info = storedInfo()
        let key = newKey(for: normalized, existing: Set(info.keys).union(storedLists().keys))
        info[key] = ["name": normalized, "order": customKeys().count]
        UserDefaults.standard.set(info, forKey: infoDefaultsKey)
        saveInfo(keys: [key])
        NotificationCenter.default.post(name: .userDataChanged, object: nil)
        return key
    }

    @discardableResult
    public static func renameList(_ key: String, to name: String) -> Bool {
        // A list deleted elsewhere while its rename alert was open must not come back.
        guard isCustom(key), customKeys().contains(key) else { return false }
        let normalized = normalizeName(name)
        guard validateName(normalized, excluding: key) == .none else { return false }
        var info = storedInfo()
        if (info[key]?["name"] as? String) == normalized { return true }
        let order = (info[key]?["order"] as? Int) ?? customKeys().firstIndex(of: key) ?? customKeys().count
        info[key] = ["name": normalized, "order": order]
        UserDefaults.standard.set(info, forKey: infoDefaultsKey)
        saveInfo(keys: [key])
        NotificationCenter.default.post(name: .userDataChanged, object: nil)
        return true
    }

    /// Deletes a custom list along with its tags, locally and in the cloud.
    public static func deleteList(_ key: String) {
        guard isCustom(key) else { return }
        var info = storedInfo()
        var lists = storedLists()
        info.removeValue(forKey: key)
        lists.removeValue(forKey: key)
        UserDefaults.standard.set(info, forKey: infoDefaultsKey)
        UserDefaults.standard.set(lists, forKey: listsDefaultsKey)
        if let doc = userDoc {
            doc.setData(
                ["lists": [key: FieldValue.delete()], "listInfo": [key: FieldValue.delete()]],
                mergeFields: [FieldPath(["lists", key]), FieldPath(["listInfo", key])]
            )
        }
        NotificationCenter.default.post(name: .userDataChanged, object: nil)
    }

    /// Moves the custom list at `from` to `to` among the custom lists.
    @discardableResult
    public static func moveList(from: Int, to: Int) -> Bool {
        var keys = customKeys()
        guard from != to, keys.indices.contains(from), keys.indices.contains(to) else { return false }
        keys.insert(keys.remove(at: from), at: to)
        return reorderLists(keys)
    }

    /// Applies a complete new order of the custom lists.
    @discardableResult
    public static func reorderLists(_ keys: [String]) -> Bool {
        let current = customKeys()
        guard keys != current, keys.count == current.count, Set(keys) == Set(current) else { return false }
        var info = storedInfo()
        for (index, key) in keys.enumerated() {
            info[key] = ["name": name(for: key), "order": index]
        }
        UserDefaults.standard.set(info, forKey: infoDefaultsKey)
        saveInfo(keys: keys)
        NotificationCenter.default.post(name: .userDataChanged, object: nil)
        return true
    }

    // MARK: - Cloud sync

    /// Drops every list on this device without touching the cloud. Used when a different
    /// account signs in: the lists here belong to the account that last synced them.
    static func forgetLocal() {
        UserDefaults.standard.removeObject(forKey: listsDefaultsKey)
        UserDefaults.standard.removeObject(forKey: infoDefaultsKey)
        NotificationCenter.default.post(name: .userDataChanged, object: nil)
    }

    /// Records which account this device's lists belong to. Signing in as a different account
    /// than the one that last synced here drops the local lists first, so one user's lists are
    /// never uploaded into another user's brand-new document. Signing out keeps the lists, and
    /// the same account signing back in finds them untouched. Returns whether lists were dropped.
    @discardableResult
    static func prepareLocalLists(forUid uid: String) -> Bool {
        let synced = UserDefaults.standard.string(forKey: syncedUidDefaultsKey)
        UserDefaults.standard.set(uid, forKey: syncedUidDefaultsKey)
        guard let synced, synced != uid else { return false }
        forgetLocal()
        return true
    }

    /// Replaces the local copy with a cloud snapshot. Every list follows the cloud exactly: a
    /// built-in list the document does not mention is empty (Android deletes the field when a
    /// list empties), so signing in never keeps this device's tags in an account that has none.
    /// Returns the ids of every list, for prefetching.
    @discardableResult
    static func applyRemote(lists remoteLists: [String: Any]?, info remoteInfo: [String: Any]?) -> [Int] {
        let oldLists = storedLists()
        let oldInfo = storedInfo()
        var lists: [String: [Int]] = [:]
        for key in reservedKeys {
            lists[key] = ids(from: remoteLists?[key]) ?? []
        }
        for (key, value) in remoteLists ?? [:] where isCustom(key) {
            lists[key] = ids(from: value) ?? []
        }
        var info: [String: [String: Any]] = [:]
        for (key, value) in remoteInfo ?? [:] where isCustom(key) {
            guard let entry = value as? [String: Any] else { continue }
            var stored: [String: Any] = [:]
            if let name = entry["name"] as? String { stored["name"] = name }
            if let order = (entry["order"] as? NSNumber)?.intValue { stored["order"] = order }
            info[key] = stored
        }
        UserDefaults.standard.set(lists, forKey: listsDefaultsKey)
        UserDefaults.standard.set(info, forKey: infoDefaultsKey)
        let changed = !NSDictionary(dictionary: oldLists).isEqual(to: lists)
            || !NSDictionary(dictionary: oldInfo).isEqual(to: info)
        if changed {
            NotificationCenter.default.post(name: .userDataChanged, object: nil)
        }
        return lists.values.flatMap { $0 }
    }

    /// The whole registry in cloud shape, for seeding a brand-new user document.
    static func remotePayload() -> [String: Any] {
        var lists: [String: [Int]] = [:]
        for key in allKeys() {
            lists[key] = ids(for: key)
        }
        var info: [String: Any] = [:]
        for (index, key) in customKeys().enumerated() {
            info[key] = ["name": name(for: key), "order": index]
        }
        return ["lists": lists, "listInfo": info]
    }

    // MARK: - Storage

    static func storedLists() -> [String: Any] {
        UserDefaults.standard.dictionary(forKey: listsDefaultsKey) ?? [:]
    }

    static func storedInfo() -> [String: [String: Any]] {
        (UserDefaults.standard.dictionary(forKey: infoDefaultsKey) as? [String: [String: Any]]) ?? [:]
    }

    private static func ids(from value: Any?) -> [Int]? {
        (value as? [Any])?.compactMap { ($0 as? NSNumber)?.intValue }
    }

    private static func saveInfo(keys: [String]) {
        guard let doc = userDoc else { return }
        let info = storedInfo()
        var payload: [String: Any] = [:]
        var paths: [Any] = []
        for key in keys {
            guard let entry = info[key] else { continue }
            payload[key] = ["name": entry["name"] ?? key, "order": entry["order"] ?? Int.max]
            paths.append(FieldPath(["listInfo", key]))
        }
        guard !paths.isEmpty else { return }
        doc.setData(["listInfo": payload], mergeFields: paths)
    }

    static func newKey(for name: String, existing: Set<String>) -> String {
        var slug = name.lowercased()
            .unicodeScalars
            .map { CharacterSet.alphanumerics.contains($0) && $0.isASCII ? String($0) : "-" }
            .joined()
        while slug.contains("--") { slug = slug.replacingOccurrences(of: "--", with: "-") }
        slug = slug.trimmingCharacters(in: CharacterSet(charactersIn: "-"))
        slug = String(slug.prefix(24)).trimmingCharacters(in: CharacterSet(charactersIn: "-"))
        if slug.isEmpty { slug = "list" }
        let alphabet = Array("abcdefghijklmnopqrstuvwxyz0123456789")
        while true {
            let suffix = String((0..<4).map { _ in alphabet.randomElement()! })
            let key = "\(slug)-\(suffix)"
            if !reservedKeys.contains(key) && !existing.contains(key) { return key }
        }
    }
}
