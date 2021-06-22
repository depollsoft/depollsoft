//
//  DPSongsModel.swift
//  pitchperfect
//
//  Created by David Poll on 8/2/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import Firebase

public extension Notification.Name {
    static let songsChanged = Notification.Name("pitchPerfect.songsChanged")
}

@objc public class DPSongsModel: NSObject {
    @objc public static let songsChangedNotificationName = Notification.Name.songsChanged
    @objc public static let sharedInstance = DPSongsModel()
    
    private var userDoc: DocumentReference?
    private var allListeners: [ListenerRegistration] = []
    
    @objc public func attachToFirestore(store: Bool = false) {
        userDoc = Firestore.firestore().document("/users/\(Auth.auth().currentUser!.uid)")
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
                    self.songLists[change.document.documentID] = DPSongList(snapshot: change.document)
                case .modified:
                    self.songLists[change.document.documentID]?.restore(snapshot: change.document)
                case .removed:
                    self.songLists.removeValue(forKey: change.document.documentID)
                }
            }
        }))
    }
    
    @objc public func detachFromFirestore() {
        userDoc = nil
        for listener in allListeners {
            listener.remove()
        }
        allListeners = []
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
    private let reference: DocumentReference;
    
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
        self.name = snapshot.get("name") as! String
        self.songs = DPJsonSerializer.deserializeDictionary((snapshot.get("songs") as! [AnyHashable: Any])) as! [DPPitchedSong]
        
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
    
    @objc public func storeValue() {
        self.reference.setData([
            "name": self.name,
            "songs": DPJsonSerializer.serialize(self.songs) ?? []
        ], merge: true)
    }
}
