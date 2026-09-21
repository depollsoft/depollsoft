//
//  DPSongsModel.swift
//  pitchperfect
//
//  Created by David Poll on 8/2/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//
//  The set lists. Each list is one Firestore document
//  `users/{uid}/songLists/{listId}` carrying `name`, `songs` and (new) `order`.
//  `default` is the home list — always present, always first, never deletable —
//  and shows as "My Songs" whatever its legacy stored name says.
//

import Foundation
import FirebaseAuth
import FirebaseFirestore

public extension Notification.Name {
    static let songsChanged = Notification.Name("pitchPerfect.songsChanged")
}

/// Why a set list name cannot be used, checked in the order the design doc lists.
public enum DPSongListNameProblem {
    case empty
    case tooLong
    case reserved
    case duplicate

    public var message: String {
        switch self {
        case .empty: return "Give the set list a name."
        case .tooLong: return "Keep the name under 61 characters."
        case .reserved: return "That name is reserved."
        case .duplicate: return "You already have a set list with that name."
        }
    }
}

/// One source list's songs that the current list does not already have.
public struct DPAddableSongs {
    public let list: DPSongList
    public let songs: [DPPitchedSong]
}

@objc public class DPSongsModel: NSObject {

    static let legacySongsKey = "depollsoft.pitchperfect.Songs"
    static let songListsKey = "depollsoft.pitchperfect.SongLists"
    static let currentListKey = "depollsoft.pitchperfect.CurrentSongList"

    /// The reserved id of the home list, and the name it shows under.
    @objc public static let defaultListId = "default"
    @objc public static let defaultListDisplayName = "My Songs"
    /// The longest name a set list may carry.
    public static let nameLengthLimit = 60

    @objc public static let songsChangedNotificationName = Notification.Name.songsChanged
    @objc public static let sharedInstance = DPSongsModel()

    private var userDoc: DocumentReference?
    private var allListeners: [ListenerRegistration] = []
    /// Raised while a Firestore snapshot is being applied, so nothing written
    /// by the restore path travels back to the server it just came from.
    private var applyingSnapshot = false

    public override init() {
        super.init()

        if let serializedSongs = UserDefaults.standard.dictionary(forKey: DPSongsModel.legacySongsKey),
           let songs = DPJsonSerializer.deserializeDictionary(serializedSongs) as? [DPPitchedSong] {
            let defaultList = DPSongList(id: DPSongsModel.defaultListId)
            defaultList.name = "Default"
            defaultList.songs = songs
            self.songLists[DPSongsModel.defaultListId] = defaultList

            self.storeAll()
            UserDefaults.standard.removeObject(forKey: DPSongsModel.legacySongsKey)
        } else if let serializedLists = UserDefaults.standard.dictionary(forKey: DPSongsModel.songListsKey) {
            for (id, serializedList) in serializedLists {

                guard let castList = serializedList as? [String: Any],
                      let serializedSongs = castList["songs"] as? [[AnyHashable: Any]] else {
                    continue
                }

                let songs = serializedSongs.compactMap {
                    DPJsonSerializer.deserializeDictionary($0) as? DPPitchedSong
                }
                let songList = DPSongList(id: id)
                songList.name = castList["name"] as? String ?? id
                songList.order = (castList["order"] as? NSNumber)?.intValue
                songList.songs = songs
                self.songLists[id] = songList
            }
        }

        if self.songLists[DPSongsModel.defaultListId] == nil {
            let defaultList = DPSongList(id: DPSongsModel.defaultListId)
            defaultList.name = "Default"
            self.songLists[DPSongsModel.defaultListId] = defaultList
        }
    }

    @objc public func attachToFirestore(store: Bool = false) {
        guard let user = Auth.auth().currentUser else { return }
        // A brand-new account keeps what this device has (the login flow uploads it);
        // signing in to an existing account means the account's lists replace the local ones.
        let remoteWins = !store
            && !DPSongsModel.isNewAccount(createdAt: user.metadata.creationDate,
                                          lastSignInAt: user.metadata.lastSignInDate)
        attachToFirestore(userDoc: Firestore.firestore().document("/users/\(user.uid)"),
                          store: store, remoteWins: remoteWins)
    }

    /// Whether the account was created by the sign-in that just happened: Firebase
    /// stamps both times in the same request, so they differ by no more than it took.
    static func isNewAccount(createdAt: Date?, lastSignInAt: Date?) -> Bool {
        guard let createdAt, let lastSignInAt else { return false }
        return abs(lastSignInAt.timeIntervalSince(createdAt)) < 60
    }

