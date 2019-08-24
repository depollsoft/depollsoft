//
//  DPAppDelegate.swift
//  pitchperfect
//
//  Created by David Poll on 8/2/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import Parse
import Firebase

public extension DPAppDelegate {
    private static var userDoc: DocumentReference? = nil

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
                    /*let oldTeachable = DPAppDelegate.teachable()
                    let oldFavorites = DPAppDelegate.favorites()
                    if !snapshot!.exists {
                        // There was no existing user, so initialize the user
                        DPAppDelegate.userDoc?.setData([
                            "teachableIds": oldTeachable,
                            "favoriteIds": oldFavorites
                            ], merge:true)
                        return
                    }
                    let teachableIds = (snapshot?.get("teachableIds") as? [Any])?.map({ v -> Int in (v as! NSNumber).intValue}) ?? oldTeachable
                    let favoriteIds = (snapshot?.get("favoriteIds") as? [Any])?.map({ v -> Int in (v as! NSNumber).intValue}) ?? oldFavorites
                    
                    // Don't try to write these back to the server -- they're already there.
                    DPAppDelegate.setTeachable(teachableIds, doSave: false)
                    DPAppDelegate.setFavorites(favoriteIds, doSave: false)*/
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
                        PFUser.logOut()
                        print("Logged out Parse: \(curUser!.objectId!) and logged in Firebase: \(res!.user.uid)")
                    }
            }
        }
    }
}
