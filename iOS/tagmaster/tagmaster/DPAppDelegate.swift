//
//  DPAppDelegate.swift
//  tagmaster
//
//  The launch work SwiftUI's App leaves to a delegate (Firebase, consent, the
//  Facebook SDK, remote notifications, list sync on sign-in), the sign-in URL
//  check, the accent, and the saved-tag helpers the screens call.
//

import AVKit
import FirebaseAuth
import FirebaseCore
import FirebaseFirestore
import Foundation
import UIKit
#if canImport(FBSDKCoreKit)
import FBSDKCoreKit
#endif
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

public extension Notification.Name {
    static let userDataChanged = Notification.Name("tagmaster.userDataChanged")
    /// A tag list's order or contents may have changed; an open detail rechecks its neighbours.
    static let TMTagListDidChange = Notification.Name("TMTagListDidChangeNotification")
    /// The open tag or the split presentation changed; lists reconcile their selection.
    static let TMTagSelectionDidChange = Notification.Name("TMTagSelectionDidChangeNotification")
}

/// Adopted by any on-screen list of tags so the open detail can step to a
/// neighbouring tag without leaving the list.
@objc protocol TMTagListSource: NSObjectProtocol {
    /// The tag ids in the order they are presented, top to bottom.
    func tm_listedTagIds() -> [NSNumber]
    /// Called after a step lands on `tagId`, so the source can select and reveal its row
    /// (and fetch the next page when the last loaded tag was stepped onto).
    @objc(tm_didStepToTagId:) func tm_didStep(toTagId tagId: Int32)
}

@objc(DPAppDelegate)
final class DPAppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        DPAppDelegate.configureCacheSerialization()
        DPAppLog.start()
        FirebaseApp.configure()
        TelemetryConsent.configure()
        #if canImport(FBSDKCoreKit)
        ApplicationDelegate.shared.application(application, didFinishLaunchingWithOptions: launchOptions)
        #endif
        application.registerForRemoteNotifications()
        extraInit()
        return true
    }

    /// Both the app and the unit-test host must round-trip URLs and dates in the disk cache.
    @objc static func configureCacheSerialization() {
        DPJsonSerializer.register({ object in (object as? URL)?.absoluteString },
                                            deserializer: { input -> Any? in input.flatMap { URL(string: $0) } },
                                            for: NSURL.self)
        let numbers = NumberFormatter()
        DPJsonSerializer.register({ object in
            (object as? Date).flatMap { numbers.string(from: NSNumber(value: $0.timeIntervalSince1970)) }
        }, deserializer: { input -> Any? in
            Date(timeIntervalSince1970: numbers.number(from: input ?? "")?.doubleValue ?? 0)
        }, for: type(of: NSDate()))
    }

    /// Whether a sign-in provider (Google, Facebook, Firebase) claims `url`; only a URL
    /// none of them wants is routed as a tag link.
    @objc static func handleAuthURL(_ url: URL) -> Bool {
        #if canImport(GoogleSignIn)
        if GIDSignIn.sharedInstance.handle(url) { return true }
        #endif
        #if canImport(FBSDKCoreKit)
        if ApplicationDelegate.shared.application(UIApplication.shared, open: url, options: [:]) { return true }
        #endif
        return authCanHandle(url)
    }

    /// Firebase Auth's own check; tests replace it.
    static var authCanHandle: (URL) -> Bool = { Auth.auth().canHandle($0) }

    /// Tag Master's one accent, shared with Android: #007AA3 in light, #5AC8FA in dark.
    @objc static let accentColor: UIColor = UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0x5A / 255.0, green: 0xC8 / 255.0, blue: 0xFA / 255.0, alpha: 1)
            : UIColor(red: 0x00 / 255.0, green: 0x7A / 255.0, blue: 0xA3 / 255.0, alpha: 1)
    }

    // MARK: Saved tags

    @objc static func containsFavorite(_ tagId: Int32) -> Bool { favorites().contains(Int(tagId)) }
    @objc static func containsTeachable(_ tagId: Int32) -> Bool { teachable().contains(Int(tagId)) }
    @objc static func addFavorite(_ tagId: Int32) { setFavorites(adding(Int(tagId), to: favorites())) }
    @objc static func addTeachable(_ tagId: Int32) { setTeachable(adding(Int(tagId), to: teachable())) }
    @objc static func removeFavorite(_ tagId: Int32) { setFavorites(favorites().filter { $0 != Int(tagId) }) }
    @objc static func removeTeachable(_ tagId: Int32) { setTeachable(teachable().filter { $0 != Int(tagId) }) }
    @objc(moveFavoriteAt:to:) static func moveFavorite(at from: UInt, to: UInt) {
        setFavorites(moving(favorites(), from: Int(from), to: Int(to)))
    }
    @objc(moveTeachableAt:to:) static func moveTeachable(at from: UInt, to: UInt) {
        setTeachable(moving(teachable(), from: Int(from), to: Int(to)))
    }

    private static func adding(_ id: Int, to ids: [Int]) -> [Int] { ids.contains(id) ? ids : ids + [id] }

    private static func moving(_ ids: [Int], from: Int, to: Int) -> [Int] {
        guard ids.indices.contains(from), to >= 0, to < ids.count else { return ids }
        var moved = ids
        moved.insert(moved.remove(at: from), at: to)
        return moved
    }
}

