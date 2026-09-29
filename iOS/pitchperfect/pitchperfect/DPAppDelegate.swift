//
//  DPAppDelegate.swift
//  pitchperfect
//
//  Created by David Poll on 8/2/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//
//  Launch work the SwiftUI app hands to UIKit: Firebase, consent, audio, the
//  JSON aliases the stores serialize with, and the auth callbacks.
//

import AVFoundation
import FirebaseAuth
import FirebaseCore
import FirebaseFirestore
import GoogleMobileAds
import SwiftUI
import UIKit
import WidgetKit
#if canImport(FBSDKCoreKit)
import FBSDKCoreKit
#endif
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

@objc(DPAppDelegate)
final class DPAppDelegate: UIResponder, UIApplicationDelegate {
    private static var userDoc: DocumentReference?
    private var authListener: AuthStateDidChangeListenerHandle?

    static var isRunningTests: Bool { NSClassFromString("XCTestCase") != nil }

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        // A fresh process cannot be sounding a widget pitch; never leave a cell lit.
        configureWidgetPlayback()
        guard !Self.isRunningTests else { return true }
        DPSettingsModel.sharedInstance.applyReferencePitch()
        // Models made before launch (NotePlayer) pick up the stored tuning.
        NotificationCenter.default.post(name: .settingsChanged, object: DPSettingsModel.sharedInstance)

        DPAppLog.start()
        FirebaseApp.configure()
        TelemetryConsent.configure()
#if canImport(FBSDKCoreKit)
        ApplicationDelegate.shared.application(application, didFinishLaunchingWithOptions: launchOptions)
#endif
        // Keep the ad SDK from replacing Crashlytics signal handlers.
        MobileAds.shared.disableSDKCrashReporting()
        AdConsent.configure()
        AdConsent.onConsentFlowFinished = { [weak self] in self?.offerOptionalLogin() }
        application.registerForRemoteNotifications()

        try? AVAudioSession.sharedInstance().setCategory(.playback)
        attachSyncToSignIn()
        return true
    }

    /// Lit cells mirror tones owned by this process. A new process owns no
    /// tones, so any persisted active pitches are stale.
    func configureWidgetPlayback() {
        WidgetPlaybackBridge.installStopObserver()
        guard !WidgetPitchState.activePitches.isEmpty else { return }
        WidgetPitchState.set([])
        WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
    }

    /// Songs and settings follow whoever is signed in.
    private func attachSyncToSignIn() {
        var registration: ListenerRegistration?
        authListener = Auth.auth().addStateDidChangeListener { _, user in
            if let reg = registration {
                reg.remove()
                registration = nil
                DPSongsModel.sharedInstance.detachFromFirestore()
                DPSettingsModel.sharedInstance.detachFromFirestore()
            }
            if let user {
                DPSongsModel.sharedInstance.attachToFirestore()
                DPSettingsModel.sharedInstance.attachToFirestore()
                DPAppDelegate.userDoc = Firestore.firestore().document("users/\(user.uid)")
                registration = DPAppDelegate.userDoc?.addSnapshotListener { _, error in
                    if let error { print(error) }
                }
            } else {
                DPAppDelegate.userDoc = nil
            }
        }
    }

    // MARK: - Presentation from outside SwiftUI

    static var rootController: UIViewController? {
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows).first(where: \.isKeyWindow)?.rootViewController
    }

    /// Every activation offers Privacy choices until the user has made them.
    static func sceneDidBecomeActive() {
        guard !isRunningTests, let root = rootController else { return }
        TelemetryConsent.presentIfNeeded(from: root)
    }

    private func offerOptionalLogin() {
        DispatchQueue.main.async {
            let defaults = UserDefaults.standard
            guard TelemetryConsent.hasChosen, let root = Self.rootController, root.presentedViewController == nil else { return }
            // First launch belongs to the first pitch: the login prompt waits for the next session.
            guard defaults.bool(forKey: "depollsoft.pitchperfect.FirstLaunchSeen") else {
                defaults.set(true, forKey: "depollsoft.pitchperfect.FirstLaunchSeen")
                return
            }
            guard !defaults.bool(forKey: "depollsoft.pitchperfect.LoginShown"), Auth.auth().currentUser == nil else { return }
            defaults.set(true, forKey: "depollsoft.pitchperfect.LoginShown")
            root.present(UIHostingController(rootView: NavigationStack { LoginIntroScreen() }), animated: true)
        }
    }

    // MARK: - URLs

    /// Auth providers' callbacks (the SwiftUI scene hands every opened URL here).
    @discardableResult
    static func handle(url: URL) -> Bool {
#if canImport(GoogleSignIn)
        if GIDSignIn.sharedInstance.handle(url) { return true }
#endif
#if canImport(FBSDKCoreKit)
        if ApplicationDelegate.shared.application(UIApplication.shared, open: url, sourceApplication: nil, annotation: nil) {
            return true
        }
#endif
        return Auth.auth().canHandle(url)
    }
}

