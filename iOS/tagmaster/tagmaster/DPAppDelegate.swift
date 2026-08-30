//
//  AppDelegate.swift
//  tagmaster
//
//  Created by David Poll on 6/7/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import AppTrackingTransparency
import Firebase
import AVKit

public extension Notification.Name {
    static let userDataChanged = Notification.Name("tagmaster.userDataChanged")
}

public extension DPAppDelegate {
    private static var userDoc: DocumentReference? = nil
    private static let LISTS_KEY = "depollsoft.pitchperfect.lists"
    
    @objc static func setTeachable(_ teachables: [Int]) {
        setTeachable(teachables, doSave: true)
    }
    
    @objc static func setTeachable(_ teachables: [Int], doSave: Bool) {
        let oldTeachables = DPAppDelegate.teachable()
        var lists = UserDefaults.standard.dictionary(forKey: LISTS_KEY) ?? [:]
        lists["teachable"] = teachables
        UserDefaults.standard.set(lists, forKey: LISTS_KEY)
        if !oldTeachables.elementsEqual(teachables) {
            if doSave && userDoc != nil {
                userDoc?.setData(["lists": ["teachable": teachables]], mergeFields: ["lists.teachable"])
            }
            NotificationCenter.default.post(name: .userDataChanged, object: nil)
        }
    }
    
    @objc static func teachable() -> [Int] {
        let dict = UserDefaults.standard.dictionary(forKey: LISTS_KEY)
        return dict?["teachable"] as? [Int] ?? []
    }
    
    static func oldTeachable() -> [Int]? {
        let array = UserDefaults.standard.array(forKey: "teachable")
        if array != nil {
            return array!.map({ v -> Int in (v as! NSNumber).intValue})
        }
        return nil
    }
    
    @objc static func setFavorites(_ favorites: [Int]) {
        setFavorites(favorites, doSave: true)
    }
    
    @objc static func setFavorites(_ favorites: [Int], doSave: Bool) {
        let oldFavorites = DPAppDelegate.favorites()
        var lists = UserDefaults.standard.dictionary(forKey: LISTS_KEY) ?? [:]
        lists["favorite"] = favorites
        UserDefaults.standard.set(lists, forKey: LISTS_KEY)
        if !oldFavorites.elementsEqual(favorites) {
            if doSave && userDoc != nil {
                userDoc?.setData(["lists": ["favorite": favorites]], mergeFields: ["lists.favorite"])
            }
            NotificationCenter.default.post(name: .userDataChanged, object: nil)
        }
    }
    
    @objc static func favorites() -> [Int] {
        let dict = UserDefaults.standard.dictionary(forKey: LISTS_KEY)
        return dict?["favorite"] as? [Int] ?? []
    }
    
    static func oldFavorites() -> [Int]? {
        let array = UserDefaults.standard.array(forKey: "favorites")
        if array != nil {
            return array!.map({ v -> Int in (v as! NSNumber).intValue})
        }
        return nil
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

        try! AVAudioSession.sharedInstance().setCategory(.playback)
        
        var registration: ListenerRegistration? = nil
        _ = Auth.auth().addStateDidChangeListener { (auth, user) in
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
                    
                    let teachableIds = (snapshot?.get("lists.teachable") as? [Any])?.map({ v -> Int in (v as! NSNumber).intValue}) ?? oldTeachable
                    let favoriteIds = (snapshot?.get("lists.favorite") as? [Any])?.map({ v -> Int in (v as! NSNumber).intValue}) ?? oldFavorites
                                        
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
