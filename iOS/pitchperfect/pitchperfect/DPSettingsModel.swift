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
let noteSoundKey = "depollsoft.pitchperfect.NoteSound"

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
                             referencePitch: (snapshot.get("referencePitch") as? NSNumber)?.intValue,
                             noteSound: snapshot.get("noteSound") as? String)
        }
    }

    func applyRemote(wakeLock: Bool?, toggleNotes: Bool?, referencePitch: Int?, noteSound: String? = nil) {
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
        // A value this version can't use falls back to A440, as a stored one does.
        if let remote = referencePitch {
            let pitch = DPSettingsModel.referencePitchRange.contains(remote) ? remote : DPSettingsModel.standardReferencePitch
            if pitch != self.referencePitch {
                defaults.set(pitch, forKey: referencePitchKey)
                applyReferencePitch()
                changed = true
            }
        }
        // A sound this version doesn't know plays the pitch pipe here, and the
        // account keeps the other device's choice.
        if let remote = noteSound {
            let sound = DPNoteSound.validated(remote)
            if sound != self.noteSound {
                defaults.set(sound, forKey: noteSoundKey)
                applyNoteSound()
                changed = true
            }
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

    /// The choices to offer: the common ones, plus the current one if another
    /// device chose something else.
    static func referencePitchChoices(current: Int) -> [Int] {
        Array(Set(commonReferencePitches + [current])).sorted()
    }

    /// Tunes the notes, and the widget in its own process, to the stored A4.
    @objc public func applyReferencePitch() {
        let pitch = referencePitch
        DPNote.referencePitch = Double(pitch)
        guard WidgetTuningState.referencePitch != Double(pitch) else { return }
        WidgetTuningState.set(Double(pitch))
        WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
    }

    /// The voice notes play in: a DPNoteSound id. Anything this version doesn't
    /// know reads as the pitch pipe.
    @objc public var noteSound: String {
        get { DPNoteSound.validated(UserDefaults.standard.string(forKey: noteSoundKey)) }
        set {
            guard DPNoteSound.isKnown(newValue) else { return }
            UserDefaults.standard.set(newValue, forKey: noteSoundKey)
            applyNoteSound()
            if userRef != nil {
                userRef?.setData(["noteSound": newValue], merge: true)
            }
            NotificationCenter.default.post(name: .settingsChanged, object: self)
        }
    }

    /// The picker's sections: the default alone, then the waves, then the instruments.
    static let noteSoundSections: [(title: String?, sounds: [String])] = [
        (nil, [DPNoteSoundPitchPipe]),
        ("Waves", DPNoteSound.waves()),
        ("Instruments", DPNoteSound.instruments()),
    ]

    /// A sound as the picker names it.
    static func noteSoundLabel(_ sound: String) -> String {
        switch sound {
        case DPNoteSoundPitchPipe: "Pitch Perfect (Loud)"
        case "sine": "Sine"
        case "triangle": "Triangle"
        case "square": "Square"
        case "sawtooth": "Sawtooth"
        case "piano": "Piano"
        case "electricPiano": "Electric Piano"
        case "harpsichord": "Harpsichord"
        case "vibraphone": "Vibraphone"
        case "organ": "Organ"
        case "reedOrgan": "Reed Organ"
        case "accordion": "Accordion"
        case "harmonica": "Harmonica"
        case "guitar": "Guitar"
        case "harp": "Harp"
        case "strings": "Strings"
        case "choir": "Choir"
        case "trumpet": "Trumpet"
        case "clarinet": "Clarinet"
        case "flute": "Flute"
        default: sound
        }
    }

    /// Plays notes, and the widget in its own process, in the stored sound, and
    /// loads its instrument ahead of the first note.
    @objc public func applyNoteSound() {
        let sound = noteSound
        DPNote.sound = sound
        MIDINotePlayer.shared.prepare(sound: sound)
        guard WidgetSoundState.sound != sound else { return }
        WidgetSoundState.set(sound)
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
