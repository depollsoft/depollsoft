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
    private static var userDoc: DocumentReference? = nil
    private static let listsKey = "depollsoft.pitchperfect.lists"
    
    @objc static func setTeachable(_ teachables: [Int]) {
        setTeachable(teachables, doSave: true)
    }
    
    @objc static func setTeachable(_ teachables: [Int], doSave: Bool) {
        let oldTeachables = DPAppDelegate.teachable()
        var lists = UserDefaults.standard.dictionary(forKey: listsKey) ?? [:]
        lists["teachable"] = teachables
        UserDefaults.standard.set(lists, forKey: listsKey)
        if !oldTeachables.elementsEqual(teachables) {
            if doSave && userDoc != nil {
                userDoc?.setData(["lists": ["teachable": teachables]], mergeFields: ["lists.teachable"])
            }
            NotificationCenter.default.post(name: .userDataChanged, object: nil)
        }
    }
    
    @objc static func teachable() -> [Int] {
        let dict = UserDefaults.standard.dictionary(forKey: listsKey)
        return dict?["teachable"] as? [Int] ?? []
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
        let oldFavorites = DPAppDelegate.favorites()
        var lists = UserDefaults.standard.dictionary(forKey: listsKey) ?? [:]
        lists["favorite"] = favorites
        UserDefaults.standard.set(lists, forKey: listsKey)
        if !oldFavorites.elementsEqual(favorites) {
            if doSave && userDoc != nil {
                userDoc?.setData(["lists": ["favorite": favorites]], mergeFields: ["lists.favorite"])
            }
            NotificationCenter.default.post(name: .userDataChanged, object: nil)
        }
    }
    
    @objc static func favorites() -> [Int] {
        let dict = UserDefaults.standard.dictionary(forKey: listsKey)
        return dict?["favorite"] as? [Int] ?? []
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
        
    @objc func extraInit() {
        DPAppDelegate.migrateOldLists()

        do {
            try AVAudioSession.sharedInstance().setCategory(.playback)
        } catch {
            print("Failed to configure audio session: \(error)")
        }
        
        var registration: ListenerRegistration? = nil
        _ = Auth.auth().addStateDidChangeListener { (_, user) in
            if registration != nil {
                registration?.remove()
            }
            if user != nil {
                DPAppDelegate.userDoc = Firestore.firestore().document("users/\(user!.uid)")
                registration = DPAppDelegate.userDoc!.addSnapshotListener { (snapshot, error) in
                    if error != nil {
                        print(error!)
                        return
                    }
                    let oldTeachable = DPAppDelegate.teachable()
                    let oldFavorites = DPAppDelegate.favorites()
                    if !snapshot!.exists {
                        // There was no existing user, so initialize the user
                        DPAppDelegate.userDoc?.setData([
                            "lists": ["favorite": oldFavorites, "teachable": oldTeachable]
                        ], merge:true)
                        return
                    }
                    
                    let teachableIds = (snapshot?.get("lists.teachable") as? [Any])?.compactMap {
                        ($0 as? NSNumber)?.intValue
                    } ?? oldTeachable
                    let favoriteIds = (snapshot?.get("lists.favorite") as? [Any])?.compactMap {
                        ($0 as? NSNumber)?.intValue
                    } ?? oldFavorites
                                        
                    // Don't try to write these back to the server -- they're already there.
                    DPAppDelegate.setTeachable(teachableIds, doSave: false)
                    DPAppDelegate.setFavorites(favoriteIds, doSave: false)
                    
                    // Prefetch tags
                    DispatchQueue.global().async {
                        DPTag.query(byIds: (teachableIds + favoriteIds).map { NSNumber(value: $0) }, cache: true)
                    }
                }
            } else {
                DPAppDelegate.userDoc = nil
            }
        }
    }
    
}