extension DPAppDelegate {
    @objc static func setTeachable(_ teachables: [Int]) {
        setTeachable(teachables, doSave: true)
    }

    @objc static func setTeachable(_ teachables: [Int], doSave: Bool) {
        TMTagLists.setIds(teachables, for: TMTagLists.teachableKey, doSave: doSave)
    }

    @objc static func teachable() -> [Int] {
        TMTagLists.ids(for: TMTagLists.teachableKey)
    }

    static func oldTeachable() -> [Int]? {
        guard let array = UserDefaults.standard.array(forKey: "teachable") else {
            return nil
        }
        return array.compactMap { ($0 as? NSNumber)?.intValue }
    }

    @objc static func setFavorites(_ favorites: [Int]) {
        setFavorites(favorites, doSave: true)
    }

    @objc static func setFavorites(_ favorites: [Int], doSave: Bool) {
        TMTagLists.setIds(favorites, for: TMTagLists.favoriteKey, doSave: doSave)
    }

    @objc static func favorites() -> [Int] {
        TMTagLists.ids(for: TMTagLists.favoriteKey)
    }

    static func oldFavorites() -> [Int]? {
        guard let array = UserDefaults.standard.array(forKey: "favorites") else {
            return nil
        }
        return array.compactMap { ($0 as? NSNumber)?.intValue }
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

    /// Mirrors the signed-in user's document into the local lists. Exposed so an integration test
    /// can drive it against the Firestore emulator; the app calls it from `extraInit`.
    @discardableResult
    static func connectLists(to userDoc: DocumentReference) -> ListenerRegistration {
        TMTagLists.userDoc = userDoc
        // Metadata changes are included so the server's confirmation of a missing document
        // arrives even when nothing else changed; see handleUserSnapshot.
        return userDoc.addSnapshotListener(includeMetadataChanges: true) { (snapshot, error) in
            if let error = error {
                print(error)
                return
            }
            guard let snapshot = snapshot else { return }
            handleUserSnapshot(
                exists: snapshot.exists,
                fromCache: snapshot.metadata.isFromCache,
                lists: snapshot.get("lists") as? [String: Any],
                info: snapshot.get("listInfo") as? [String: Any],
                seed: { userDoc.setData(TMTagLists.remotePayload(), merge: true) }
            )
        }
    }

    /// One user-document snapshot after sign-in.
    ///
    /// The account's document is the truth: its lists replace whatever this device had, so
    /// signing in never carries local-only lists into an existing account. The one time this
    /// device's lists are uploaded is when the server says the account has no document yet,
    /// which is a brand-new account. A cache miss says nothing about the account (it is what an
    /// offline start looks like), so it neither seeds the document nor clears the local lists;
    /// the server-confirmed snapshot that follows decides.
    static func handleUserSnapshot(exists: Bool,
                                   fromCache: Bool,
                                   lists: [String: Any]?,
                                   info: [String: Any]?,
                                   seed: () -> Void) {
        if !exists {
            if !fromCache { seed() }
            return
        }
        let allIds = TMTagLists.applyRemote(lists: lists, info: info)

        // Prefetch tags
        DispatchQueue.global().async {
            DPTag.query(byIds: allIds.map { NSNumber(value: $0) }, cache: true)
        }
    }

    static func disconnectLists() {
        TMTagLists.userDoc = nil
    }

    @objc func extraInit() {
        DPAppDelegate.migrateOldLists()

        do {
            try AVAudioSession.sharedInstance().setCategory(.playback)
        } catch {
            print("Failed to configure audio session: \(error)")
        }
        
        var registration: ListenerRegistration? = nil
        _ = Auth.auth().addStateDidChangeListener { (_, user) in
            registration?.remove()
            registration = nil
            if let user = user {
                TMTagLists.prepareLocalLists(forUid: user.uid)
                registration = DPAppDelegate.connectLists(to: Firestore.firestore().document("users/\(user.uid)"))
            } else {
                DPAppDelegate.disconnectLists()
            }
        }
    }
    
}
