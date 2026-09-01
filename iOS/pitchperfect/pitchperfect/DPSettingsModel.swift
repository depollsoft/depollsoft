//
//  DPSettingsModel.swift
//  pitchperfect
//

import Foundation
import FirebaseAuth
import FirebaseFirestore

let wakeLockKey = "depollsoft.pitchperfect.WakeLock"
let toggleNoteKey = "depollsoft.pitchperfect.ToggleNote"

public extension Notification.Name {
    static let settingsChanged = Notification.Name("pitchPerfect.settingsChanged")
}

@objc public final class DPSettingsModel: NSObject {
    private var listenerRegistration: ListenerRegistration?
    private var userRef: DocumentReference?

    @objc public static let settingsChangedNotificationName = Notification.Name.settingsChanged
    @objc public static let sharedInstance = DPSettingsModel()

    @objc public func attachToFirestore() {
        guard let user = Auth.auth().currentUser else {
            detachFromFirestore()
            return
        }

        listenerRegistration?.remove()
        userRef = Firestore.firestore().document("users/\(user.uid)")
        listenerRegistration = userRef?.addSnapshotListener { [weak self] snapshot, error in
            guard let self, error == nil else { return }
            wakeLock = snapshot?.get("wakeLock") as? Bool ?? wakeLock
            toggleNotes = snapshot?.get("toggleNotes") as? Bool ?? toggleNotes
        }
    }

    @objc public func detachFromFirestore() {
        listenerRegistration?.remove()
        listenerRegistration = nil
        userRef = nil
    }

    @objc public var userString: String {
        guard let user = Auth.auth().currentUser else { return "Logged out" }
        guard let provider = user.providerData.first else { return "Current User: \(user.uid)" }
        switch provider.providerID {
        case FacebookAuthProviderID:
            return "Facebook: \(provider.email ?? user.uid)"
        case GoogleAuthProviderID:
            return "Google: \(provider.email ?? user.uid)"
        case PhoneAuthProviderID:
            return provider.phoneNumber ?? "Current User: \(user.uid)"
        default:
            return provider.email ?? "Current User: \(user.uid)"
        }
    }

    @objc public var wakeLock: Bool {
        get { UserDefaults.standard.bool(forKey: wakeLockKey) }
        set {
            UserDefaults.standard.set(newValue, forKey: wakeLockKey)
            DPAppDelegate.setIdleTimerDisabled(newValue)
            userRef?.setData(["wakeLock": newValue], merge: true)
            NotificationCenter.default.post(name: .settingsChanged, object: self)
        }
    }

    @objc public var toggleNotes: Bool {
        get { UserDefaults.standard.bool(forKey: toggleNoteKey) }
        set {
            UserDefaults.standard.set(newValue, forKey: toggleNoteKey)
            userRef?.setData(["toggleNotes": newValue], merge: true)
            NotificationCenter.default.post(name: .settingsChanged, object: self)
        }
    }

    private override init() {
        super.init()
        DPAppDelegate.setIdleTimerDisabled(wakeLock)
    }
}
