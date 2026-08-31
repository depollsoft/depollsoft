//
//  DPSettingsModel.swift
//  pitchperfect
//
//  Created by David Poll on 6/20/21.
//  Copyright © 2021 DepollSoft. All rights reserved.
//

import Foundation
import FirebaseAuth
import FirebaseFirestore

let wakeLockKey = "depollsoft.pitchperfect.WakeLock"
let toggleNoteKey = "depollsoft.pitchperfect.ToggleNote"

public extension Notification.Name {
    static let settingsChanged = Notification.Name("pitchPerfect.settingsChanged")
}

@objc public class DPSettingsModel: NSObject {
    private var listenerRegistration: ListenerRegistration?
    private var userRef: DocumentReference?
    
    @objc public static let settingsChangedNotificationName = Notification.Name.settingsChanged
    
    @objc public func attachToFirestore() {
        guard let user = Auth.auth().currentUser else {
            detachFromFirestore()
            return
        }

        listenerRegistration?.remove()
        userRef = Firestore.firestore().document("users/\(user.uid)")
        listenerRegistration = userRef?.addSnapshotListener { snapshot, error in
            if error != nil {
                return
            }
            self.wakeLock = snapshot?.get("wakeLock") as? Bool ?? self.wakeLock
            self.toggleNotes = snapshot?.get("toggleNotes") as? Bool ?? self.toggleNotes
        }
    }
    
    @objc public func detachFromFirestore() {
        if listenerRegistration != nil {
            listenerRegistration?.remove()
        }
        listenerRegistration = nil
        userRef = nil
    }
    
    @objc public var userString: String {
        get {
            let curUser = Auth.auth().currentUser
            if (curUser == nil) {
                return "Logged out"
            }
            if (curUser!.providerData.count > 0) {
                let providerData = curUser!.providerData.first!
                switch(providerData.providerID) {
                case FacebookAuthProviderID:
                    return "Facebook \(providerData.email!)"
                case GoogleAuthProviderID:
                    return "Google: \(providerData.email!)"
                case PhoneAuthProviderID:
                    return providerData.phoneNumber!
                default:
                    return providerData.email ?? "Current User: \(curUser!.uid)"
                }
            }
            return "Current User: \(curUser!.uid)";
        }
    }
    
    @objc public var wakeLock: Bool {
        get {
            UserDefaults.standard.bool(forKey: wakeLockKey)
        }
        set {
            UserDefaults.standard.set(newValue, forKey: wakeLockKey)
            if userRef != nil {
                userRef?.setData(["wakeLock": newValue], merge: true)
            }
            NotificationCenter.default.post(name: .settingsChanged, object: self)
        }
    }
    
    @objc public var toggleNotes: Bool {
        get {
            UserDefaults.standard.bool(forKey: toggleNoteKey)
        }
        set {
            UserDefaults.standard.set(newValue, forKey: toggleNoteKey)
            if userRef != nil {
                userRef?.setData(["toggleNotes": newValue], merge: true)
            }
            NotificationCenter.default.post(name: .settingsChanged, object: self)
        }
    }
    
    @objc public static let sharedInstance: DPSettingsModel = DPSettingsModel()
    
    private override init() {
        super.init()
        self.wakeLock = false
        self.toggleNotes = false
    }
}
