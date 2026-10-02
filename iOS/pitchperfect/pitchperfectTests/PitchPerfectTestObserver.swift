//
//  PitchPerfectTestObserver.swift
//  pitchperfectTests
//
//  The bundle's principal class: runs once before any test.
//

import FirebaseAnalytics
import FirebaseAuth
import FirebaseCore
import FirebaseInstallations
import Foundation
import SwiftUI
import XCTest
@testable import pitchperfect

@objc(PitchPerfectTestObserver)
final class PitchPerfectTestObserver: NSObject, XCTestObservation {
    override init() {
        super.init()
        XCTestObservationCenter.shared.addTestObserver(self)
    }

    func testBundleWillStart(_ testBundle: Bundle) {
        // No test may start the audio hardware: wiring AVAudioEngine's output
        // has deadlocked hosted runs. Instruments get no engine here.
        MIDINotePlayer.usesAudioHardware = false
        // The first Canvas and alert pay their one-off rendering cost here, not in a test; see TestRendering.
        MainActor.assumeIsolated {
            TestRendering.warmUp([AnyView(PitchInstrumentView(model: PitchPipeModel()).frame(width: 360, height: 360))])
            Self.startFirebase()
        }
    }

    /// Hosted tests use Firebase (PitchPerfectTestCase configures it). Its first
    /// start loads a saved user and an installation ID from the keychain, which
    /// the unsigned test host can't read (-34018). On CI that start once blocked
    /// the main thread for 30 s inside a test and spent its whole budget
    /// (CleanupHelperTests, on main and on PR #89); tests running alongside it
    /// in other runs took 6 and 11 s instead of 1 or 2. Starting it here, and
    /// waiting until Auth has reported its user and an installation ID has been
    /// asked for, pays that once under the run's startup allowance.
    @MainActor
    private static func startFirebase() {
        if FirebaseApp.app() == nil { FirebaseApp.configure() }
        var authStarted = false
        var installationAnswered = false
        var analyticsAnswered = false
        let listener = Auth.auth().addStateDidChangeListener { _, _ in authStarted = true }
        Installations.installations().installationID { _, _ in
            DispatchQueue.main.async { installationAnswered = true }
        }
        // Analytics starts on its own queue and answers only once that is done. A run where
        // its start (about 10 s on CI) overlapped CleanupHelperTests never showed the edit
        // controls; runs where it started minutes later passed.
        Analytics.sessionID { _, _ in
            DispatchQueue.main.async { analyticsAnswered = true }
        }
        let deadline = Date(timeIntervalSinceNow: 120)
        while !(authStarted && installationAnswered && analyticsAnswered), Date() < deadline {
            RunLoop.main.run(until: Date(timeIntervalSinceNow: 0.05))
        }
        Auth.auth().removeStateDidChangeListener(listener)
    }
}
