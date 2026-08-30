//
//  DPAppDelegate.swift
//  pitchperfect
//
//  Created by David Poll on 8/2/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import Firebase

public extension DPAppDelegate {
    private static var userDoc: DocumentReference? = nil

    @objc func extraInit() {
        var registration: ListenerRegistration? = nil
        _ = Auth.auth().addStateDidChangeListener { (_, user) in
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
    
}
