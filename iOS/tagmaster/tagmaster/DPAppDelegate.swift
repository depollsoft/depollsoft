//
//  AppDelegate.swift
//  tagmaster
//
//  Created by David Poll on 6/7/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import Firebase
import Parse

public extension Notification.Name {
    static let userDataChanged = Notification.Name("tagmaster.userDataChanged")
}

public extension DPAppDelegate {
    private static var userDoc: DocumentReference? = nil
    
    @objc static func setTeachable(_ teachables: [Int]) {
        let oldTeachables = DPAppDelegate.teachable()
        UserDefaults.standard.set(teachables, forKey: "teachable")
        if !oldTeachables.elementsEqual(teachables) {
            if userDoc != nil {
                userDoc?.setData(["teachableIds": teachables], merge: true)
            }
            NotificationCenter.default.post(name: .userDataChanged, object: nil)
        }
    }
    
    @objc static func teachable() -> [Int] {
        let array = UserDefaults.standard.array(forKey: "teachable")
        if array != nil {
            return array!.map({ v -> Int in (v as! NSNumber).intValue})
        }
        return []
    }
    
    @objc static func setFavorites(_ favorites: [Int]) {
        let oldFavorites = DPAppDelegate.favorites()
        UserDefaults.standard.set(favorites, forKey: "favorites")
        if !oldFavorites.elementsEqual(favorites) {
            if userDoc != nil {
                userDoc?.setData(["favoriteIds": favorites], merge: true)
            }
            NotificationCenter.default.post(name: .userDataChanged, object: nil)
        }
    }
    
    @objc static func favorites() -> [Int] {
        let array = UserDefaults.standard.array(forKey: "favorites")
        if array != nil {
            return array!.map({ v -> Int in (v as! NSNumber).intValue})
        }
        return []
    }
    
    @objc func extraInit() {
        convertParseUser()
        
        var registration: ListenerRegistration? = nil
        Auth.auth().addStateDidChangeListener { (auth, user) in
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
                    if snapshot == nil {
                        // There was no existing user, so initialize the user
                        DPAppDelegate.setTeachable(oldTeachable)
                        DPAppDelegate.setFavorites(oldFavorites)
                        return
                    }
                    let teachableIds = (snapshot?.get("teachableIds") as? [Any])?.map({ v -> Int in (v as! NSNumber).intValue}) ?? oldTeachable
                    DPAppDelegate.setTeachable(teachableIds)
                    let favoriteIds = (snapshot?.get("favoriteIds") as? [Any])?.map({ v -> Int in (v as! NSNumber).intValue}) ?? oldFavorites
                    DPAppDelegate.setFavorites(favoriteIds)
                }
            } else {
                DPAppDelegate.userDoc = nil
            }
        }
    }
    
    func convertParseUser() {
        let curUser = PFUser.current()
        if curUser != nil {
            Functions.functions().httpsCallable("exchangeAuthToken")
                .call(["token":curUser?.sessionToken]) { res, error in
                    if error != nil {
                        print(error!)
                        return
                    }
                    let dataDict = res!.data as! Dictionary<String, Any>
                    let firebaseToken = dataDict["token"] as! String
                    Auth.auth().signIn(withCustomToken: firebaseToken){ (res, error) in
                        if error != nil {
                            print(error!)
                            return
                        }
                        print(res!.user)
                    }
            }
        }
    }
}