    /// Set while a sign-in is waiting for its first server snapshot to decide which lists stay.
    private var pruneOnServerSnapshot = false

    /// The attachment the emulator tests drive: every Firestore path this model
    /// takes hangs off one injected user document.
    @objc public func attachToFirestore(userDoc: DocumentReference, store: Bool = false) {
        attachToFirestore(userDoc: userDoc, store: store, remoteWins: !store)
    }

    /// With `remoteWins`, the first snapshot confirmed by the server decides the
    /// set of lists: any list on this device that the account does not have is
    /// dropped, exactly as the account's default list has always replaced the
    /// local one.
    public func attachToFirestore(userDoc: DocumentReference, store: Bool, remoteWins: Bool) {
        self.userDoc = userDoc
        pruneOnServerSnapshot = remoteWins && !store
        for (_, list) in self.songLists {
            list.setParent(userRef: userDoc)
        }
        if store {
            self.storeAll()
        }
        listenForSongLists()
    }

    /// Remote wins: keeps the default list and every list the account has and
    /// discards the rest, marking them so a stale holder cannot write them back.
    /// Returns the ids that were dropped.
    @discardableResult
    func applyRemoteWins(remoteIds: Set<String>) -> [String] {
        let dropped = songLists.keys.filter { $0 != DPSongsModel.defaultListId && !remoteIds.contains($0) }
        guard !dropped.isEmpty else { return [] }
        for id in dropped {
            songLists[id]?.discardLocally()
            removeSongList(forKey: id)
        }
        if let stored = UserDefaults.standard.string(forKey: DPSongsModel.currentListKey), dropped.contains(stored) {
            UserDefaults.standard.set(DPSongsModel.defaultListId, forKey: DPSongsModel.currentListKey)
        }
        return dropped
    }

    private func listenForSongLists() {
        guard let userDoc else { return }
        // Metadata changes are included so the moment the server confirms the
        // cached view is reported even when no document changed: that
        // confirmation is what settles a sign-in (see `pruneOnServerSnapshot`).
        allListeners.append(userDoc.collection("songLists").addSnapshotListener(includeMetadataChanges: true) { (snapshot, error) in
            if error != nil {
                return
            }
            let settlesSignIn = self.pruneOnServerSnapshot && snapshot.map { !$0.metadata.isFromCache } == true
            if (snapshot?.documentChanges.isEmpty ?? true) && !settlesSignIn {
                return
            }
            self.applyingSnapshot = true
            for change in snapshot!.documentChanges {
                switch change.type {
                case .added:
                    if self.songLists[change.document.documentID] != nil {
                        self.songLists[change.document.documentID]?.restore(snapshot: change.document)
                    } else {
                        let restored = DPSongList(snapshot: change.document)
                        self.songLists[change.document.documentID] = restored
                    }
                case .modified:
                    self.songLists[change.document.documentID]?.restore(snapshot: change.document)
                case .removed:
                    // Flagged first: an editor still holding the list would otherwise
                    // store it back and recreate the document the other device deleted.
                    self.songLists[change.document.documentID]?.discardLocally()
                    self.removeSongList(forKey: change.document.documentID)
                }
            }
            // The first server-confirmed snapshot of a sign-in settles which lists
            // exist here. A cached (possibly empty) snapshot must not: it says
            // nothing about the account.
            if self.pruneOnServerSnapshot, let snapshot, !snapshot.metadata.isFromCache {
                self.pruneOnServerSnapshot = false
                self.applyRemoteWins(remoteIds: Set(snapshot.documents.map(\.documentID)))
            }
            // A list deleted above is gone from `songLists`, so this only
            // rewrites what survived — locally, never back to the server.
            self.storeAll()
            self.applyingSnapshot = false
            self.postChanged()
        })
    }

    @objc public func detachFromFirestore() {
        userDoc = nil
        pruneOnServerSnapshot = false
        for listener in allListeners {
            listener.remove()
        }
        allListeners = []
    }

    @objc public func removeSongList(forKey: String) {
        self.songLists.removeValue(forKey:forKey)
        var dict: [String: Any] = UserDefaults.standard.dictionary(forKey: DPSongsModel.songListsKey) ?? [:]
        dict.removeValue(forKey: forKey)
        UserDefaults.standard.set(dict, forKey: DPSongsModel.songListsKey)
    }

    @objc public var songLists: [String: DPSongList] = [:] {
        didSet {
            NotificationCenter.default.post(name: .songsChanged, object: self)
        }
    }

