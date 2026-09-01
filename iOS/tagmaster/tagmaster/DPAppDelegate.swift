//
//  AppDelegate.swift
//  tagmaster
//
//  Created by David Poll on 6/7/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
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
    
    @objc static func containsFavorite(_ tagId: Int32) -> Bool {
        favorites().contains(Int(tagId))
    }

    @objc static func moveFavorite(at fromIndex: Int, to toIndex: Int) {
        var values = favorites()
        guard values.indices.contains(fromIndex), toIndex >= 0, toIndex <= values.count else { return }
        let value = values.remove(at: fromIndex)
        values.insert(value, at: min(toIndex, values.count))
        setFavorites(values)
    }

    @objc static func addFavorite(_ tagId: Int32) {
        var values = favorites()
        guard !values.contains(Int(tagId)) else { return }
        values.append(Int(tagId))
        setFavorites(values)
    }

    @objc static func removeFavorite(_ tagId: Int32) {
        setFavorites(favorites().filter { $0 != Int(tagId) })
    }

    @objc static func containsTeachable(_ tagId: Int32) -> Bool {
        teachable().contains(Int(tagId))
    }

    @objc static func moveTeachable(at fromIndex: Int, to toIndex: Int) {
        var values = teachable()
        guard values.indices.contains(fromIndex), toIndex >= 0, toIndex <= values.count else { return }
        let value = values.remove(at: fromIndex)
        values.insert(value, at: min(toIndex, values.count))
        setTeachable(values)
    }

    @objc static func addTeachable(_ tagId: Int32) {
        var values = teachable()
        guard !values.contains(Int(tagId)) else { return }
        values.append(Int(tagId))
        setTeachable(values)
    }

    @objc static func removeTeachable(_ tagId: Int32) {
        setTeachable(teachable().filter { $0 != Int(tagId) })
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
