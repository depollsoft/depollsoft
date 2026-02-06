//
//  AppDelegate.swift
//  tagmaster
//
//  Created by David Poll on 6/7/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import AppTrackingTransparency
import Firebase
import Parse
import AVKit

public extension Notification.Name {
    static let userDataChanged = Notification.Name("tagmaster.userDataChanged")
}

public extension DPAppDelegate {
    /// Connect Firestore to the local emulator for development/testing.
    /// Call before any Firestore operations. Gated behind --useFirebaseEmulator launch argument.
    private static var emulatorConfigured = false
    
    @objc static func configureFirestoreEmulatorIfNeeded() {
        if !emulatorConfigured && ProcessInfo.processInfo.arguments.contains("--useFirebaseEmulator") {
            let settings = Firestore.firestore().settings
            settings.host = "localhost:8080"
            settings.isSSLEnabled = false
            settings.cacheSettings = MemoryCacheSettings()
            Firestore.firestore().settings = settings
            emulatorConfigured = true
        }
    }
    
    @objc static func useFirestoreEmulator(host: String = "localhost", port: Int = 8080) {
        if !emulatorConfigured {
            let settings = Firestore.firestore().settings
            settings.host = "\(host):\(port)"
            settings.isSSLEnabled = false
            settings.cacheSettings = MemoryCacheSettings()
            Firestore.firestore().settings = settings
            emulatorConfigured = true
        }
    }
}

public extension DPAppDelegate {

    @objc static func setTeachable(_ teachables: [Int]) {
        let model = ListModel.get("teachable")
        model.setIds(teachables)
    }

    @objc static func teachable() -> [Int] {
        return ListModel.get("teachable").ids
    }

    static func oldTeachable() -> [Int]? {
        let array = UserDefaults.standard.array(forKey: "teachable")
        if array != nil {
            return array!.map({ v -> Int in (v as! NSNumber).intValue})
        }
        return nil
    }

    @objc static func setFavorites(_ favorites: [Int]) {
        let model = ListModel.get("favorite")
        model.setIds(favorites)
    }

    @objc static func favorites() -> [Int] {
        return ListModel.get("favorite").ids
    }

    static func oldFavorites() -> [Int]? {
        let array = UserDefaults.standard.array(forKey: "favorites")
        if array != nil {
            return array!.map({ v -> Int in (v as! NSNumber).intValue})
        }
        return nil
    }

    private static func migrateOldLists() {
        if let oldTeachable = oldTeachable() {
            setTeachable(oldTeachable)
            UserDefaults.standard.removeObject(forKey: "teachable")
        }
        if let oldFavorites = oldFavorites() {
            setFavorites(oldFavorites)
            UserDefaults.standard.removeObject(forKey: "favorites")
        }
    }

    @objc func extraInit() {
        DPAppDelegate.configureFirestoreEmulatorIfNeeded()
        DPAppDelegate.migrateOldLists()
        convertParseUser()
        try! AVAudioSession.sharedInstance().setCategory(.playback)

        _ = Auth.auth().addStateDidChangeListener { (auth, user) in
            if user != nil {
                ListModel.connectToFirestore()

                // Prefetch tags
                DispatchQueue.global().async {
                    let allIds = DPAppDelegate.teachable() + DPAppDelegate.favorites()
                    DPTag.query(byIds: allIds.map { NSNumber(value: $0) }, cache: true)
                }
            } else {
                ListModel.disconnectFromFirestore()
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
