//
//  DPSongsModel.swift
//  pitchperfect
//
//  Created by David Poll on 8/2/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import FirebaseAuth
import FirebaseFirestore

public extension Notification.Name {
    static let songsChanged = Notification.Name("pitchPerfect.songsChanged")
}

@objc public class DPSongsModel: NSObject {
    // swiftlint:disable:next identifier_name
    static let SONGS_KEY_OLD = "depollsoft.pitchperfect.Songs"
    // swiftlint:disable:next identifier_name
    static let SONG_LISTS_KEY = "depollsoft.pitchperfect.SongLists"
    @objc public static let songsChangedNotificationName = Notification.Name.songsChanged
    @objc public static let sharedInstance = DPSongsModel()
    
    private var userDoc: DocumentReference?
    private var allListeners: [ListenerRegistration] = []
    
    public override init() {
        super.init()
        if let serializedSongs = UserDefaults.standard.dictionary(forKey: DPSongsModel.SONGS_KEY_OLD) {
            let songs = DPJsonSerializer.deserializeDictionary(serializedSongs) as? [DPPitchedSong] ?? []
            self.songLists["default"] = DPSongList(id: "default")
            self.defaultSongList.name = "Default"
            self.defaultSongList.songs = songs
            self.storeAll()
            UserDefaults.standard.removeObject(forKey: DPSongsModel.SONGS_KEY_OLD)
        } else if let serializedLists = UserDefaults.standard.dictionary(forKey: DPSongsModel.SONG_LISTS_KEY) {
            for (id, serializedList) in serializedLists {
                guard let castList = serializedList as? [String: Any] else {
                    continue
                }
                let name = castList["name"] as? String ?? id
                let serializedSongs = castList["songs"] as? [[AnyHashable: Any]] ?? []
                let songs = serializedSongs.compactMap {
                    DPJsonSerializer.deserializeDictionary($0) as? DPPitchedSong
                }
                let songList = DPSongList(id: id)
                songList.name = name
                songList.songs = songs
                self.songLists[id] = songList
            }
        } else {
            self.songLists["default"] = DPSongList(id: "default")
            self.defaultSongList.name = "Default"
        }
        if self.songLists["default"] == nil {
            self.songLists["default"] = DPSongList(id: "default")
            self.defaultSongList.name = "Default"
        }
    }
    
    @objc public func attachToFirestore(store: Bool = false) {
        userDoc = Firestore.firestore().document("/users/\(Auth.auth().currentUser!.uid)")
        for (_, list) in self.songLists {
            list.setParent(userRef: userDoc!)
        }
        if store {
            self.storeAll()
        }
        listenForSongLists()
    }
    
    private func listenForSongLists() {
        allListeners.append(userDoc!.collection("songLists").addSnapshotListener({ (snapshot, error) in
            if error != nil {
                return
            }
            for change in snapshot!.documentChanges {
                switch change.type {
                case .added:
                    if self.songLists[change.document.documentID] != nil {
                        self.songLists[change.document.documentID]?.restore(snapshot: change.document)
                    } else {
                        self.songLists[change.document.documentID] = DPSongList(snapshot: change.document)
                    }
                case .modified:
                    self.songLists[change.document.documentID]?.restore(snapshot: change.document)
                case .removed:
                    self.removeSongList(forKey: change.document.documentID)
                }
            }
            self.storeAll()
        }))
    }
    
    @objc public func detachFromFirestore() {
        userDoc = nil
        for listener in allListeners {
            listener.remove()
        }
        allListeners = []
    }
    
    @objc public func removeSongList(forKey: String) {
        self.songLists.removeValue(forKey:forKey)
        var dict: [String: Any] = UserDefaults.standard.dictionary(forKey: DPSongsModel.SONG_LISTS_KEY) ?? [:]
        dict.removeValue(forKey: forKey)
        UserDefaults.standard.set(dict, forKey: DPSongsModel.SONG_LISTS_KEY)
    }
    
    @objc public var songLists: [String: DPSongList] = [:] {
        didSet {
            NotificationCenter.default.post(name: .songsChanged, object: self)
        }
    }
    
    @objc public var defaultSongList: DPSongList {
        get {
            return self.songLists["default"]!
        }
    }
    
    @objc public func storeAll() {
        for songList in self.songLists {
            songList.value.storeValue()
        }
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
    
    public let id: String
    public var name: String {
        didSet {
            NotificationCenter.default.post(name: .songsChanged, object: self)
        }
    }
    @objc public var songs: [DPPitchedSong] = [] {
        didSet {
            NotificationCenter.default.post(name: .songsChanged, object: self)
        }
    }
    
    func restore(snapshot: DocumentSnapshot) {
        self.name = snapshot.get("name") as? String ?? self.name
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
    
    @objc public func storeValue() {
        var dict: [String: Any] = UserDefaults.standard.dictionary(forKey: DPSongsModel.SONG_LISTS_KEY) ?? [:]
        let serializedSongs = self.songs.map { DPJsonSerializer.serialize($0) }
        dict[self.id] = ["name": self.name, "songs": serializedSongs]
        UserDefaults.standard.setValue(dict, forKey: DPSongsModel.SONG_LISTS_KEY)
        self.reference?.setData([
            "name": self.name,
            "songs": serializedSongs
        ], merge: true)
    }
}
