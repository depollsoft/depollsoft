//
//  AppDelegate.swift
//  tagmaster
//
//  Created by David Poll on 6/7/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import AppTrackingTransparency
import FirebaseAuth
import FirebaseFirestore
import AVKit

public extension Notification.Name {
    static let userDataChanged = Notification.Name("tagmaster.userDataChanged")
}

public extension DPAppDelegate {
    @objc static func setTeachable(_ teachables: [Int]) {
        setTeachable(teachables, doSave: true)
    }

    @objc static func setTeachable(_ teachables: [Int], doSave: Bool) {
        TMTagLists.setIds(teachables, for: TMTagLists.teachableKey, doSave: doSave)
    }

    @objc static func teachable() -> [Int] {
        TMTagLists.ids(for: TMTagLists.teachableKey)
    }

    static func oldTeachable() -> [Int]? {
        guard let array = UserDefaults.standard.array(forKey: "teachable") else {
            return nil
        }
        return array.compactMap { ($0 as? NSNumber)?.intValue }
    }

    @objc static func setFavorites(_ favorites: [Int]) {
        setFavorites(favorites, doSave: true)
    }

    @objc static func setFavorites(_ favorites: [Int], doSave: Bool) {
        TMTagLists.setIds(favorites, for: TMTagLists.favoriteKey, doSave: doSave)
    }

    @objc static func favorites() -> [Int] {
        TMTagLists.ids(for: TMTagLists.favoriteKey)
    }

    static func oldFavorites() -> [Int]? {
        guard let array = UserDefaults.standard.array(forKey: "favorites") else {
            return nil
        }
        return array.compactMap { ($0 as? NSNumber)?.intValue }
    }

    private static func migrateOldLists() {
        if let oldTeachable = oldTeachable() {
            setTeachable(oldTeachable)
            UserDefaults.standard.removeObject(forKey: "teachable")
        }
        if let oldFavorites = oldFavorites() {
            setFavorites(oldFavorites)
            UserDefaults.standard.removeObject(forKey: "favorites")
        }
    }

    /// Mirrors the signed-in user's document into the local lists. Exposed so an integration test
    /// can drive it against the Firestore emulator; the app calls it from `extraInit`.
    @discardableResult
    static func connectLists(to userDoc: DocumentReference) -> ListenerRegistration {
        TMTagLists.userDoc = userDoc
        // Metadata changes are included so the server's confirmation of a missing document
        // arrives even when nothing else changed; see handleUserSnapshot.
        return userDoc.addSnapshotListener(includeMetadataChanges: true) { (snapshot, error) in
            if let error = error {
                print(error)
                return
            }
            guard let snapshot = snapshot else { return }
            handleUserSnapshot(
                exists: snapshot.exists,
                fromCache: snapshot.metadata.isFromCache,
                lists: snapshot.get("lists") as? [String: Any],
                info: snapshot.get("listInfo") as? [String: Any],
                seed: { userDoc.setData(TMTagLists.remotePayload(), merge: true) }
            )
        }
    }

    /// One user-document snapshot after sign-in.
    ///
    /// The account's document is the truth: its lists replace whatever this device had, so
    /// signing in never carries local-only lists into an existing account. The one time this
    /// device's lists are uploaded is when the server says the account has no document yet,
    /// which is a brand-new account. A cache miss says nothing about the account (it is what an
    /// offline start looks like), so it neither seeds the document nor clears the local lists;
    /// the server-confirmed snapshot that follows decides.
    static func handleUserSnapshot(exists: Bool,
                                   fromCache: Bool,
                                   lists: [String: Any]?,
                                   info: [String: Any]?,
                                   seed: () -> Void) {
        if !exists {
            if !fromCache { seed() }
            return
        }
        let allIds = TMTagLists.applyRemote(lists: lists, info: info)

        // Prefetch tags
        DispatchQueue.global().async {
            DPTag.query(byIds: allIds.map { NSNumber(value: $0) }, cache: true)
        }
    }

    static func disconnectLists() {
        TMTagLists.userDoc = nil
    }

    @objc func extraInit() {
        DPAppDelegate.migrateOldLists()

        do {
            try AVAudioSession.sharedInstance().setCategory(.playback)
        } catch {
            print("Failed to configure audio session: \(error)")
        }
        
        var registration: ListenerRegistration? = nil
        _ = Auth.auth().addStateDidChangeListener { (_, user) in
            registration?.remove()
            registration = nil
            if let user = user {
                TMTagLists.prepareLocalLists(forUid: user.uid)
                registration = DPAppDelegate.connectLists(to: Firestore.firestore().document("users/\(user.uid)"))
            } else {
                DPAppDelegate.disconnectLists()
            }
        }
    }
    
}
