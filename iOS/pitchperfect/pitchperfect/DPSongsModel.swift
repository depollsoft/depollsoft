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
    @objc public static let sharedInstance = DPSongsModel()
    
    private var userDoc: DocumentReference?
    private var allListeners: [ListenerRegistration] = []
    private var songListeners: [String: ListenerRegistration] = [:]
    
    @objc public func attachToFirestore() {
        userDoc = Firestore.firestore().document("/users/\(Auth.auth().currentUser!.uid)")
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
                    self.songLists[change.document.documentID] = DPSongList(id: change.document.documentID, name: change.document.get("name") as! String)
                    self.listenForSongs(songListRef: change.document.reference)
                case .modified:
                    self.songLists[change.document.documentID]?.name = change.document.get("name") as! String
                case .removed:
                    self.songLists.removeValue(forKey: change.document.documentID)
                    let l = self.songListeners.removeValue(forKey: change.document.documentID)
                    l?.remove()
                @unknown default:
                    fatalError()
                }
            }
        }))
    }
    
    private func listenForSongs(songListRef: DocumentReference) {
        _ = songLists[songListRef.documentID]
        let listener = songListRef.collection("songs").addSnapshotListener { (snapshot, error) in
            for change in snapshot!.documentChanges {
                switch change.type {
                case .added: break
                case .removed: break
                case .modified: break
                @unknown default:
                    fatalError()
                }
            }
        }
        allListeners.append(listener)
        songListeners[songListRef.documentID] = listener
    }
    
    @objc public func detachFromFirestore() {
        userDoc = nil
        for listener in allListeners {
            listener.remove()
        }
        allListeners = []
        songListeners = [:]
    }
    
    @objc public var songLists: [String: DPSongList] = [:] {
        didSet {
            NotificationCenter.default.post(name: .songsChanged, object: self)
        }
    }
}

@objc public class DPSongList: NSObject {
    public init(id: String, name: String) {
        self.name = name
        self.id = id
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
    
    @objc public func storeValue() {
        
    }
}
