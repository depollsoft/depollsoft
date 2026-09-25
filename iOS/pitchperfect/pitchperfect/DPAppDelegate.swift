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
        Self.registerSerializationAliases()
        attachSyncToSignIn()
        return true
    }

    /// The type names the stored songs and lists were serialized under.
    static func registerSerializationAliases() {
        DPJsonSerializer.registerAlias("List", for: NSClassFromString("__NSArrayM"))
        DPJsonSerializer.registerAlias("Key", for: DPKey.self)
        DPJsonSerializer.registerAlias("KeyType", for: DPKeyType.self)
        DPJsonSerializer.registerAlias("Accidental", for: DPAccidental.self)
        DPJsonSerializer.registerAlias("Note", for: DPNote.self)
        DPJsonSerializer.registerAlias("PitchedSong", for: DPPitchedSong.self)
        DPJsonSerializer.registerAlias("String", for: NSString.self)
        DPJsonSerializer.registerAlias("Primitive", for: DPJsonPrimitive.self)
        DPJsonSerializer.registerAlias("Integer", forObjCType: String(cString: "i"))
        DPJsonSerializer.registerAlias("Boolean", forObjCType: String(cString: "B"))
        DPJsonSerializer.registerAlias("Double", forObjCType: String(cString: "d"))
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

    static var frontmostController: UIViewController? {
        let root = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows).first(where: \.isKeyWindow)?.rootViewController
        var top = root
        while let next = top?.presentedViewController { top = next }
        return top
    }

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
        // The stores read whatever this simulator last saved (a UI test run of the
        // real app, say), which needs the same aliases the app registers.
        DPAppDelegate.registerSerializationAliases()
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
        if DPAppDelegate.isRunningTests {
            UIApplicationMain(CommandLine.argc, CommandLine.unsafeArgv, nil, NSStringFromClass(DPTestAppDelegate.self))
        } else {
            PitchPerfectApp.main()
        }
    }
}

struct PitchPerfectApp: App {
    @UIApplicationDelegateAdaptor(DPAppDelegate.self) private var delegate
    @State private var models = PitchPerfectModels()

    var body: some Scene {
        WindowGroup {
            PitchPerfectRoot(models: models)
        }
    }
}