    /// My Songs. Always present: a list deleted elsewhere comes straight back.
    @objc public var defaultSongList: DPSongList {
        get {
            if let existing = self.songLists[DPSongsModel.defaultListId] {
                return existing
            }
            let recreated = DPSongList(id: DPSongsModel.defaultListId)
            recreated.name = "Default"
            if let userDoc {
                recreated.setParent(userRef: userDoc)
            }
            self.songLists[DPSongsModel.defaultListId] = recreated
            return recreated
        }
    }

    @objc public func storeAll() {
        for songList in self.songLists {
            songList.value.store(remote: !applyingSnapshot)
        }
    }

    private func postChanged() {
        NotificationCenter.default.post(name: .songsChanged, object: self)
    }

    // MARK: - The current list

    /// The list the Songs tab is showing: a per-device choice, never synced.
    /// A stored id that names no list falls back to `default`.
    @objc public var currentListId: String {
        get {
            let stored = UserDefaults.standard.string(forKey: DPSongsModel.currentListKey)
                ?? DPSongsModel.defaultListId
            return songLists[stored] != nil ? stored : DPSongsModel.defaultListId
        }
        set {
            let target = songLists[newValue] != nil ? newValue : DPSongsModel.defaultListId
            UserDefaults.standard.set(target, forKey: DPSongsModel.currentListKey)
            postChanged()
        }
    }

    @objc public var currentList: DPSongList {
        songLists[currentListId] ?? defaultSongList
    }

    // MARK: - Names and ordering

    /// My Songs first; then custom lists by `order` ascending; lists with no
    /// `order` after all ordered ones, by display name.
    @objc public var orderedLists: [DPSongList] {
        let home = defaultSongList
        let custom = songLists.values.filter { $0.id != DPSongsModel.defaultListId }
        let byName: (DPSongList, DPSongList) -> Bool = { left, right in
            self.displayName(for: left).localizedCaseInsensitiveCompare(self.displayName(for: right))
                == .orderedAscending
        }
        let ordered = custom.filter { $0.order != nil }.sorted { left, right in
            let leftOrder = left.order ?? 0
            let rightOrder = right.order ?? 0
            return leftOrder == rightOrder ? byName(left, right) : leftOrder < rightOrder
        }
        let unordered = custom.filter { $0.order == nil }.sorted(by: byName)
        return [home] + ordered + unordered
    }

    /// The home list shows as "My Songs" whatever its legacy stored name says.
    @objc public func displayName(for list: DPSongList) -> String {
        if list.id == DPSongsModel.defaultListId {
            let trimmed = list.name.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.isEmpty || trimmed.caseInsensitiveCompare("Default") == .orderedSame {
                return DPSongsModel.defaultListDisplayName
            }
            return list.name
        }
        return list.name.isEmpty ? list.id : list.name
    }

    /// Runs of whitespace collapse to one space, and the ends are trimmed.
    @objc public static func normalizeName(_ raw: String) -> String {
        raw.split(whereSeparator: { $0.isWhitespace || $0.isNewline })
            .joined(separator: " ")
    }

    public func validateName(_ raw: String, excluding: String? = nil) -> DPSongListNameProblem? {
        let name = DPSongsModel.normalizeName(raw)
        if name.isEmpty { return .empty }
        if name.count > DPSongsModel.nameLengthLimit { return .tooLong }
        if name.caseInsensitiveCompare("default") == .orderedSame { return .reserved }
        for list in songLists.values where list.id != excluding {
            if displayName(for: list).caseInsensitiveCompare(name) == .orderedSame {
                return .duplicate
            }
        }
        return nil
    }

    /// The one line a name prompt shows while the typed name cannot be used.
    @objc public func nameErrorMessage(_ raw: String, excluding: String?) -> String? {
        validateName(raw, excluding: excluding)?.message
    }

    /// A stable slug key made from the name plus four random base-36 characters.
    public static func makeListId(for name: String, existing: Set<String>) -> String {
        var slug = ""
        var pendingSeparator = false
        for character in name.lowercased() {
            if character.isASCII, character.isLetter || character.isNumber {
                if pendingSeparator, !slug.isEmpty { slug.append("-") }
                pendingSeparator = false
                slug.append(character)
            } else {
                pendingSeparator = true
            }
        }
        if slug.count > 40 { slug = String(slug.prefix(40)) }
        while slug.hasSuffix("-") { slug.removeLast() }
        let base = slug.isEmpty ? "list" : slug
        let alphabet = Array("abcdefghijklmnopqrstuvwxyz0123456789")
        var candidate: String
        repeat {
            candidate = base + "-" + String((0..<4).map { _ in alphabet.randomElement()! })
        } while candidate == DPSongsModel.defaultListId || existing.contains(candidate)
        return candidate
    }

