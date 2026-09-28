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
import WidgetKit

let wakeLockKey = "depollsoft.pitchperfect.WakeLock"
let toggleNoteKey = "depollsoft.pitchperfect.ToggleNote"
let referencePitchKey = "depollsoft.pitchperfect.ReferencePitch"

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
        attachToFirestore(userDoc: Firestore.firestore().document("users/\(user.uid)"))
    }

    /// Follows the settings on the account's document: local changes are written
    /// there, and the document's values are applied here without being written
    /// back. (Applying them through the setters wrote each one back, and filled a
    /// field the document did not have yet with this device's value.)
    func attachToFirestore(userDoc: DocumentReference) {
        listenerRegistration?.remove()
        userRef = userDoc
        listenerRegistration = userDoc.addSnapshotListener { [weak self] snapshot, error in
            guard let self, error == nil, let snapshot else { return }
            self.applyRemote(wakeLock: snapshot.get("wakeLock") as? Bool,
                             toggleNotes: snapshot.get("toggleNotes") as? Bool,
                             referencePitch: (snapshot.get("referencePitch") as? NSNumber)?.intValue)
        }
    }

    private func applyRemote(wakeLock: Bool?, toggleNotes: Bool?, referencePitch: Int?) {
        let defaults = UserDefaults.standard
        var changed = false
        if let wakeLock, wakeLock != self.wakeLock {
            defaults.set(wakeLock, forKey: wakeLockKey)
            changed = true
        }
        if let toggleNotes, toggleNotes != self.toggleNotes {
            defaults.set(toggleNotes, forKey: toggleNoteKey)
            changed = true
        }
        if let referencePitch, DPSettingsModel.referencePitchRange.contains(referencePitch),
           referencePitch != self.referencePitch {
            defaults.set(referencePitch, forKey: referencePitchKey)
            applyReferencePitch()
            changed = true
        }
        if changed { NotificationCenter.default.post(name: .settingsChanged, object: self) }
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
                // Facebook and Google accounts can come without an email, and a
                // phone account without its number; never crash reading them.
                case FacebookAuthProviderID:
                    return "Facebook \(providerData.email ?? "")"
                case GoogleAuthProviderID:
                    return "Google: \(providerData.email ?? "")"
                case PhoneAuthProviderID:
                    return providerData.phoneNumber ?? "Current User: \(curUser!.uid)"
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
    
    /// Common choices for A4, in Hz: historical, standard and orchestral pitches.
    public static let commonReferencePitches = [415, 430, 432, 435, 438, 440, 441, 442, 443, 444, 446]
    public static let standardReferencePitch = 440
    /// What a stored or synced tuning may hold; anything else reads as 440 Hz.
    static let referencePitchRange = 400...480

    /// The A4 the notes are tuned to, in Hz.
    @objc public var referencePitch: Int {
        get {
            let stored = UserDefaults.standard.integer(forKey: referencePitchKey)
            return DPSettingsModel.referencePitchRange.contains(stored) ? stored : DPSettingsModel.standardReferencePitch
        }
        set {
            guard DPSettingsModel.referencePitchRange.contains(newValue) else { return }
            UserDefaults.standard.set(newValue, forKey: referencePitchKey)
            applyReferencePitch()
            if userRef != nil {
                userRef?.setData(["referencePitch": newValue], merge: true)
            }
            NotificationCenter.default.post(name: .settingsChanged, object: self)
        }
    }

    /// Tunes the notes, and the widget in its own process, to the stored A4.
    @objc public func applyReferencePitch() {
        let pitch = referencePitch
        DPNote.referencePitch = Double(pitch)
        guard WidgetTuningState.referencePitch != Double(pitch) else { return }
        WidgetTuningState.set(Double(pitch))
        WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
    }

    @objc public static let sharedInstance: DPSettingsModel = DPSettingsModel()
    
    /// Starts from whatever the user last chose. (Until the port this reset Toggle
    /// Notes and Wake Lock to off at every launch, which the Objective-C model
    /// and Android never did.) Internal so tests can make a fresh one.
    override init() {
        super.init()
    }
}
