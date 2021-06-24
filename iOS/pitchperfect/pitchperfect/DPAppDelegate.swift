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
import depolllib

public extension DPAppDelegate {
    private static var userDoc: DocumentReference? = nil

    @objc func extraInit() {
        GADMobileAds.sharedInstance().disableSDKCrashReporting()
        convertParseUser()
        
        var registration: ListenerRegistration? = nil
        Auth.auth().addStateDidChangeListener { (auth, user) in
            if registration != nil {
                registration?.remove()
                DPSongsModel.sharedInstance.detachFromFirestore()
                DPSettingsModel.sharedInstance.detachFromFirestore()
            }
            if user != nil {
                DPSongsModel.sharedInstance.attachToFirestore()
                DPSettingsModel.sharedInstance.attachToFirestore()
                DPAppDelegate.userDoc = Firestore.firestore().document("users/\(user!.uid)")
                registration = DPAppDelegate.userDoc!.addSnapshotListener { (snapshot, error) in
                    if error != nil {
                        print(error!)
                        return
                    }
                }
            } else {
                DPAppDelegate.userDoc = nil
            }
        }
        var tags: [String] = []
        if Auth.auth().currentUser != nil {
            tags.append("logged_in")
        }
        Analytics.sharedInstance.logEvent(Analytics.appOpenEvent, tags: Set(tags))
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
