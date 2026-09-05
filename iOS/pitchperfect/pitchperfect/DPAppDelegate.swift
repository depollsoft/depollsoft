//
//  DPAppDelegate.swift
//  pitchperfect
//
//  Created by David Poll on 8/2/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import FirebaseAuth
import FirebaseFirestore
import FirebaseFunctions
import WidgetKit

public extension DPAppDelegate {
    private static var userDoc: DocumentReference? = nil

    /// The widget's lit cell mirrors a tone owned by this process. A new
    /// process owns no tone, so any persisted active pitch is stale.
    @objc func configureWidgetPlayback() {
        WidgetPlaybackBridge.installStopObserver()
        guard WidgetPitchState.activePitch != nil else { return }
        WidgetPitchState.set(nil)
        WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
    }

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