    // MARK: - List management

    private var customLists: [DPSongList] {
        orderedLists.filter { $0.id != DPSongsModel.defaultListId }
    }

    /// Creates `{name, songs: [], order: <count of custom lists>}`.
    @discardableResult
    @objc(createListNamed:) public func createList(named raw: String) -> DPSongList? {
        let name = DPSongsModel.normalizeName(raw)
        guard validateName(name) == nil else { return nil }
        return insertList(named: name, songs: [])
    }

    @discardableResult
    @objc public func renameList(_ list: DPSongList, to raw: String) -> Bool {
        let name = DPSongsModel.normalizeName(raw)
        guard validateName(name, excluding: list.id) == nil else { return false }
        list.name = name
        list.storeValue()
        postChanged()
        return true
    }

    /// A new list named "<name> copy" (then "copy 2", "copy 3" …) holding deep
    /// copies of every song with fresh song ids.
    @discardableResult
    @objc public func duplicateList(_ list: DPSongList) -> DPSongList? {
        let copy = insertList(named: copyName(of: displayName(for: list)),
                              songs: list.songs.map(DPSongsModel.copy(of:)))
        return copy
    }

    /// Deletes the document, then the local entry. Never allowed for `default`.
    @discardableResult
    @objc public func deleteList(_ list: DPSongList) -> Bool {
        guard list.id != DPSongsModel.defaultListId else { return false }
        list.deleteRemote()
        let wasCurrent = UserDefaults.standard.string(forKey: DPSongsModel.currentListKey) == list.id
        removeSongList(forKey: list.id)
        if wasCurrent {
            currentListId = DPSongsModel.defaultListId
        } else {
            postChanged()
        }
        return true
    }

    /// Rewrites `order` on every custom list (0, 1, 2 …) so the sequence is dense.
    @discardableResult
    @objc public func reorderLists(_ ids: [String]) -> Bool {
        var sequence = ids.filter { $0 != DPSongsModel.defaultListId && songLists[$0] != nil }
        for list in customLists where !sequence.contains(list.id) {
            sequence.append(list.id)
        }
        for (index, id) in sequence.enumerated() {
            guard let list = songLists[id] else { continue }
            list.order = index
            list.storeValue()
        }
        postChanged()
        return !sequence.isEmpty
    }

    /// Empties My Songs and deletes every other set list.
    @objc public func clearAll() {
        for list in customLists {
            list.deleteRemote()
            removeSongList(forKey: list.id)
        }
        defaultSongList.songs = []
        defaultSongList.storeValue()
        currentListId = DPSongsModel.defaultListId
    }

    // MARK: - Moving songs between lists

    /// Every other list's songs the target does not already have, grouped by
    /// source list in list order. A song counts as present when its title
    /// matches case-insensitively and its key is the same.
    public func addableSongs(for target: DPSongList) -> [DPAddableSongs] {
        let present = Set(target.songs.map(DPSongsModel.fingerprint(of:)))
        var groups: [DPAddableSongs] = []
        for list in orderedLists where list.id != target.id {
            let songs = list.songs.filter { !present.contains(DPSongsModel.fingerprint(of: $0)) }
            if !songs.isEmpty {
                groups.append(DPAddableSongs(list: list, songs: songs))
            }
        }
        return groups
    }

    /// Whether "Add songs from another set list…" has anything to offer.
    @objc public func hasAddableSongs(for target: DPSongList) -> Bool {
        !addableSongs(for: target).isEmpty
    }

    /// Deep copies with fresh song ids, appended in their source order.
    @discardableResult
    @objc public func copySongs(_ songs: [DPPitchedSong], to list: DPSongList) -> [DPPitchedSong] {
        guard !songs.isEmpty else { return [] }
        let copies = songs.map(DPSongsModel.copy(of:))
        list.songs.append(contentsOf: copies)
        list.storeValue()
        return copies
    }

    // MARK: - Helpers

    private func insertList(named name: String, songs: [DPPitchedSong]) -> DPSongList {
        let list = DPSongList(id: DPSongsModel.makeListId(for: name, existing: Set(songLists.keys)))
        list.name = name
        list.order = customLists.count
        list.songs = songs
        if let userDoc {
            list.setParent(userRef: userDoc)
        }
        songLists[list.id] = list
        list.storeValue()
        return list
    }

