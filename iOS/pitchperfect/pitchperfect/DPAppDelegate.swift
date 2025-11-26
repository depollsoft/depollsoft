//
//  DPAppDelegate.swift
//  pitchperfect
//
//  Created by David Poll on 8/2/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import Firebase
import Parse

public extension DPAppDelegate {
    private static var userDoc: DocumentReference? = nil

    @objc func extraInit() {
        convertParseUser()

        var registration: ListenerRegistration? = nil
        Auth.auth().addStateDidChangeListener { (_, user) in
            if let reg = registration {
                reg.remove()
                registration = nil
                DPSongsModel.sharedInstance.detachFromFirestore()
                DPSettingsModel.sharedInstance.detachFromFirestore()
            }
            if let user = user {
                DPSongsModel.sharedInstance.attachToFirestore()
                DPSettingsModel.sharedInstance.attachToFirestore()
                DPAppDelegate.userDoc = Firestore.firestore().document("users/\(user.uid)")
                registration = DPAppDelegate.userDoc?.addSnapshotListener { (_, error) in
                    if let error = error {
                        print(error)
                    }
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