/// The host the unit tests run in: no Firebase, no ads, no window of its own.
@objc(DPTestAppDelegate)
final class DPTestAppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        let window = UIWindow(frame: UIScreen.main.bounds)
        window.rootViewController = UIViewController()
        window.isHidden = true
        self.window = window
        return true
    }
}

@main
enum PitchPerfectMain {
    static func main() {
#if DEBUG
        UITestSongStore.prepare(ProcessInfo.processInfo.environment)
#endif
        if DPAppDelegate.isRunningTests {
            UIApplicationMain(CommandLine.argc, CommandLine.unsafeArgv, nil, NSStringFromClass(DPTestAppDelegate.self))
        } else {
            PitchPerfectApp.main()
        }
    }
}

struct PitchPerfectApp: App {
    @UIApplicationDelegateAdaptor(DPAppDelegate.self) private var delegate
    // Built when SwiftUI creates the app, before didFinishLaunching: the song
    // store registers its own serialization aliases before it reads.
    @State private var models = PitchPerfectModels()

    var body: some Scene {
        WindowGroup {
            PitchPerfectRoot(models: models)
        }
    }
}

#if DEBUG
/// Launch hooks for UI tests that need the song store in a particular state,
/// applied before anything reads it. Only defaults are touched, so nothing is
/// registered or decoded before the store does it itself.
enum UITestSongStore {
    private static let keys = [DPSongsModel.songListsKey, DPSongsModel.currentListKey, DPSongsModel.legacySongsKey]
    private static func stashed(_ key: String) -> String { "depollsoft.pitchperfect.uitest.stash." + key }
    private static let stashMarker = "depollsoft.pitchperfect.uitest.stashed"

    static func prepare(_ environment: [String: String], defaults: UserDefaults = .standard) {
        // PP_STASH_SONGS: set the simulator's own songs aside (once) for a test.
        if environment["PP_STASH_SONGS"] == "1", !defaults.bool(forKey: stashMarker) {
            for key in keys {
                defaults.set(defaults.object(forKey: key), forKey: stashed(key))
                defaults.removeObject(forKey: key)
            }
            defaults.set(true, forKey: stashMarker)
        }
        // PP_LEGACY_SONGS_JSON: the old app's saved songs, as it stored them.
        if let json = environment["PP_LEGACY_SONGS_JSON"],
           let songs = try? JSONSerialization.jsonObject(with: Data(json.utf8)) {
            defaults.set(songs, forKey: DPSongsModel.legacySongsKey)
        }
        // PP_UNSTASH_SONGS: put them back, replacing whatever the test left.
        if environment["PP_UNSTASH_SONGS"] == "1", defaults.bool(forKey: stashMarker) {
            for key in keys {
                defaults.set(defaults.object(forKey: stashed(key)), forKey: key)
                defaults.removeObject(forKey: stashed(key))
            }
            defaults.removeObject(forKey: stashMarker)
        }
    }
}
#endif