    /// "<name> copy", then "copy 2", "copy 3" …, each trimmed to fit the name limit.
    private func copyName(of name: String) -> String {
        let taken = Set(songLists.values.map { displayName(for: $0).lowercased() })
        func candidate(_ suffix: String) -> String {
            let room = max(1, DPSongsModel.nameLengthLimit - suffix.count)
            return DPSongsModel.normalizeName(String(name.prefix(room))) + suffix
        }
        var result = candidate(" copy")
        var index = 2
        while taken.contains(result.lowercased()) {
            result = candidate(" copy \(index)")
            index += 1
        }
        return result
    }

    /// Song ids are identity inside one list; a copy is an independent song.
    private static func copy(of song: DPPitchedSong) -> DPPitchedSong {
        let copy = DPPitchedSong()
        copy.name = song.name
        copy.key = song.key
        return copy
    }

    private static func fingerprint(of song: DPPitchedSong) -> String {
        let title = (song.name ?? "").lowercased()
        let key = song.key
        let name = key?.friendlyName() ?? ""
        let accidentals = key.map { Int($0.numAccidentals) } ?? 0
        return "\(title)\u{1F}\(name)\u{1F}\(accidentals)"
    }
}

@objc public class DPSongList: NSObject {
    private var reference: DocumentReference?;

    init(id: String) {
        self.id = id
        self.name = ""
        self.reference = nil
        super.init()
    }

    init(snapshot: DocumentSnapshot) {
        self.id = snapshot.documentID
        self.reference = snapshot.reference
        self.name = ""
        super.init()
        restore(snapshot: snapshot)
    }

    @objc public let id: String
    public var name: String {
        didSet {
            NotificationCenter.default.post(name: .songsChanged, object: self)
        }
    }
    /// Position among custom lists, 0-based. Ignored for `default`, which is
    /// pinned first, and missing on legacy documents.
    public var order: Int?
    @objc public var songs: [DPPitchedSong] = [] {
        didSet {
            NotificationCenter.default.post(name: .songsChanged, object: self)
        }
    }

    func restore(snapshot: DocumentSnapshot) {

        self.name = snapshot.get("name") as? String ?? self.id
        self.order = (snapshot.get("order") as? NSNumber)?.intValue

        let serializedSongs = snapshot.get("songs") as? [[AnyHashable: Any]] ?? []
        self.songs = serializedSongs.compactMap {
            DPJsonSerializer.deserializeDictionary($0) as? DPPitchedSong
        }
    }

    @objc public func addSong(_ song: DPPitchedSong) {
        songs.append(song)
        NotificationCenter.default.post(name: .songsChanged, object: self)
    }

    @objc public func removeSong(_ song: DPPitchedSong) {
        if let index = songs.firstIndex(of: song) {
            songs.remove(at: index)
            NotificationCenter.default.post(name: .songsChanged, object: self)
        }
    }

    @objc public func removeSong(atIndex: Int) {
        songs.remove(at: atIndex)
        NotificationCenter.default.post(name: .songsChanged, object: self)
    }

    @objc public func addSong(_ song: DPPitchedSong, atIndex: Int) {
        songs.insert(song, at: atIndex)
        NotificationCenter.default.post(name: .songsChanged, object: self)
    }

    @objc public func sortSongs() {
        self.songs.sort { left, right in
            return left.name.lowercased() < right.name.lowercased()
        }
        NotificationCenter.default.post(name: .songsChanged, object: self)
    }

    func setParent(userRef: DocumentReference) {
        self.reference = userRef.collection("songLists").document(self.id)
    }

    /// Set once the list has been deleted. A stale holder — an editor still open
    /// on one of its songs, a captured `list` in a completion block — can write
    /// afterwards, and `store` would otherwise recreate both the local entry and
    /// the document.
    @objc public private(set) var isDeleted = false

    /// Deletes this list's document, when there is one, and refuses every later write.
    func deleteRemote() {
        isDeleted = true
        reference?.delete()
    }

    /// Drops the list from this device without touching the server: it was never there.
    func discardLocally() {
        isDeleted = true
    }

    @objc public func storeValue() {
        store(remote: true)
    }

    func store(remote: Bool) {
        guard !isDeleted else { return }
        var dict: [String: Any] = UserDefaults.standard.dictionary(forKey: DPSongsModel.songListsKey) ?? [:]
        let serializedSongs = self.songs.map { DPJsonSerializer.serialize($0) }
        var value: [String: Any] = ["name": self.name, "songs": serializedSongs]
        if let order {
            value["order"] = order
        }
        dict[self.id] = value
        UserDefaults.standard.setValue(dict, forKey: DPSongsModel.songListsKey)
        guard remote else { return }
        self.reference?.setData(value, merge: true)
    }
}
